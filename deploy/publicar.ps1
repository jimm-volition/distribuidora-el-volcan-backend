# Compila Auth, Pedidos y React en Windows, los copia a la EC2 y los instala (Guia 18, pasos 9, 13 y 18).
#
# Uso (PowerShell, desde la carpeta deploy):
#   .\publicar.ps1 -Ip 3.85.10.20 -Llave C:\ruta\labsuser.pem -Front C:\ruta\gas-el-volcan-react
#   .\publicar.ps1 ... -Preparar      # la primera vez: tambien prepara el servidor
param(
    [Parameter(Mandatory = $true)] [string] $Ip,
    [Parameter(Mandatory = $true)] [string] $Llave,
    [Parameter(Mandatory = $true)] [string] $Front,
    [switch] $Preparar
)
$ErrorActionPreference = "Stop"
$Repo = Split-Path -Parent $PSScriptRoot
$Destino = "ubuntu@$Ip"
$Ssh = @("-i", $Llave, "-o", "StrictHostKeyChecking=accept-new")

function Paso($texto) { Write-Host "`n==> $texto" -ForegroundColor Cyan }
function Revisar($texto) { if ($LASTEXITCODE -ne 0) { throw "Fallo: $texto" } }

Paso "Compilando Auth (con pruebas)"
Push-Location "$Repo\gas-el-volcan-auth"; .\mvnw.cmd -q clean package; Revisar "Auth"; Pop-Location

Paso "Compilando Pedidos (con pruebas)"
Push-Location "$Repo\gas-el-volcan-pedidos"; .\mvnw.cmd -q clean package; Revisar "Pedidos"; Pop-Location

Paso "Compilando React (usa .env.production)"
Push-Location $Front; npm run build; Revisar "React"; Pop-Location

$JarAuth = Get-ChildItem "$Repo\gas-el-volcan-auth\target\*.jar" | Where-Object Name -notlike "*.original" | Select-Object -First 1
$JarPedidos = Get-ChildItem "$Repo\gas-el-volcan-pedidos\target\*.jar" | Where-Object Name -notlike "*.original" | Select-Object -First 1

Paso "Copiando a la EC2 ($Ip)"
ssh @Ssh $Destino "rm -rf /tmp/gas-volcan && mkdir -p /tmp/gas-volcan"; Revisar "ssh"
scp @Ssh $JarAuth.FullName "${Destino}:/tmp/gas-volcan/auth.jar"; Revisar "scp auth"
scp @Ssh $JarPedidos.FullName "${Destino}:/tmp/gas-volcan/pedidos.jar"; Revisar "scp pedidos"
scp @Ssh -r "$Front\dist" "${Destino}:/tmp/gas-volcan/web"; Revisar "scp react"
scp @Ssh -r "$PSScriptRoot" "${Destino}:/tmp/gas-volcan/deploy"; Revisar "scp deploy"
# Por si Git dejo los scripts con saltos de linea de Windows (CRLF), que bash no acepta.
ssh @Ssh $Destino "sed -i 's/\r$//' /tmp/gas-volcan/deploy/*.sh /tmp/gas-volcan/deploy/*/*"; Revisar "ssh"

if ($Preparar) {
    Paso "Preparando el servidor (solo la primera vez)"
    ssh -t @Ssh $Destino "sudo bash /tmp/gas-volcan/deploy/preparar-servidor.sh"; Revisar "preparar"
}

Paso "Instalando la version"
ssh @Ssh $Destino "sudo bash /tmp/gas-volcan/deploy/instalar-version.sh"; Revisar "instalar"

Write-Host "`nPublicado: http://$Ip" -ForegroundColor Green
