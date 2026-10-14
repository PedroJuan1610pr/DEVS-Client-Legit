# Devs Peer API + túnel Cloudflare (mismo esquema que Licencias)

Los clientes **no instalan nada**. Leen la URL del Gist:

https://gist.githubusercontent.com/PedroJuan1610pr/6cff64c9557c02e22ab743c7a912ff96/raw/peer-api.json

## En el Ubuntu (una vez)

```bash
# 1) API HTTP (si no está)
sudo systemctl enable --now devs-peer-api

# 2) Scripts del zip
sudo cp update-gist-peer.sh watch-tunnel-peer.sh /opt/devs-peer-api/
sudo chmod +x /opt/devs-peer-api/update-gist-peer.sh /opt/devs-peer-api/watch-tunnel-peer.sh
sudo chown pedro:pedro /opt/devs-peer-api/update-gist-peer.sh /opt/devs-peer-api/watch-tunnel-peer.sh

# 3) Token GitHub (el mismo de Licencias)
ls ~/.config/licencias/github-gist-token

# 4) Servicios túnel + watcher
sudo cp devs-peer-tunnel.service devs-peer-tunnel-watch.service /etc/systemd/system/
sudo systemctl daemon-reload
sudo systemctl enable --now devs-peer-tunnel
sudo systemctl enable --now devs-peer-tunnel-watch

# 5) Ver URL y que el Gist se actualizó
sleep 8
grep -a trycloudflare /opt/devs-peer-api/tunnel.log | tail -3
curl -s "https://gist.githubusercontent.com/PedroJuan1610pr/6cff64c9557c02e22ab743c7a912ff96/raw/peer-api.json"
```

## Cliente

No hace falta `peer-api.txt`. El mod consulta el Gist solo.
