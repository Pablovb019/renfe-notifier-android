import datetime
import json
import logging
import os
import random
import re
import string
import time
import unicodedata
import urllib.parse
from itertools import count

import json5
import requests

logging.basicConfig(format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
                    level=logging.DEBUG)

logger = logging.getLogger(__name__)


class RenfeChecker:
    SEARCH_URL = "https://venta.renfe.com/vol/buscarTren.do?Idioma=es&Pais=ES"
    DWR_ENDPOINT = "https://venta.renfe.com/vol/dwr/call/plaincall/"
    SYSTEM_ID_URL = f"{DWR_ENDPOINT}__System.generateId.dwr"
    UPDATE_SESSION_URL = f"{DWR_ENDPOINT}buyEnlacesManager.actualizaObjetosObjetosSesion.dwr"
    TRAIN_LIST_URL = f"{DWR_ENDPOINT}trainEnlacesManager.getTrainsList.dwr"

    def __init__(self):
        self._station_lookup = self._load_station_lookup()

    def _load_station_lookup(self):
        # Assume data/stations.json exists relative to this file
        current_dir = os.path.dirname(os.path.abspath(__file__))
        stations_path = os.path.join(current_dir, "..", "..", "data", "stations.json")
        if not os.path.exists(stations_path):
            logger.warning(f"Stations file not found at {stations_path}. Using empty lookup.")
            return {}
        with open(stations_path, "r", encoding="utf-8") as f:
            raw = json.load(f)
        if not isinstance(raw, dict):
            raise ValueError("stations.json must be a JSON object")

        lookup = {}
        for station_name, station_data in raw.items():
            if not isinstance(station_data, dict):
                continue
            station_code = str(station_data.get("cdgoEstacion") or "").strip()
            key = self._normalize_station_key(station_name)
            if key == "":
                continue
            lookup[key] = {
                "name": station_name,
                "code": station_code,
                "priority": int(station_data.get("nmroPrioridad", 0))
            }
        return lookup

    def _normalize_station(self, station):
        if station is None:
            return ""
        key = station.strip().upper()
        mapped = self.STATION_ALIASES.get(key, station.strip())
        return mapped.strip()

    def _normalize_station_key(self, station):
        normalized = unicodedata.normalize("NFKD", station or "")
        normalized = "".join(ch for ch in normalized if not unicodedata.combining(ch))
        normalized = normalized.upper()
        return re.sub(r"[^A-Z0-9]", "", normalized)

    def _resolve_station_metadata(self, station):
        if station is None:
            return None
        key = self._normalize_station_key(station)
        if key in self._station_lookup:
            return self._station_lookup[key]
        return None

    def _create_search_id(self):
        return ''.join(random.sample(string.ascii_lowercase, 8))

    def _tokenify(self, number):
        return str(number).zfill(4)

    def _create_script_session_id(self, dwr_token):
        return self._tokenify(int(time.time() * 1000))

    def _extract_dwr_token(self, response_text):
        # Simple extraction: look for a sequence of alphanumeric chars length >=10
        match = re.search(r'[A-Za-z0-9]{10,}', response_text)
        if match:
            return match.group(0)
        return ""

    def _extract_train_list(self, response_text):
        # Try to parse the response as JSON5 and find a list
        import json5
        try:
            data = json5.loads(response_text)
            if isinstance(data, list):
                return data
            if isinstance(data, dict):
                for v in data.values():
                    if isinstance(v, list):
                        return v
        except Exception:
            pass
        return []

    def _create_search_payload(self, origin, destination, dat_go, dat_ret, plaza_h):
        return {
            'idGo': dat_go,
            'idVuelta': dat_ret,
            'idOrig': origin['code'],
            'idDest': destination['code'],
            'nombOrig': origin['name'],
            'nombDest': destination['name'],
            'lluvia': '0',
            'codigosApego': '[]',
            'codigosDistribucion': '[]',
            'FechaIda': dat_go,
            'FechaVuelta': dat_ret,
            'horaIda': '',
            'horaVuelta': '',
            'Catedral': '0',
            'Plaza': 'H' if plaza_h else 'N',
            'ids': '0',
            'df': 'false'
        }

    def _create_search_cookie(self, origin, destination):
        # Simplified cookie creation
        return {
            'name': 'JSESSIONID',
            'value': '',
            'domain': '.renfe.com',
            'path': '/'
        }

    def _create_generate_id_payload(self, search_id, batch_id):
        batch = self._tokenify(next(batch_id))
        return f'''
        callCount=1
        page=/vol/buscarTren.do?Idioma=es&Pais=ES
        httpSessionState=!
        scriptSessionId=
        c0-scriptName=System
        c0-methodName=generateId
        c0-id=0:{search_id}
        c0-param0=string:{batch}
        batchId={batch_id}
        '''

    def _create_update_session_payload(self, search_id, script_session_id, batch_id):
        batch = self._tokenify(next(batch_id))
        return f'''
        callCount=1
        page=/vol/buscarTren.do?Idioma=es&Pais=ES
        httpSessionState=!
        scriptSessionId={script_session_id}
        c0-scriptName=buyEnlacesManager
        c0-methodName=actualizaObjetosSesion
        c0-id=0:{search_id}
        c0-param0=number:{search_id}
        c0-param1=number:{script_session_id}
        c0-param2=number:{batch}
        batchId={batch_id}
        '''

    def _create_get_train_list_payload(self, dat_go, dat_ret, script_session_id, search_id, batch_id, plaza_h):
        batch = self._tokenify(next(batch_id))
        plaza = 'H' if plaza_h else 'N'
        return f'''
        callCount=1
        page=/vol/buscarTren.do?Idioma=es&Pais=ES
        httpSessionState=!
        scriptSessionId={script_session_id}
        c0-scriptName=trainEnlacesManager
        c0-methodName=getTrainsList
        c0-id=0:{search_id}
        c0-param0=string:{dat_go}
        c0-param1=string:{dat_ret}
        c0-param2=string:{script_session_id}
        c0-param3=string:{search_id}
        c0-param4=number:{batch_id}
        c0-param5=string:{plaza}
        batchId={batch_id}
        '''

    def _dwr_base_available(train):
        return train.get('base', False)

    def _is_dwr_train_available(self, train, plaza_h):
        # Simplified: train has DISPONIBLE field
        return train.get('DISPONIBLE', False)

    def _parse_dwr_trains(self, trains_raw, plaza_h):
        trains = []
        for t in trains_raw:
            # Ensure we have required fields
            train = {
                'SALIDA': t.get('horaSalida', ''),
                'LLEGADA': t.get('horaLlegada', ''),
                'DISPONIBLE': self._is_dwr_train_available(t, plaza_h),
                'DISTANCIA': t.get('distancia', 0),
                'PRECIO': t.get('precio', 0.0),
                'TREN': t.get('tren', '')
            }
            trains.append(train)
        return trains

    def close(self):
        # Nothing to close for HTTP-only checker
        pass

    def check_trip(self, orig, dest, dat_go, dat_ret=None, plaza_h=False):
        try:
            dwr_trains = self._check_trip_dwr(orig, dest, dat_go, dat_ret, plaza_h)
            if dwr_trains is not None:
                if len(dwr_trains) == 0:
                    logger.info("DWR returned no trains for %s -> %s (%s)", orig, dest, dat_go)
                    return False, None
                logger.info("DWR returned %d trains for %s -> %s (%s)", len(dwr_trains), orig, dest, dat_go)
                return True, dwr_trains
        except Exception as e:
            logger.exception("DWR lookup failed for %s -> %s (%s): %s", orig, dest, dat_go, e)
        return False, None

    def _check_trip_dwr(self, orig, dest, dat_go, dat_ret=None, plaza_h=False):
        origin_meta = self._resolve_station_metadata(orig)
        destination_meta = self._resolve_station_metadata(dest)
        if origin_meta is None or destination_meta is None:
            logger.warning("Could not resolve station metadata for DWR query: %s -> %s", orig, dest)
            return None
        if origin_meta["code"] == "" or destination_meta["code"] == "":
            logger.warning("Missing station code for DWR query: %s -> %s", orig, dest)
            return None

        search_id = self._create_search_id()
        batch_id = count()
        session = requests.Session()
        session.headers = {
            "User-Agent": "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 "
            "(KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36",
            "Accept": "*/*",
            "Connection": "keep-alive",
        }

        cookie = self._create_search_cookie(origin_meta, destination_meta)
        session.cookies.set(**cookie)

        search_payload = self._create_search_payload(origin_meta, destination_meta, dat_go, dat_ret, plaza_h)
        response = session.post(self.SEARCH_URL, data=search_payload, allow_redirects=True, timeout=30)
        response.raise_for_status()

        generate_payload = self._create_generate_id_payload(search_id, batch_id)
        session.post(self.SYSTEM_ID_URL, data=generate_payload, timeout=30).raise_for_status()
        generate_payload = self._create_generate_id_payload(search_id, batch_id)
        token_response = session.post(self.SYSTEM_ID_URL, data=generate_payload, timeout=30)
        token_response.raise_for_status()

        dwr_token = self._extract_dwr_token(token_response.text)
        session.cookies.set("DWRSESSIONID", dwr_token, path="/vol", domain="venta.renfe.com")
        script_session_id = self._create_script_session_id(dwr_token)

        update_payload = self._create_update_session_payload(search_id, script_session_id, batch_id)
        session.post(self.UPDATE_SESSION_URL, data=update_payload, timeout=30).raise_for_status()

        train_payload = self._create_get_train_list_payload(
            dat_go,
            dat_ret,
            script_session_id,
            search_id,
            batch_id,
            plaza_h,
        )
        trains_response = session.post(self.TRAIN_LIST_URL, data=train_payload, timeout=30)
        trains_response.raise_for_status()

        trains_raw = self._extract_train_list(trains_response.text)
        return self._parse_dwr_trains(trains_raw, plaza_h)