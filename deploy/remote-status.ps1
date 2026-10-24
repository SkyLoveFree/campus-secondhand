#Requires -Version 5.1
[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'
$HostName = '120.76.241.77'
$SshUser  = 'root'
$KeyPath  = Join-Path $env:USERPROFILE '.ssh\campus_deploy'
$Service  = 'campus-secondhand'
$Port     = 8080

function Fail($msg) { Write-Host "[错误] $msg" -ForegroundColor Red; exit 1 }

if (-not (Test-Path $KeyPath)) { Fail "未找到 SSH 私钥：$KeyPath" }
$target = "$SshUser@$HostName"

Write-Host "=== 远程服务状态 ===" -ForegroundColor Cyan
& ssh -i $KeyPath -o BatchMode=yes -o ConnectTimeout=10 -o StrictHostKeyChecking=accept-new $target "systemctl --no-pager --full status $Service | head -12; echo '---'; systemctl is-enabled $Service; echo '---'; ss -lntp | grep :$Port || true" 2>&1

Write-Host ""
Write-Host "=== HTTP 健康检查 ===" -ForegroundColor Cyan
$url = "http://${HostName}:$Port"
try {
    $resp = Invoke-WebRequest -Uri "$url/api/items" -UseBasicParsing -TimeoutSec 5
    Write-Host "URL: $url" -ForegroundColor Green
    Write-Host "HTTP: $($resp.StatusCode)" -ForegroundColor Green
} catch {
    Write-Host "URL: $url" -ForegroundColor Yellow
    Write-Host "HTTP: 未通过（服务可能已停止或端口未放行）" -ForegroundColor Yellow
}
