# Despliegue en AWS (EC2) — Gas El Volcán

Basado en la Guía 18. React, Auth y Pedidos quedan en una sola instancia EC2 Ubuntu, con dos bases MySQL separadas, dos servicios systemd y Nginx como único punto público.

```text
Internet ──80──▶ Nginx ─┬─ /                       → React (/var/www/gas-volcan)
                        ├─ /auth-service/api/...    → Auth     127.0.0.1:8081/api/...
                        └─ /pedidos-service/api/... → Pedidos  127.0.0.1:8082/api/...
MySQL 8 (solo local): gas_volcan_auth (usuario auth_app) · gas_volcan_pedidos (usuario pedidos_app)
```

| Componente | Puerto | Acceso |
|---|---|---|
| Nginx + React | 80 | Público |
| Auth | 8081 | Solo `127.0.0.1` |
| Pedidos | 8082 | Solo `127.0.0.1` |
| MySQL | 3306 | Solo local |
| SSH | 22 | Solo tu IP |

React llama a las API con rutas relativas (`.env.production`), así el mismo build sirve aunque cambie la IP pública de la instancia.

## Archivos

| Archivo | Para qué |
|---|---|
| `nginx/gas-volcan.conf` | Publica React y reenvía cada prefijo a su backend. Solo expone `/api/`, no Swagger |
| `systemd/gas-volcan-auth.service`, `gas-volcan-pedidos.service` | Un proceso por backend, con reinicio automático |
| `preparar-servidor.sh` | Se corre **una vez**: instala paquetes, crea bases/usuarios, genera secretos y `/etc/gas-volcan/*.env` |
| `instalar-version.sh` | Se corre en **cada publicación**: reemplaza los JAR y el frontend, reinicia y comprueba |
| `publicar.ps1` | Desde Windows: compila los tres proyectos, los copia a la EC2 y llama a los scripts |
| `../gas-el-volcan-*/src/main/resources/application-prod.properties` | Perfil `prod`: escucha en `127.0.0.1` y lee todo desde variables de entorno |

Los secretos (contraseñas de MySQL y `JWT_SECRET`) se generan en el servidor con `openssl` y quedan solo en `/etc/gas-volcan/*.env` (`chmod 600`, dueño root). No están en Git.

## 1. Crear la instancia (consola de AWS, Learner Lab)

1. En el Learner Lab: **Start Lab**, esperar el punto verde y abrir **AWS**.
2. En **AWS Details**, descargar la llave **PEM** (`labsuser.pem`).
3. Ir a **EC2 → Launch instance**:
   - Nombre: `gas-el-volcan`
   - Imagen: **Ubuntu Server 24.04 LTS**
   - Tipo: **t3.medium** (2 servicios Java + MySQL necesitan unos 2 GB de RAM; `t3.small` queda justo)
   - Key pair: **vockey**
   - Network settings → **Edit** → Security group nuevo con dos reglas:
     - SSH, puerto 22, origen **My IP**
     - HTTP, puerto 80, origen **Anywhere (0.0.0.0/0)**
   - **No** abrir 8081, 8082 ni 3306.
   - Almacenamiento: 16 GB.
4. **Launch instance** y copiar la **Public IPv4 address**.
5. (Recomendado) **Elastic IPs → Allocate → Associate** a la instancia, para que la IP no cambie al reiniciar el laboratorio.

## 2. Preparar la llave en Windows (una vez)

```powershell
icacls .\labsuser.pem /inheritance:r
icacls .\labsuser.pem /grant:r "$($env:USERNAME):(R)"
ssh -i .\labsuser.pem ubuntu@IP_PUBLICA     # probar que entra; salir con exit
```

## 3. Publicar (desde Windows, en esta carpeta `deploy`)

Primera vez (prepara el servidor y publica):
```powershell
.\publicar.ps1 -Ip IP_PUBLICA -Llave C:\ruta\labsuser.pem -Front C:\ruta\gas-el-volcan-react -Preparar
```
Pide las contraseñas de los 3 usuarios de prueba (Enter deja las de la demo).

Cada vez que cambie el código (sin `-Preparar`):
```powershell
.\publicar.ps1 -Ip IP_PUBLICA -Llave C:\ruta\labsuser.pem -Front C:\ruta\gas-el-volcan-react
```

Al final el script muestra las comprobaciones a través de Nginx (productos 200, boletas sin token 401, etc.). La aplicación queda en `http://IP_PUBLICA`.

## 4. Swagger sin abrir puertos (túnel SSH)

```powershell
ssh -i C:\ruta\labsuser.pem -L 18081:127.0.0.1:8081 -L 18082:127.0.0.1:8082 ubuntu@IP_PUBLICA
```
Con esa terminal abierta: Auth en `http://localhost:18081/swagger-ui.html` y Pedidos en `http://localhost:18082/swagger-ui.html`.

## 5. Diagnóstico (en la EC2)

```bash
sudo systemctl status gas-volcan-auth gas-volcan-pedidos nginx mysql --no-pager
sudo journalctl -u gas-volcan-auth -n 80 --no-pager
sudo journalctl -u gas-volcan-pedidos -n 80 --no-pager
sudo tail -n 50 /var/log/nginx/error.log
sudo ss -lntp | grep -E '80|8081|8082|3306'
```

| Síntoma | Revisar |
|---|---|
| 502 Bad Gateway | El servicio no arrancó: `journalctl` del servicio |
| 401 en Pedidos con token | `JWT_SECRET` distinto en `auth.env` y `pedidos.env` |
| Access denied for user | `DB_USER`/`DB_PASSWORD` del `.env` del servicio |
| La página no carga | Security group sin el puerto 80, o la IP cambió |
| Recargar `/catalogo` da 404 | Falta `try_files` en Nginx |

## Learner Lab: tener en cuenta

- Al terminar la sesión del laboratorio la instancia **se detiene**. Antes de la presentación: **Start Lab**, esperar 1–2 minutos y abrir `http://IP_PUBLICA`. Los servicios arrancan solos (systemd).
- Sin Elastic IP, la IP pública cambia en cada inicio. El frontend sigue funcionando (rutas relativas); solo hay que usar la IP nueva en el navegador y en `ssh`/`publicar.ps1`.
- Si se crea una instancia nueva, hay que volver a correr `publicar.ps1 ... -Preparar`.
