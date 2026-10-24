#Requires -Version 5.1
<#
  校园二手交易系统 - 一键启动脚本（Windows）
  用法：
    .\start.ps1                 # 启动（已有 Jar 时直接运行）
    .\start.ps1 -Rebuild        # 重新构建前端 + 后端后再启动
    .\start.ps1 -Port 8080      # 指定端口
    .\start.ps1 -NoBrowser      # 不自动打开浏览器
  也可以双击根目录的「一键启动.bat」。
#>
[CmdletBinding()]
param(
    [switch]$Rebuild,
    [int]$Port = 8080,
    [switch]$NoBrowser
)

$ErrorActionPreference = 'Continue'
$root = if ($PSScriptRoot) { $PSScriptRoot } else { Split-Path -Parent $MyInvocation.MyCommand.Path }
Set-Location $root

try { [Console]::OutputEncoding = [System.Text.Encoding]::UTF8 } catch { }

function Write-Step($msg) { Write-Host "[启动] $msg" -ForegroundColor Cyan }
function Write-Warn($msg) { Write-Host "[提示] $msg" -ForegroundColor Yellow }
function Fail($msg) {
    Write-Host "[错误] $msg" -ForegroundColor Red
    exit 1
}

function Read-Password {
    $secure = Read-Host "MySQL 密码（输入不显示；直接回车表示空密码）" -AsSecureString
    $bstr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure)
    try {
        return [Runtime.InteropServices.Marshal]::PtrToStringBSTR($bstr)
    } finally {
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($bstr)
    }
}

function Find-Jar {
    $dir = Join-Path $root 'backend\target'
    if (-not (Test-Path $dir)) { return $null }
    Get-ChildItem $dir -Filter '*.jar' -ErrorAction SilentlyContinue |
        Where-Object { $_.Name -notmatch 'sources|javadoc|original' } |
        Sort-Object LastWriteTime -Descending |
        Select-Object -First 1
}

Write-Host "========================================" -ForegroundColor Green
Write-Host "  校园二手交易系统 - 一键启动" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green

# 1. 检查 Java
$javaExe = $null
$javaCmd = Get-Command java -ErrorAction SilentlyContinue
if ($javaCmd) {
    $javaExe = $javaCmd.Source
} elseif ($env:JAVA_HOME) {
    $candidate = Join-Path $env:JAVA_HOME 'bin\java.exe'
    if (Test-Path $candidate) { $javaExe = $candidate }
}
if (-not $javaExe) {
    Fail "未找到 java。请安装 JDK 21 并配置 PATH（或 JAVA_HOME）后重试。"
}
$javaVersionLine = (& $javaExe -version 2>&1 | Select-Object -First 1)
Write-Step "Java: $javaVersionLine"

# 2. 检查端口
$conn = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue
if ($conn) {
    $alreadyRunning = $false
    try {
        $resp = Invoke-WebRequest -Uri "http://localhost:$Port/api/items" -UseBasicParsing -TimeoutSec 3
        if ($resp.StatusCode -eq 200) { $alreadyRunning = $true }
    } catch { }

    if ($alreadyRunning) {
        Write-Step "系统已在运行：http://localhost:$Port"
        if (-not $NoBrowser) { Start-Process "http://localhost:$Port" }
        exit 0
    }

    $pids = ($conn | Select-Object -ExpandProperty OwningProcess -Unique) -join ', '
    Write-Warn "端口 $Port 被占用（PID: $pids），且不是正常的本系统实例（可能上次未正常关闭）。"
    $answer = Read-Host "是否停止占用进程并继续？(y/N)"
    if ($answer -match '^[Yy]') {
        foreach ($processId in ($conn | Select-Object -ExpandProperty OwningProcess -Unique)) {
            try { Stop-Process -Id $processId -Force -ErrorAction Stop } catch { }
        }
        Start-Sleep -Seconds 2
    } else {
        Fail "端口被占用，已退出。可手动关闭占用程序后重试。"
    }
}

# 3. 查找/构建 Jar
$jar = Find-Jar
if ($Rebuild -or -not $jar) {
    Write-Step "开始构建（首次或修改代码后需要联网，可能耗时几分钟）..."
    $npmCmd = Get-Command npm -ErrorAction SilentlyContinue
    if (-not $npmCmd) {
        Fail "未找到 npm。请安装 Node.js 20+，或直接使用已构建好的 Jar（不传 -Rebuild）。"
    }

    Push-Location (Join-Path $root 'frontend')
    try {
        if (-not (Test-Path 'node_modules')) {
            Write-Step "安装前端依赖 npm install ..."
            & npm install
            if ($LASTEXITCODE -ne 0) { Fail "npm install 失败。" }
        }
        Write-Step "构建前端 npm run build ..."
        & npm run build
        if ($LASTEXITCODE -ne 0) { Fail "前端构建失败。" }

        $staticDir = Join-Path $root 'backend\src\main\resources\static'
        New-Item -ItemType Directory -Force $staticDir | Out-Null
        Remove-Item "$staticDir\*" -Recurse -Force -ErrorAction SilentlyContinue
        Copy-Item '.\dist\*' -Destination $staticDir -Recurse -Force
    } finally {
        Pop-Location
    }

    Push-Location (Join-Path $root 'backend')
    try {
        Write-Step "打包后端 mvnw package ..."
        & .\mvnw.cmd -q -DskipTests package
        if ($LASTEXITCODE -ne 0) { Fail "后端打包失败。" }
    } finally {
        Pop-Location
    }

    $jar = Find-Jar
}

if (-not $jar) {
    Fail "未找到可运行的 Jar。请确认 backend\target 下存在 campus-secondhand-backend-*.jar，或执行：.\start.ps1 -Rebuild"
}
Write-Step "Jar: $($jar.FullName)"

# 4. 数据库账号：环境变量 > .env.local > 交互输入
$dbUser = $env:DB_USERNAME
$dbPass = $env:DB_PASSWORD
$dbPassSet = ($null -ne $env:DB_PASSWORD)
$promptedPassword = $false

$envFile = Join-Path $root '.env.local'
if (Test-Path $envFile) {
    foreach ($line in (Get-Content $envFile -ErrorAction SilentlyContinue)) {
        if ($line -match '^\s*([^#][^=]*)=(.*)$') {
            $key = $Matches[1].Trim()
            $value = $Matches[2].Trim().Trim('"').Trim("'")
            if ($key -eq 'DB_USERNAME') { $dbUser = $value }
            if ($key -eq 'DB_PASSWORD') { $dbPass = $value; $dbPassSet = $true }
        }
    }
}

if ([string]::IsNullOrWhiteSpace($dbUser)) {
    $inputUser = Read-Host "MySQL 用户名（直接回车默认 root）"
    $dbUser = if ([string]::IsNullOrWhiteSpace($inputUser)) { 'root' } else { $inputUser.Trim() }
}
if (-not $dbPassSet) {
    $dbPass = Read-Password
    $dbPassSet = $true
    $promptedPassword = $true
}

# 5. 数据库预检（mysql 客户端存在时）
$mysqlExe = $null
$mysqlCmd = Get-Command mysql -ErrorAction SilentlyContinue
if ($mysqlCmd) {
    $mysqlExe = $mysqlCmd.Source
} else {
    foreach ($candidate in @(
        'C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe',
        'C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe'
    )) {
        if (Test-Path $candidate) { $mysqlExe = $candidate; break }
    }
}

if ($mysqlExe) {
    Write-Step "检查数据库连接（campus_secondhand）..."
    $oldMysqlPwd = $env:MYSQL_PWD
    $env:MYSQL_PWD = $dbPass
    $dbOutput = & $mysqlExe -u $dbUser -N -B -e "SELECT COUNT(*) FROM campus_secondhand.user;" 2>&1
    $dbExit = $LASTEXITCODE
    if ($null -ne $oldMysqlPwd) { $env:MYSQL_PWD = $oldMysqlPwd } else { Remove-Item Env:MYSQL_PWD -ErrorAction SilentlyContinue }

    if ($dbExit -ne 0) {
        $dbMessage = ($dbOutput | Out-String).Trim()
        if ($dbMessage -match 'Unknown database') {
            Write-Warn "数据库 campus_secondhand 尚未初始化。"
            $initAnswer = Read-Host "是否现在执行 sql\schema.sql + sql\data.sql？（会重建表并写入测试数据，y/N）"
            if ($initAnswer -match '^[Yy]') {
                $env:MYSQL_PWD = $dbPass
                Write-Step "初始化数据库 schema.sql ..."
                & cmd /c "`"$mysqlExe`" -u $dbUser --default-character-set=utf8mb4 < `"$root\sql\schema.sql`""
                if ($LASTEXITCODE -ne 0) {
                    Remove-Item Env:MYSQL_PWD -ErrorAction SilentlyContinue
                    Fail "schema.sql 执行失败。"
                }
                Write-Step "写入测试数据 data.sql ..."
                & cmd /c "`"$mysqlExe`" -u $dbUser --default-character-set=utf8mb4 < `"$root\sql\data.sql`""
                if ($LASTEXITCODE -ne 0) {
                    Remove-Item Env:MYSQL_PWD -ErrorAction SilentlyContinue
                    Fail "data.sql 执行失败。"
                }
                Remove-Item Env:MYSQL_PWD -ErrorAction SilentlyContinue
                Write-Step "数据库初始化完成。"
            } else {
                Fail "数据库未初始化。请先按 README「数据库初始化」执行 SQL，或重新运行脚本并选择 y。"
            }
        } else {
            Fail "数据库连接失败：$dbMessage`n请确认 MySQL 服务已启动、账号密码正确、campus_secondhand 已初始化。"
        }
    } else {
        Write-Step ("数据库连接正常（user 表当前 " + ($dbOutput -join '') + " 行）")
    }
} else {
    Write-Warn "未找到 mysql 客户端，跳过数据库预检；若启动后接口 500，请检查数据库配置。"
}

# 6. 保存本地配置（可选，.env.local 已被 .gitignore 忽略）
if ($promptedPassword -and -not (Test-Path $envFile) -and $dbPass -ne '') {
    $saveAnswer = Read-Host "是否保存到本地 .env.local，下次免输入密码？（仅本机使用、已 gitignore，y/N）"
    if ($saveAnswer -match '^[Yy]') {
        try {
            Set-Content -Path $envFile -Value @("DB_USERNAME=$dbUser", "DB_PASSWORD=$dbPass") -Encoding UTF8
            Write-Step "已保存到 .env.local（不会提交到 Git）。"
        } catch {
            Write-Warn "保存 .env.local 失败：$($_.Exception.Message)"
        }
    }
}

# 7. 启动
$env:DB_USERNAME = $dbUser
$env:DB_PASSWORD = $dbPass

Write-Host ""
Write-Step "服务地址：http://localhost:$Port"
Write-Step "按 Ctrl+C 停止服务；窗口不要关闭。"
if (-not $NoBrowser) {
    Start-Job -ScriptBlock {
        param($url)
        Start-Sleep -Seconds 8
        Start-Process $url
    } -ArgumentList "http://localhost:$Port" | Out-Null
    Write-Step "启动完成后会自动打开浏览器。"
}

try {
    & $javaExe -jar $jar.FullName "--server.port=$Port"
    $exitCode = $LASTEXITCODE
    if ($exitCode -ne 0) {
        Write-Warn "服务已退出，退出码：$exitCode"
    }
} finally {
    Get-Job -ErrorAction SilentlyContinue | Where-Object { $_.State -eq 'Running' } | Stop-Job -ErrorAction SilentlyContinue
    Get-Job -ErrorAction SilentlyContinue | Remove-Job -Force -ErrorAction SilentlyContinue
}
