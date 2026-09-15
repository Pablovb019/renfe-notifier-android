#!/usr/bin/env python3
"""
Script de medición de recursos para el scheduler (Paso 32).
Mide CPU, RAM, disco, duración, peticiones HTTP y bytes con:
- 1 y 5 seguimientos
- Con y sin agrupación
Usa FakeSearch para medir solo la lógica de la app (sin red real).
"""

import asyncio
import gc
import os
import sys
import time
from datetime import UTC, datetime
from datetime import time as dtime
from pathlib import Path

import psutil

# Añadir backend al path
sys.path.insert(0, str(Path(__file__).parent.parent))

from app.config import Settings
from app.followups.database import FollowUpRepository
from app.followups.domain import Availability, FollowUp, FollowUpMode
from app.renfe.client import SearchResult
from app.renfe.parser import ParseStatus, TrainList
from app.renfe.stations import StationCatalog
from app.scheduler.plan import build_plan


class FakeSearch:
    """Simula búsquedas a Renfe con métricas controladas."""

    def __init__(self, results: list[SearchResult], delay_s: float = 0.05):
        self._results = results
        self._delay = delay_s
        self.call_count = 0

    async def __call__(
        self, *, origin, destination, travel_date, plaza_h
    ) -> SearchResult:
        self.call_count += 1
        await asyncio.sleep(self._delay)  # Simula latencia de red
        return self._results[(self.call_count - 1) % len(self._results)]


def create_test_followups(count: int, same_group: bool = False) -> list[FollowUp]:
    """Crea followups de prueba con códigos de estación reales del catálogo."""
    base_date = datetime.now(UTC).date()
    now = datetime.now(UTC)
    followups = []
    # Usar códigos reales del catálogo: MADRI (60000), BARCE (71801), etc.
    origin_codes = [
        "60000",
        "17000",
        "51003",
        "48020",
        "15000",
    ]  # Madrid, Girona, Valencia, Bilbao, A Coruña
    dest_codes = [
        "71801",
        "17000",
        "51003",
        "48020",
        "15000",
    ]  # Barcelona, Girona, Valencia, Bilbao, A Coruña

    for i in range(count):
        if same_group:
            # Mismo grupo: misma ruta, fecha, plaza_h
            origin_code = "60000"  # Madrid
            destination_code = "71801"  # Barcelona
            travel_date = base_date
            plaza_h = False
        else:
            # Grupos distintos - rotar entre códigos válidos
            origin_code = origin_codes[i % len(origin_codes)]
            destination_code = dest_codes[i % len(dest_codes)]
            # Evitar mismo origen/destino
            if origin_code == destination_code:
                destination_code = dest_codes[(i + 1) % len(dest_codes)]
            travel_date = base_date
            plaza_h = False

        followup = FollowUp.create(
            followup_id=f"test-{i}",
            origin_code=origin_code,
            destination_code=destination_code,
            travel_date=travel_date,
            mode=FollowUpMode.ALL,
            now=now,
            specific_train_id=None,
            plaza_h=plaza_h,
        )
        followups.append(followup)
    return followups


def create_fake_search_results() -> list[SearchResult]:
    """Crea resultados de búsqueda simulados con métricas realistas."""
    from decimal import Decimal

    # Tren con disponibilidad
    trains_with_availability = [
        type(
            "Train",
            (),
            {
                "identifier": "AVE-123",
                "departure": dtime(8, 0),
                "arrival": dtime(10, 30),
                "availability": Availability.AVAILABLE,
                "price": Decimal("45.50"),
            },
        )()
    ]
    # Tren sin disponibilidad
    trains_no_availability = [
        type(
            "Train",
            (),
            {
                "identifier": "AVE-456",
                "departure": dtime(12, 0),
                "arrival": dtime(14, 30),
                "availability": Availability.NO_AVAILABILITY,
                "price": None,
            },
        )()
    ]

    results = [
        SearchResult(
            trains=TrainList(
                status=ParseStatus.OK,
                plaza_h_requested=False,
                trains=trains_with_availability,
            ),
            metrics=type(
                "Metrics", (), {"request_count": 5, "bytes_received": 12450}
            )(),
        ),
        SearchResult(
            trains=TrainList(
                status=ParseStatus.OK,
                plaza_h_requested=False,
                trains=trains_no_availability,
            ),
            metrics=type(
                "Metrics", (), {"request_count": 5, "bytes_received": 12450}
            )(),
        ),
        SearchResult(
            trains=TrainList(
                status=ParseStatus.OK,
                plaza_h_requested=True,
                trains=trains_with_availability,
            ),
            metrics=type(
                "Metrics", (), {"request_count": 8, "bytes_received": 18750}
            )(),
        ),
    ]
    return results


async def run_measurement(
    name: str,
    followups: list[FollowUp],
    search_results: list[SearchResult],
    concurrency: int = 2,
    interval_s: float = 0.1,
) -> dict:
    """Ejecuta una medición completa y retorna métricas."""
    process = psutil.Process(os.getpid())

    # Configuración
    settings = Settings()
    repo = FollowUpRepository(settings.database_path)
    # La BD ya está inicializada (tests previos), solo limpiar datos
    with repo._session() as conn:
        conn.execute("DELETE FROM followups")
        conn.execute("DELETE FROM episodes")
        conn.execute("DELETE FROM alert_events")
        conn.commit()

    # Guardar followups
    for fu in followups:
        repo.create(fu)
        repo.add_episode(
            fu.followup_id, 0, datetime.now(UTC), fu.seen_available_train_ids
        )

    # Monitoring: resetear contadores del singleton existente (evita problemas de import order)
    from app import monitoring as monitoring_module

    with monitoring_module.monitoring._lock:
        monitoring_module.monitoring._logical_queries = 0
        monitoring_module.monitoring._http_requests = 0
        monitoring_module.monitoring._bytes_received = 0

    # Catálogo
    catalog = StationCatalog()

    # Scheduler (importar DESPUÉS de resetear monitoring)
    from app.scheduler.service import SchedulerService as _SchedulerService

    fake_search = FakeSearch(search_results, delay_s=0.02)
    scheduler = _SchedulerService(
        repository=repo,
        search_fn=fake_search,
        catalog=catalog,
        clock=lambda: datetime.now(UTC),
        interval_s=interval_s,
        concurrency=concurrency,
    )

    # Medición inicial
    gc.collect()
    await asyncio.sleep(0.1)
    cpu_start = process.cpu_percent()
    mem_start = process.memory_info().rss / 1024 / 1024  # MB
    disk_start = sum(
        f.stat().st_size
        for f in Path(settings.database_path).parent.rglob("*")
        if f.is_file()
    )
    start_time = time.perf_counter()

    # Ejecutar un ciclo
    await scheduler.run_once()

    # Medición final
    end_time = time.perf_counter()
    gc.collect()
    await asyncio.sleep(0.1)
    cpu_end = process.cpu_percent()
    mem_end = process.memory_info().rss / 1024 / 1024
    disk_end = sum(
        f.stat().st_size
        for f in Path(settings.database_path).parent.rglob("*")
        if f.is_file()
    )

    duration = end_time - start_time
    cpu_avg = (cpu_start + cpu_end) / 2
    mem_delta = mem_end - mem_start
    disk_delta = disk_end - disk_start

    # Leer métricas del monitoring del módulo
    from app import monitoring as monitoring_module

    snap = monitoring_module.monitoring.snapshot()

    return {
        "name": name,
        "followups": len(followups),
        "groups": len(build_plan(followups)),
        "concurrency": concurrency,
        "duration_s": round(duration, 3),
        "cpu_percent_avg": round(cpu_avg, 1),
        "memory_mb_start": round(mem_start, 1),
        "memory_mb_end": round(mem_end, 1),
        "memory_delta_mb": round(mem_delta, 1),
        "disk_delta_kb": round(disk_delta / 1024, 1),
        "http_requests": snap.http_requests,
        "bytes_received": snap.bytes_received,
        "logical_queries": snap.logical_queries,
        "search_calls": fake_search.call_count,
    }


def format_table(results: list[dict]) -> str:
    """Formatea resultados en tabla legible."""
    headers = [
        "Escenario",
        "Seguimientos",
        "Grupos",
        "Concurrencia",
        "Duración (s)",
        "CPU %",
        "RAM Inicio (MB)",
        "RAM Delta (MB)",
        "Disco Delta (KB)",
        "Peticiones HTTP",
        "Bytes Recibidos",
        "Queries Lógicas",
        "Llamadas Search",
    ]
    rows = []
    for r in results:
        rows.append(
            [
                r["name"],
                str(r["followups"]),
                str(r["groups"]),
                str(r["concurrency"]),
                str(r["duration_s"]),
                str(r["cpu_percent_avg"]),
                str(r["memory_mb_start"]),
                str(r["memory_delta_mb"]),
                str(r["disk_delta_kb"]),
                str(r["http_requests"]),
                str(r["bytes_received"]),
                str(r["logical_queries"]),
                str(r["search_calls"]),
            ]
        )

    # Calcular anchos
    col_widths = [len(h) for h in headers]
    for row in rows:
        for i, cell in enumerate(row):
            col_widths[i] = max(col_widths[i], len(cell))

    def fmt_row(cells):
        return " | ".join(c.ljust(w) for c, w in zip(cells, col_widths))

    lines = [fmt_row(headers), fmt_row(["-" * w for w in col_widths])]
    for row in rows:
        lines.append(fmt_row(row))
    return "\n".join(lines)


async def main():
    print("=" * 80)
    print("MEDICIÓN DE RECURSOS - PASO 32")
    print("=" * 80)
    print()

    # Preparar resultados de búsqueda
    search_results = create_fake_search_results()

    # Escenarios de prueba
    scenarios = [
        # (nombre, followups, same_group, concurrency)
        ("1 seguimiento, 1 grupo", create_test_followups(1, same_group=True), 2),
        (
            "5 seguimientos, 5 grupos (sin agrupación)",
            create_test_followups(5, same_group=False),
            2,
        ),
        (
            "5 seguimientos, 1 grupo (agrupación total)",
            create_test_followups(5, same_group=True),
            2,
        ),
        (
            "5 seguimientos, 1 grupo, concurrencia 1",
            create_test_followups(5, same_group=True),
            1,
        ),
        (
            "5 seguimientos, 1 grupo, concurrencia 2",
            create_test_followups(5, same_group=True),
            2,
        ),
    ]

    results = []
    for name, followups, concurrency in scenarios:
        print(f"Ejecutando: {name}...")
        result = await run_measurement(
            name, followups, search_results, concurrency=concurrency
        )
        results.append(result)

    print()
    print("RESULTADOS:")
    print("-" * 80)
    print(format_table(results))
    print()

    # Análisis
    print("ANÁLISIS:")
    print("-" * 80)

    # Comparar 1 vs 5 seguimientos sin agrupación
    r1 = next(r for r in results if r["name"] == "1 seguimiento, 1 grupo")
    r5_nogroup = next(
        r for r in results if r["name"] == "5 seguimientos, 5 grupos (sin agrupación)"
    )
    r5_group = next(
        r for r in results if r["name"] == "5 seguimientos, 1 grupo (agrupación total)"
    )

    print(
        f"1 seguimiento:           {r1['duration_s']:.3f}s, {r1['http_requests']} req, {r1['bytes_received']:,} bytes"
    )
    print(
        f"5 seguimientos (5 grupos): {r5_nogroup['duration_s']:.3f}s, {r5_nogroup['http_requests']} req, {r5_nogroup['bytes_received']:,} bytes"
    )
    print(
        f"5 seguimientos (1 grupo):  {r5_group['duration_s']:.3f}s, {r5_group['http_requests']} req, {r5_group['bytes_received']:,} bytes"
    )
    print()

    # Beneficio de agrupación
    group_saved_requests = r5_nogroup["http_requests"] - r5_group["http_requests"]
    group_saved_bytes = r5_nogroup["bytes_received"] - r5_group["bytes_received"]
    print(
        f"Ahorro por agrupacion (5->1 grupo): {group_saved_requests} peticiones, {group_saved_bytes:,} bytes ({(group_saved_bytes / r5_nogroup['bytes_received'] * 100):.1f}%)"
    )
    print()

    # Concurrencia
    r_conc1 = next(
        r for r in results if r["name"] == "5 seguimientos, 1 grupo, concurrencia 1"
    )
    r_conc2 = next(
        r for r in results if r["name"] == "5 seguimientos, 1 grupo, concurrencia 2"
    )
    print(f"Concurrencia 1: {r_conc1['duration_s']:.3f}s")
    print(f"Concurrencia 2: {r_conc2['duration_s']:.3f}s")
    print()

    # Proyección a VM e2-micro (1 GB RAM, 1 vCPU)
    print("PROYECCIÓN A VM e2-micro (1 GB RAM, 1 vCPU):")
    print("-" * 80)
    print(f"RAM base app: ~{r1['memory_mb_start']:.0f} MB")
    print(f"RAM por ciclo (5 grupos): +{r5_nogroup['memory_delta_mb']:.1f} MB")
    print(f"RAM por ciclo (1 grupo):  +{r5_group['memory_delta_mb']:.1f} MB")
    print(f"Disco por ciclo: ~{r5_group['disk_delta_kb']:.1f} KB")
    print(f"CPU promedio: {r5_group['cpu_percent_avg']:.1f}% (1 vCPU = 100% max)")
    print()

    # Evaluación Selenium
    print("EVALUACIÓN FALLBACK SELENIUM:")
    print("-" * 80)
    print("El scraper actual usa HTTP puro (DWR) con métricas:")
    print(f"  - Peticiones por búsqueda: {r1['http_requests']}")
    print(f"  - Bytes por búsqueda: ~{r1['bytes_received']:,}")
    print(f"  - Duración por grupo: {r1['duration_s']:.3f}s (simulado 20ms latencia)")
    print()
    print("En VM real con latencia real (~200-500ms por request):")
    print("  - 5 requests * 300ms = 1.5s por búsqueda")
    print("  - 1 grupo = 1.5s/ciclo; 5 grupos = 7.5s/ciclo (sin agrupación)")
    print("  - Con agrupacion (1 grupo): 1.5s/ciclo -> 80% menos tiempo CPU")
    print()
    print("CONCLUSIÓN: HTTP DWR basta. Selenium NO necesario para el caso base.")
    print("  - Selenium añadiría: Chrome (~150-300 MB RAM), ChromeDriver, complejidad")
    print(
        f"  - e2-micro (1 GB) tendría: App (~{r1['memory_mb_start']:.0f} MB) + Selenium (~200 MB) = ~{r1['memory_mb_start'] + 200:.0f} MB"
    )
    print(
        f"  - Margen: 1 GB - {r1['memory_mb_start'] + 200:.0f} MB = {1024 - r1['memory_mb_start'] - 200:.0f} MB para SO + buffer"
    )
    print("  - RIESGO: Muy ajustado. Si HTTP funciona, EVITAR Selenium.")
    print()

    # Qué falta medir en VM real
    print("MEDICIONES REALES EN e2-micro QUE FALTAN (Paso 32):")
    print("-" * 80)
    print("1. Latencia real Renfe (DWR) desde IP de GCP europe-west1")
    print("2. CPU/RAM con concurrencia 2 y múltiples ciclos sostenidos (1 hora)")
    print("3. Memoria con GC de Python bajo carga sostenida")
    print("4. Disco SQLite con WAL + múltiples lecturas/escrituras concurrentes")
    print("5. Red: bytes reales facturados vs bytes de aplicación")
    print("6. Health check + migraciones + docker compose en arranque")
    print("7. Si Selenium fuera necesario: RAM con Chrome headless + 1 búsqueda")
    print()

    # Guardar resultados JSON
    import json

    output = {
        "timestamp": datetime.now(UTC).isoformat(),
        "environment": "local-windows",
        "python_version": sys.version,
        "results": results,
        "analysis": {
            "grouping_saves_requests": group_saved_requests,
            "grouping_saves_bytes": group_saved_bytes,
            "selenium_needed": False,
            "selenium_rationale": "HTTP DWR funciona; Selenium añade 150-300 MB RAM en e2-micro (1 GB), riesgo OOM",
            "missing_vm_measurements": [
                "Latencia real Renfe desde GCP",
                "CPU/RAM carga sostenida 1h",
                "GC Python bajo carga",
                "SQLite WAL concurrente",
                "Bytes facturados vs app",
                "Arranque docker compose + health check",
            ],
        },
    }
    out_path = Path(__file__).parent.parent / "docs" / "measurements.json"
    out_path.write_text(json.dumps(output, indent=2))
    print(f"Resultados guardados en: {out_path}")


if __name__ == "__main__":
    asyncio.run(main())
