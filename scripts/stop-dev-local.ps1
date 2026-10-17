# stop-dev-local.ps1 - Stop backend jar and Vite dev server started by start-dev-local.ps1
# Stops java processes running the standalone backend jar and cmd processes running Vite dev server.
[CmdletBinding()]
param(
    [switch]$All
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$ProjectRoot = Split-Path -Parent $PSScriptRoot
$StandaloneJar = Join-Path $ProjectRoot "backend\target\dianShangPingTai-1.0.0-SNAPSHOT.jar"
$jarName = Split-Path -Leaf $StandaloneJar

Write-Host "=== Stopping local dev processes ==="

# Backend: java -jar <dianShangPingTai-1.0.0-SNAPSHOT.jar>
$backendProcesses = Get-CimInstance Win32_Process -Filter "Name = 'java.exe'" |
    Where-Object { $_.CommandLine -like "*$jarName*" }
foreach ($proc in $backendProcesses) {
    Write-Host ("Stopping backend PID {0} (cmdline matches {1})" -f $proc.ProcessId, $jarName)
    Stop-Process -Id $proc.ProcessId -Force -ErrorAction SilentlyContinue
}

# Frontend: cmd /c npm run dev (Vite). Match cmd processes whose CommandLine contains vite.
$cmdProcesses = Get-CimInstance Win32_Process -Filter "Name = 'cmd.exe'" |
    Where-Object { $_.CommandLine -like "*npm run dev*" }
foreach ($proc in $cmdProcesses) {
    Write-Host ("Stopping frontend launcher PID {0} (npm run dev)" -f $proc.ProcessId)
    Stop-Process -Id $proc.ProcessId -Force -ErrorAction SilentlyContinue
}

# Vite worker: node.exe processes running vite
$nodeProcesses = Get-CimInstance Win32_Process -Filter "Name = 'node.exe'" |
    Where-Object { $_.CommandLine -like "*vite*" }
foreach ($proc in $nodeProcesses) {
    Write-Host ("Stopping Vite node PID {0}" -f $proc.ProcessId)
    Stop-Process -Id $proc.ProcessId -Force -ErrorAction SilentlyContinue
}

Write-Host "Done. Ports 8080/5173 should be free."
