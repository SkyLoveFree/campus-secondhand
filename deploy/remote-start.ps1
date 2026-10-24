#Requires -Version 5.1
[CmdletBinding()]
param(
    [switch]$NoBrowser,
    [int]$TimeoutSec = 60
)

$ErrorActionPreference = 'Stop'
$HostName = '120.76.241.77'
$SshUser  = 'root'
$KeyPath  = Join-Path $env:USERPROFILE '.ssh\campus_deploy'
$Service  = 'campus-secondhand'
$Port     = 8080

function Write-Step($msg) { Write-Host "[远程启动] $msg" -ForegroundColor Cyan }
function Fail($msg) { Write-Host "[错误] $msg" -ForegroundColor Red; exit 1 }

if (-not (Get-Command ssh -ErrorAction SilentlyContinue)) { Fail '本机未找到 ssh 命令。' }
if (-not (Test-Path $KeyPath)) { Fail "未找到 SSH 私钥：$KeyPath`n请确认部署时生成的 campus_deploy 私钥还在。" }

$target = "$SshUser@$HostName"
Write-Step "连接 $target ..."
& ssh -i $KeyPath -o BatchMode=yes -o ConnectTimeout=10 -o StrictHostKeyChecking=accept-new $target "systemctl start $Service && systemctl is-active --quiet $Service"
if ($LASTEXITCODE -ne 0) {
    Fail "启动失败。可执行以下命令查看日志：`nssh -i `"$KeyPath`" $target `"journalctl -u $Service -n 100 --no-pager`""
}

$url = "http://${HostName}:$Port"
Write-Step "等待服务就绪：$url"
$deadline = (Get-Date).AddSeconds($TimeoutSec)
$ready = $false
while ((Get-Date) -lt $deadline) {
    try {
        $resp = Invoke-WebRequest -Uri "$url/api/items" -UseBasicParsing -TimeoutSec 3
        if ($resp.StatusCode -eq 200) { $ready = $true; break }
    } catch { }
    Start-Sleep -Seconds 3
}

if (-not $ready) {
    Fail "服务已执行启动，但 ${TimeoutSec}s 内未通过健康检查。请查看远程日志：`nssh -i `"$KeyPath`" $target `"journalctl -u $Service -n 100 --no-pager`""
}

Write-Host "[远程启动] 已启动：$url" -ForegroundColor Green
Write-Host "[远程启动] 后端服务由 systemd 托管，SSH 断开不会停止，服务器重启后会自动启动。" -ForegroundColor Green
if (-not $NoBrowser) {
    Write-Step "打开浏览器 ..."
    Start-Process $url
}
