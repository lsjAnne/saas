[CmdletBinding()]
param(
    [int]$LocalPostgresPort = 15432,

    [int]$LocalRedisPort = 16379
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$postgresPattern = "{0}:127.0.0.1:5432" -f $LocalPostgresPort
$redisPattern = "{0}:127.0.0.1:6379" -f $LocalRedisPort

$processes = Get-CimInstance Win32_Process |
    Where-Object {
        $_.Name -eq "ssh.exe" -and
        $_.CommandLine -like "*$postgresPattern*" -and
        $_.CommandLine -like "*$redisPattern*"
    }

if (-not $processes) {
    Write-Host "No matching SSH tunnel process was found."
    return
}

foreach ($process in $processes) {
    Write-Host ("Stopping SSH tunnel PID: {0}" -f $process.ProcessId)
    Stop-Process -Id $process.ProcessId -Force
}
