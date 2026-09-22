# Reporte de contraste (fase 13)

> Ratios WCAG calculadas con la funcion de luminancia relativa (misma que usa
> `ColorContrastTest`) sobre los valores hex REALES de `ui/theme/Color.kt`.
> Umbral texto normal: **4.5:1** (WCAG 1.4.3). Componentes no textuales: **3:1**
> (WCAG 1.4.11). Todos los valores de este documento fueron calculados el
> 2026-09-22 con un script sobre los hex del fichero de colores (no inventados).

## Pares de texto (>= 4.5:1)

| Par | Uso en la UI | Claro | Oscuro | Resultado |
|---|---|---|---|---|
| primary / onPrimary | botones primarios | 9.75:1 | 6.82:1 | OK |
| primary / primaryContainer | texto primary sobre container | 7.58:1 | 5.22:1 | OK |
| secondary / onSecondary | roles secundarios | 6.47:1 | 7.66:1 | OK |
| tertiary / onTertiary | acentos terciarios | 6.55:1 | 7.79:1 | OK |
| error / onError | errores / dialogo de borrado | 6.46:1 | 7.72:1 | OK |
| background / onBackground | texto sobre fondo | 15.27:1 | 14.04:1 | OK |
| surface / onSurface | texto principal | 16.28:1 | 13.49:1 | OK |
| surfaceVariant / onSurfaceVariant | badge NEUTRAL / texto secundario | 7.08:1 | 5.40:1 | OK |
| inverseSurface / inverseOnSurface | superficies invertidas | 11.64:1 | 10.25:1 | OK |
| onPrimaryContainer / primaryContainer | badge SUCCESS (`RenfeStatusBadge`) | 13.65:1 | 10.05:1 | OK (anadido fase 13) |
| onTertiaryContainer / tertiaryContainer | badge WARNING | 13.24:1 | 7.38:1 | OK (anadido fase 13) |
| onErrorContainer / errorContainer | badge ERROR | 13.26:1 | 7.24:1 | OK (anadido fase 13) |
| onPrimaryContainer / surfaceContainerLow | acento exito en tarjetas (Diagnostics/FollowUps) | 16.08:1 | 13.14:1 | OK (anadido fase 13) |
| onSurface / primaryContainer | texto en tarjeta de tren seleccionada | 13.25:1 | 10.07:1 | OK (anadido fase 13) |
| onSurfaceVariant / primaryContainer | texto secundario en tarjeta seleccionada | 7.10:1 | 7.64:1 | OK (anadido fase 13) |
| onSurface / surfaceContainerLow | texto principal en tarjetas (Card M3) | 15.61:1 | 13.17:1 | OK (anadido fase 13) |
| onSurfaceVariant / surfaceContainerLow | texto secundario en tarjetas | 8.36:1 | 9.99:1 | OK (anadido fase 13) |

`ColorContrastTest` se amplio en fase 13 con los 8 pares usados por badges y tarjetas
(9 -> 17 pares de texto por esquema; siguen siendo 4 metodos de test).

## Pares no textuales (>= 3:1)

| Par | Uso | Claro | Oscuro | Resultado |
|---|---|---|---|---|
| outline / background | bordes/iconos sobre fondo | 3.97:1 | 5.74:1 | OK |
| outline / surface | bordes/iconos sobre superficie | 4.23:1 | 5.52:1 | OK |
| primary / background | indicador primario sobre fondo | 8.74:1 | 7.27:1 | OK |
| error / background | indicador de error sobre fondo | 5.79:1 | 10.61:1 | OK |
| primaryContainer / surfaceContainerLow | estado "seleccionada" de tarjeta | 1.18:1 | 1.31:1 | **FALLA 1.4.11** (hallazgo) |

## Hallazgo (informacion solo por color)

- **Tarjeta de tren seleccionada en Buscar** (`SearchComponents.kt:129-133`): el estado
  "seleccionado" se comunica solo con el color de fondo `primaryContainer` vs el fondo de tarjeta
  por defecto `surfaceContainerLow`. Diferencia medida: **1.18:1** (claro) y **1.31:1** (oscuro),
  por debajo de 3:1 (WCAG 1.4.11). No hay borde, marca de seleccion adicional ni semantica
  especifica ("sonido seleccionado").
- Decision fase 13: se documenta como **PENDIENTE/riesgo** (el alcance de la fase 13 no modifica
  componentes; la spec limita los ficheros a tocar). Mitigacion recomendada (fase futura): borde
  diferenciado (`outline`/`primary`) o icono de seleccion en la tarjeta.
- Importante: los badges de estado NUNCA dependen solo del color: llevan etiqueta de texto
  (`RenfeStatusBadge`, `ui/components/RenfeStatusBadge.kt`) y el icono es decorativo
  (`contentDescription = null`); el texto aporta siempre el significado, cumpliendo 1.4.1 en esos
  elementos.