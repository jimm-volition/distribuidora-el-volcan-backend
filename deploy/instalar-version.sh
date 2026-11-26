#!/usr/bin/env bash
# Instala una version nueva (Guia 18, pasos 11, 13 y 18). Lo llama publicar.ps1 despues de copiar
# los archivos a /tmp/gas-volcan:  auth.jar, pedidos.jar y web/ (el dist de React).
# Uso (en la EC2):  sudo bash /tmp/gas-volcan/deploy/instalar-version.sh
set -euo pipefail

if [[ $EUID -ne 0 ]]; then echo "Ejecutar con sudo"; exit 1; fi
SRC=/tmp/gas-volcan

for f in "$SRC/auth.jar" "$SRC/pedidos.jar" "$SRC/web/index.html"; do
  [[ -f "$f" ]] || { echo "Falta $f"; exit 1; }
done

echo "==> Backend"
systemctl stop gas-volcan-auth gas-volcan-pedidos || true
install -o ubuntu -g ubuntu -m 644 "$SRC/auth.jar" /opt/gas-volcan/auth/app.jar
install -o ubuntu -g ubuntu -m 644 "$SRC/pedidos.jar" /opt/gas-volcan/pedidos/app.jar
systemctl start gas-volcan-auth gas-volcan-pedidos

echo "==> Frontend"
rm -rf /var/www/gas-volcan
mkdir -p /var/www/gas-volcan
cp -a "$SRC/web/." /var/www/gas-volcan/
chown -R www-data:www-data /var/www/gas-volcan
find /var/www/gas-volcan -type d -exec chmod 755 {} \;
find /var/www/gas-volcan -type f -exec chmod 644 {} \;

echo "==> Esperando que arranquen los servicios"
for puerto in 8081 8082; do
  for _ in $(seq 60); do
    ss -lnt | grep -q "127.0.0.1:$puerto " && break
    sleep 2
  done
done
ss -lntp | grep -E ':(80|8081|8082|3306) ' || true
systemctl is-active gas-volcan-auth gas-volcan-pedidos nginx mysql

echo "==> Comprobaciones a traves de Nginx"
curl -s -o /dev/null -w "  GET  /pedidos-service/api/v1/productos -> %{http_code} (esperado 200)\n" http://127.0.0.1/pedidos-service/api/v1/productos
curl -s -o /dev/null -w "  GET  /pedidos-service/api/v1/boletas   -> %{http_code} (esperado 401, sin token)\n" http://127.0.0.1/pedidos-service/api/v1/boletas
curl -s -o /dev/null -w "  GET  /auth-service/api/v1/auth/me      -> %{http_code} (esperado 401, sin token)\n" http://127.0.0.1/auth-service/api/v1/auth/me
curl -s -o /dev/null -w "  GET  /catalogo (React)                 -> %{http_code} (esperado 200)\n" http://127.0.0.1/catalogo
rm -rf "$SRC"
echo "Listo."
