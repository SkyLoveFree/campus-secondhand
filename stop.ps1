#Requires -Version 5.1
<#
  停止由 start.ps1 / 一键启动.bat 启动的服务。
  用法：.\stop.ps1 [-Port 8080]
#>
[CmdletBinding()]
param(
    [int]$Port = 8080
)

$ErrorActionPreference = 'Stop'
try { [Console]::OutputEncoding = [System.Text.Encoding]::UTF8 } catch { }

$conn = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue
if (-not $conn) {
    Write-Host "[停止] 端口 $Port 没有正在监听的进程。" -ForegroundColor Yellow
    exit 0
}

$pids = $conn | Select-Object -ExpandProperty OwningProcess -Unique
Write-Host "[停止] 正在停止端口 $Port 的进程（PID: $($pids -join ', ')）..." -ForegroundColor Cyan
foreach ($processId in $pids) {
    try {
        Stop-Process -Id $processId -Force -ErrorAction Stop
    } catch {
        Write-Host "[提示] 无法停止 PID ${processId}: $($_.Exception.Message)" -ForegroundColor Yellow
    }
}

Start-Sleep -Seconds 2
if (Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue) {
    Write-Host "[提示] 端口 $Port 仍有进程监听，请检查权限或手动结束。" -ForegroundColor Yellow
    exit 1
}

Write-Host "[停止] 已停止。" -ForegroundColor Green
