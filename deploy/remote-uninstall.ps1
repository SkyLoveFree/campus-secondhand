#Requires -Version 5.1
[CmdletBinding()]
param(
    [switch]$Force,
    [switch]$DryRun,
    [switch]$RemoveDatabase
)

$ErrorActionPreference = 'Stop'
$HostName = '120.76.241.77'
$SshUser  = 'root'
$KeyPath  = Join-Path $env:USERPROFILE '.ssh\campus_deploy'
$Service  = 'campus-secondhand'
$AppDir   = '/opt/campus-secondhand'
$UnitFile = '/etc/systemd/system/campus-secondhand.service'
$Port     = 8080

function Write-Step($msg) { Write-Host "[远程卸载] $msg" -ForegroundColor Cyan }
function Fail($msg) { Write-Host "[错误] $msg" -ForegroundColor Red; exit 1 }

if (-not (Get-Command ssh -ErrorAction SilentlyContinue)) { Fail '本机未找到 ssh 命令。' }
if (-not (Test-Path $KeyPath)) { Fail "未找到 SSH 私钥：$KeyPath" }

Write-Host "即将从 $HostName 卸载："
Write-Host "  - systemd 服务 $Service（停止 + 取消开机自启 + 删除 unit）"
Write-Host "  - 应用目录 $AppDir（Jar、SQL、配置）"
Write-Host "  - 默认保留 MySQL 数据库 campus_secondhand 与系统 JDK/MySQL"
Write-Host ""

$dropDb = $RemoveDatabase
if (-not $DryRun) {
    if (-not $Force) {
        $answer = Read-Host "确认卸载请输入 UNINSTALL（其他任意输入取消）"
        if ($answer -ne 'UNINSTALL') {
            Write-Host "[远程卸载] 已取消。" -ForegroundColor Yellow
            exit 0
        }
    }

    if (-not $Force -and -not $RemoveDatabase) {
        $dbAnswer = Read-Host "是否同时删除数据库 campus_secondhand 和数据库用户 campus？(y/N，默认保留)"
        if ($dbAnswer -match '^[Yy]') { $dropDb = $true }
    }
}

$lines = @(
    "systemctl stop $Service || true",
    "systemctl disable $Service || true",
    "rm -f $UnitFile",
    "systemctl daemon-reload",
    "rm -rf $AppDir",
    "systemctl reset-failed $Service 2>/dev/null || true"
)
if ($dropDb) {
    $lines += "mysql -uroot -e `"DROP DATABASE IF EXISTS campus_secondhand; DROP USER IF EXISTS 'campus'@'localhost'; FLUSH PRIVILEGES;`""
}
$lines += "echo UNINSTALL_DONE"
$remoteScript = ($lines -join "`n")

if ($DryRun) {
    Write-Step "DryRun：将执行以下远程脚本（不实际执行）："
    Write-Host $remoteScript
    exit 0
}

$b64 = [Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes($remoteScript))
$target = "$SshUser@$HostName"
Write-Step "连接 $target 并卸载 ..."
$output = & ssh -i $KeyPath -o BatchMode=yes -o ConnectTimeout=10 -o StrictHostKeyChecking=accept-new $target "echo $b64 | base64 -d | bash" 2>&1
$output | Write-Host
if ($LASTEXITCODE -ne 0 -or ($output -join "`n") -notmatch 'UNINSTALL_DONE') {
    Fail "卸载未完整完成。请手动检查：`nssh -i `"$KeyPath`" $target `"systemctl status $Service; ls -ld $AppDir`""
}

Write-Host "[远程卸载] 完成。服务已停止、开机自启已取消、应用目录已删除。" -ForegroundColor Green
if ($dropDb) { Write-Host "[远程卸载] 数据库和数据库用户已删除。" -ForegroundColor Yellow }
else { Write-Host "[远程卸载] 数据库 campus_secondhand 保留；如需彻底清理可重新执行并选择删除数据库。" -ForegroundColor Yellow }
Write-Host "[远程卸载] 未卸载系统 JDK/MySQL（其他项目可能共用）。" -ForegroundColor Yellow
