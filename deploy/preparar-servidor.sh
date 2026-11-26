#!/usr/bin/env bash
# Prepara una instancia EC2 Ubuntu UNA sola vez (Guia 18, pasos 6, 10, 11 y 14):
#   - instala MySQL 8, Java 21, Nginx
#   - crea dos bases y dos usuarios MySQL separados
#   - genera las contrasenas de base y el JWT_SECRET (no quedan en Git ni en la pantalla)
#   - crea /etc/gas-volcan/auth.env y pedidos.env (chmod 600, solo root)
#   - instala los servicios systemd y el sitio de Nginx
# Uso (en la EC2):  sudo bash /tmp/gas-volcan/deploy/preparar-servidor.sh
# Se puede volver a correr: no pisa los .env ni las contrasenas que ya existen.
set -euo pipefail

if [[ $EUID -ne 0 ]]; then echo "Ejecutar con sudo"; exit 1; fi
DIR="$(cd "$(dirname "$0")" && pwd)"
ENV_DIR=/etc/gas-volcan

echo "==> Instalando paquetes"
export DEBIAN_FRONTEND=noninteractive
apt-get update -q
apt-get install -y -q mysql-server openjdk-21-jre-headless nginx curl openssl
systemctl enable --now mysql nginx

echo "==> IP publica de la instancia"
TOKEN=$(curl -s -X PUT http://169.254.169.254/latest/api/token -H "X-aws-ec2-metadata-token-ttl-seconds: 60")
IP=$(curl -s -H "X-aws-ec2-metadata-token: $TOKEN" http://169.254.169.254/latest/meta-data/public-ipv4 || true)
echo "    ${IP:-no disponible}"

mkdir -p "$ENV_DIR"
chmod 755 "$ENV_DIR"

if [[ ! -f "$ENV_DIR/auth.env" || ! -f "$ENV_DIR/pedidos.env" ]]; then
  echo "==> Generando secretos"
  JWT_SECRET=$(openssl rand -base64 32)
  CLAVE_AUTH=$(openssl rand -hex 16)
  CLAVE_PEDIDOS=$(openssl rand -hex 16)

  echo "Contrasenas de los usuarios de prueba (Enter = valor de la demo):"
  read -r -p "  admin@volcan.cl    [Admin1234]: " SEED_ADMIN;    SEED_ADMIN=${SEED_ADMIN:-Admin1234}
  read -r -p "  vendedor@volcan.cl [Vendedor1]: " SEED_VENDEDOR; SEED_VENDEDOR=${SEED_VENDEDOR:-Vendedor1}
  read -r -p "  cliente@volcan.cl  [Cliente123]: " SEED_CLIENTE; SEED_CLIENTE=${SEED_CLIENTE:-Cliente123}

  echo "==> Creando bases y usuarios MySQL (uno por servicio)"
  mysql <<SQL
CREATE DATABASE IF NOT EXISTS gas_volcan_auth CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS gas_volcan_pedidos CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS 'auth_app'@'localhost' IDENTIFIED BY '${CLAVE_AUTH}';
CREATE USER IF NOT EXISTS 'pedidos_app'@'localhost' IDENTIFIED BY '${CLAVE_PEDIDOS}';
ALTER USER 'auth_app'@'localhost' IDENTIFIED BY '${CLAVE_AUTH}';
ALTER USER 'pedidos_app'@'localhost' IDENTIFIED BY '${CLAVE_PEDIDOS}';
GRANT ALL PRIVILEGES ON gas_volcan_auth.* TO 'auth_app'@'localhost';
GRANT ALL PRIVILEGES ON gas_volcan_pedidos.* TO 'pedidos_app'@'localhost';
FLUSH PRIVILEGES;
SQL

  echo "==> Escribiendo $ENV_DIR/auth.env y pedidos.env"
  umask 077
  cat > "$ENV_DIR/auth.env" <<ENV
SPRING_PROFILES_ACTIVE=prod
DB_URL=jdbc:mysql://localhost:3306/gas_volcan_auth?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=America/Santiago
DB_USER=auth_app
DB_PASSWORD=${CLAVE_AUTH}
JWT_SECRET=${JWT_SECRET}
JWT_ISSUER=gas-el-volcan-auth
FRONTEND_URL=http://${IP:-localhost}
SEED_ADMIN_PASSWORD=${SEED_ADMIN}
SEED_VENDEDOR_PASSWORD=${SEED_VENDEDOR}
SEED_CLIENTE_PASSWORD=${SEED_CLIENTE}
ENV
  cat > "$ENV_DIR/pedidos.env" <<ENV
SPRING_PROFILES_ACTIVE=prod
DB_URL=jdbc:mysql://localhost:3306/gas_volcan_pedidos?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=America/Santiago
DB_USER=pedidos_app
DB_PASSWORD=${CLAVE_PEDIDOS}
JWT_SECRET=${JWT_SECRET}
JWT_ISSUER=gas-el-volcan-auth
FRONTEND_URL=http://${IP:-localhost}
ENV
  chown root:root "$ENV_DIR"/*.env
  chmod 600 "$ENV_DIR"/*.env
else
  echo "==> $ENV_DIR/*.env ya existen: se conservan"
fi

echo "==> Instalando servicios systemd"
mkdir -p /opt/gas-volcan/auth /opt/gas-volcan/pedidos
chown -R ubuntu:ubuntu /opt/gas-volcan
install -m 644 "$DIR/systemd/gas-volcan-auth.service" /etc/systemd/system/
install -m 644 "$DIR/systemd/gas-volcan-pedidos.service" /etc/systemd/system/
systemctl daemon-reload
systemctl enable gas-volcan-auth gas-volcan-pedidos

echo "==> Configurando Nginx"
mkdir -p /var/www/gas-volcan
install -m 644 "$DIR/nginx/gas-volcan.conf" /etc/nginx/sites-available/gas-volcan
ln -sfn /etc/nginx/sites-available/gas-volcan /etc/nginx/sites-enabled/gas-volcan
rm -f /etc/nginx/sites-enabled/default
nginx -t
systemctl reload nginx

echo
echo "Servidor preparado. Siguiente paso: publicar la aplicacion (deploy/publicar.ps1)."
ls -l "$ENV_DIR"
