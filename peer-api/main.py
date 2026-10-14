"""
Devs Peer API — presencia de cosméticos/tab sin mod en el server de MC.
Incluye descubrimiento UDP en LAN (puerto 8788) para que el cliente se conecte solo.
"""
from __future__ import annotations

import socket
import threading
import time
from typing import Dict, List, Optional

from fastapi import FastAPI, Query
from pydantic import BaseModel, Field

TIMEOUT_SEC = 20
HTTP_PORT = 8787
UDP_PORT = 8788
MAGIC_REQ = b"DEVS_PEER_DISCOVER"
MAGIC_PREFIX = b"DEVS_PEER_API "

STORE: Dict[str, dict] = {}

app = FastAPI(title="Devs Peer API", version="1.1.0")


class PresenceIn(BaseModel):
    uuid: str = Field(..., min_length=32, max_length=36)
    name: str = ""
    flags: int = 1
    color: int = 0xFF4DD2FF
    server: str = ""


class PeerOut(BaseModel):
    uuid: str
    name: str
    flags: int
    color: int
    server: str
    updated: float


def _local_ips() -> List[str]:
    ips: List[str] = []
    try:
        hostname = socket.gethostname()
        for info in socket.getaddrinfo(hostname, None, socket.AF_INET):
            ip = info[4][0]
            if ip and not ip.startswith("127."):
                ips.append(ip)
    except Exception:
        pass
    try:
        s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        s.connect(("8.8.8.8", 80))
        ip = s.getsockname()[0]
        s.close()
        if ip and ip not in ips and not ip.startswith("127."):
            ips.insert(0, ip)
    except Exception:
        pass
    return ips or ["127.0.0.1"]


def _udp_responder() -> None:
    """Responde a broadcasts DEVS_PEER_DISCOVER con la URL HTTP de esta máquina."""
    sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    try:
        sock.setsockopt(socket.SOL_SOCKET, socket.SO_BROADCAST, 1)
    except Exception:
        pass
    sock.bind(("0.0.0.0", UDP_PORT))
    sock.settimeout(1.0)
    while True:
        try:
            data, addr = sock.recvfrom(256)
            if not data.startswith(MAGIC_REQ):
                continue
            # Preferir IP que el cliente ya usó para contactarnos
            reply_ip = addr[0]
            if reply_ip.startswith("127."):
                ips = _local_ips()
                reply_ip = ips[0]
            url = f"http://{reply_ip}:{HTTP_PORT}"
            sock.sendto(MAGIC_PREFIX + url.encode("utf-8"), addr)
        except socket.timeout:
            continue
        except Exception:
            time.sleep(0.5)


@app.on_event("startup")
def _start_udp() -> None:
    t = threading.Thread(target=_udp_responder, name="devs-peer-udp", daemon=True)
    t.start()


@app.get("/health")
def health():
    return {"ok": True, "peers": len(STORE), "discover_udp": UDP_PORT}


@app.post("/api/v1/presence")
def presence(body: PresenceIn):
    if len(body.uuid) == 36:
        key = body.uuid.lower()
    else:
        key = body.uuid.lower()
    STORE[key] = {
        "uuid": key,
        "name": body.name[:32],
        "flags": int(body.flags),
        "color": int(body.color),
        "server": body.server[:128],
        "updated": time.time(),
    }
    return {"ok": True}


@app.get("/api/v1/players", response_model=List[PeerOut])
def players(
    uuids: str = Query("", description="UUIDs separados por coma"),
    server: Optional[str] = Query(None, description="Filtrar por server MC"),
):
    now = time.time()
    dead = [k for k, v in STORE.items() if now - v["updated"] > TIMEOUT_SEC]
    for k in dead:
        STORE.pop(k, None)

    want = set()
    if uuids.strip():
        for u in uuids.split(","):
            u = u.strip().lower()
            if u:
                want.add(u)

    out: List[PeerOut] = []
    for k, v in STORE.items():
        if now - v["updated"] > TIMEOUT_SEC:
            continue
        if want and k not in want and k.replace("-", "") not in {x.replace("-", "") for x in want}:
            kn = k.replace("-", "")
            if not any(w.replace("-", "") == kn for w in want):
                continue
        # Con lista de UUIDs (tab del cliente) no filtrar por server:
        # en públicos cada cliente puede ver IP distinta / dominio / puerto.
        if not want and server and v.get("server") and v["server"] != server:
            continue
        out.append(PeerOut(**v))
    return out


@app.get("/api/v1/player/{uuid}")
def one(uuid: str):
    key = uuid.lower()
    v = STORE.get(key)
    if not v:
        kn = key.replace("-", "")
        for k, val in STORE.items():
            if k.replace("-", "") == kn:
                v = val
                break
    if not v or time.time() - v["updated"] > TIMEOUT_SEC:
        return {"ok": False}
    return {"ok": True, **v}


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=HTTP_PORT)
