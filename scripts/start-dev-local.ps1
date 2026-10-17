# start-dev-local.ps1 - One-click local development startup WITHOUT external dependencies
# Backend: default profile + in-memory H2 (no PostgreSQL/Redis/Docker required)
# Frontend: Vite dev server on 127.0.0.1:5173, proxy /api -> http://127.0.0.1:8080
# Usage:
#   powershell -ExecutionPolicy Bypass -File .\scripts\start-dev-local.ps1
#   powershell -ExecutionPolicy Bypass -File .\scripts\start-dev-local.ps1 -SkipFrontend
#   powershell -ExecutionPolicy Bypass -File .\scripts\start-dev-local.ps1 -SkipBackend
[CmdletBinding()]
param(
    [switch]$SkipBackend,
    [switch]$SkipFrontend,
    [int]$BackendPort = 8080,
    [int]$FrontendPort = 5173,
    [string]$JavaHome = "D:\jdk17"
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$ProjectRoot = Split-Path -Parent $PSScriptRoot
$BackendDir = Join-Path $ProjectRoot "backend"
$FrontendDir = Join-Path $ProjectRoot "frontend"
$StandaloneJar = Join-Path $BackendDir "target\dianShangPingTai-1.0.0-SNAPSHOT.jar"
$BackendLog = Join-Path $BackendDir "target\dev-backend.log"
$FrontendLog = Join-Path $FrontendDir "dev-server.log"

function Test-LocalPort {
    param([Parameter(Mandatory = $true)][int]$Port)
    try {
        return (Test-NetConnection -ComputerName "127.0.0.1" -Port $Port -InformationLevel Quiet -WarningAction SilentlyContinue)
    } catch {
        return $false
    }
}

function Wait-PortReady {
    param(
        [Parameter(Mandatory = $true)][int]$Port,
        [int]$RetryCount = 60
    )
    for ($index = 0; $index -lt $RetryCount; $index++) {
        if (Test-LocalPort -Port $Port) {
            return $true
        }
        Start-Sleep -Seconds 1
    }
    return $false
}

function Resolve-JavaHome {
    param([Parameter(Mandatory = $true)][string]$PreferredJavaHome)
    if (Test-Path -LiteralPath $PreferredJavaHome) {
        return $PreferredJavaHome
    }
    $javaCommand = Get-Command java -ErrorAction SilentlyContinue
    if ($null -ne $javaCommand -and -not [string]::IsNullOrWhiteSpace($javaCommand.Source)) {
        $javaBin = Split-Path -Parent $javaCommand.Source
        $detectedJavaHome = Split-Path -Parent $javaBin
        if (Test-Path -LiteralPath $detectedJavaHome) {
            Write-Host ("JAVA_HOME fallback to detected JDK: {0}" -f $detectedJavaHome)
            return $detectedJavaHome
        }
    }
    throw "JAVA_HOME does not exist and no fallback java executable was found: $PreferredJavaHome"
}

function Test-JarNeedsRebuild {
    param([Parameter(Mandatory = $true)][string]$JarPath)
    if (-not (Test-Path -LiteralPath $JarPath)) {
        return $true
    }
    $jarTimestamp = (Get-Item -LiteralPath $JarPath).LastWriteTimeUtc
    $watchTargets = @(
        (Join-Path $BackendDir "pom.xml"),
        (Join-Path $BackendDir "src\main")
    )
    foreach ($target in $watchTargets) {
        if (-not (Test-Path -LiteralPath $target)) {
            continue
        }
        $item = Get-Item -LiteralPath $target
        if ($item.PSIsContainer) {
            $newestChild = Get-ChildItem -LiteralPath $target -Recurse -File |
                Sort-Object LastWriteTimeUtc -Descending |
                Select-Object -First 1
            if ($null -ne $newestChild -and $newestChild.LastWriteTimeUtc -gt $jarTimestamp) {
                return $true
            }
        } elseif ($item.LastWriteTimeUtc -gt $jarTimestamp) {
            return $true
        }
    }
    return $false
}

Write-Host "=== Local no-dependency startup ==="

if (-not $SkipBackend) {
    if (Test-LocalPort -Port $BackendPort) {
        Write-Host ("Backend port {0} is already in use. Assuming backend is already running." -f $BackendPort)
    } else {
        $resolvedJavaHome = Resolve-JavaHome -PreferredJavaHome $JavaHome
        $env:JAVA_HOME = $resolvedJavaHome
        $env:Path = "$resolvedJavaHome\bin;$env:Path"

        $env:APP_AUTH_TOKEN_SECRET = "local-dev-token-secret-please-override-2026"
        $env:APP_AUTH_BOOTSTRAP_PASSWORD = "dev-bootstrap-password-2026"
        $env:MANAGEMENT_HEALTH_REDIS_ENABLED = "false"
        $env:APP_NOTIFICATION_REQUIRE_CONFIGURED_GATEWAY = "false"
        $env:APP_LIVE_RUNTIME_STRICT_PROVIDER_GATE = "false"
        $env:APP_LIVE_RUNTIME_STRICT_CALLBACK_GATE = "false"

        if (Test-JarNeedsRebuild -JarPath $StandaloneJar) {
            Write-Host "Backend jar is missing or stale. Packaging (skip tests)..."
            Push-Location $ProjectRoot
            & mvn -q -f backend/pom.xml -DskipTests package
            $packageExit = $LASTEXITCODE
            Pop-Location
            if ($packageExit -ne 0) {
                throw "Backend package failed (exit $packageExit)."
            }
        }

        Write-Host ("Starting backend jar with default profile (H2 in-memory). Log: {0}" -f $BackendLog)
        $backendProcess = Start-Process -FilePath "java" `
            -ArgumentList @("-jar", $StandaloneJar) `
            -WorkingDirectory $BackendDir `
            -RedirectStandardOutput $BackendLog `
            -RedirectStandardError "$BackendLog.err" `
            -PassThru -WindowStyle Hidden

        if (-not (Wait-PortReady -Port $BackendPort -RetryCount 90)) {
            throw "Backend did not become ready on port $BackendPort within 90s. Check $BackendLog"
        }
        Write-Host ("Backend is ready on http://127.0.0.1:{0} (PID {1})." -f $BackendPort, $backendProcess.Id)
    }
}

if (-not $SkipFrontend) {
    if (Test-LocalPort -Port $FrontendPort) {
        Write-Host ("Frontend port {0} is already in use. Assuming frontend is already running." -f $FrontendPort)
    } else {
        Write-Host ("Starting Vite dev server. Log: {0}" -f $FrontendLog)
        $frontendProcess = Start-Process -FilePath "cmd.exe" `
            -ArgumentList @("/c", "npm run dev -- --port $FrontendPort --host 127.0.0.1 > `"$FrontendLog`" 2>&1") `
            -WorkingDirectory $FrontendDir `
            -PassThru -WindowStyle Hidden

        if (-not (Wait-PortReady -Port $FrontendPort -RetryCount 60)) {
            throw "Frontend dev server did not become ready on port $FrontendPort within 60s. Check $FrontendLog"
        }
        Write-Host ("Frontend is ready on http://127.0.0.1:{0} (PID {1})." -f $FrontendPort, $frontendProcess.Id)
    }
}

Write-Host ""
Write-Host "=== Startup summary ==="
Write-Host ("  Frontend: http://127.0.0.1:{0}   (Vite dev, proxy /api -> 127.0.0.1:{1})" -f $FrontendPort, $BackendPort)
Write-Host ("  Backend:  http://127.0.0.1:{0}/actuator/health" -f $BackendPort)
Write-Host "  Backend log: $BackendLog"
Write-Host "  Frontend log: $FrontendLog"
Write-Host "  Stop everything: powershell -ExecutionPolicy Bypass -File .\scripts\stop-dev-local.ps1"
