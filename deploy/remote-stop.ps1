#Requires -Version 5.1
[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'
$HostName = '120.76.241.77'
$SshUser  = 'root'
$KeyPath  = Join-Path $env:USERPROFILE '.ssh\campus_deploy'
$Service  = 'campus-secondhand'
$Port     = 8080

function Write-Step($msg) { Write-Host "[远程停止] $msg" -ForegroundColor Cyan }
function Fail($msg) { Write-Host "[错误] $msg" -ForegroundColor Red; exit 1 }

if (-not (Get-Command ssh -ErrorAction SilentlyContinue)) { Fail '本机未找到 ssh 命令。' }
if (-not (Test-Path $KeyPath)) { Fail "未找到 SSH 私钥：$KeyPath" }

$target = "$SshUser@$HostName"
Write-Step "停止 $target 上的 $Service ..."
& ssh -i $KeyPath -o BatchMode=yes -o ConnectTimeout=10 -o StrictHostKeyChecking=accept-new $target "systemctl stop $Service; systemctl is-active --quiet $Service && echo STILL_ACTIVE || echo STOPPED"
if ($LASTEXITCODE -ne 0) {
    Fail "停止失败。请检查 SSH 或手动执行：`nssh -i `"$KeyPath`" $target `"systemctl status $Service`""
}

Write-Host "[远程停止] 已停止（服务仍保留，可用 远程启动.bat 重新启动）。" -ForegroundColor Green
Write-Host "[远程停止] 地址 $HostName`:$Port 将不可访问，这是预期行为。" -ForegroundColor Yellow
