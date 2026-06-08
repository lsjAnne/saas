[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [string]$SshUser,

    [string]$SshHost = "192.168.112.101",

    [string]$SshKeyPath,

    [string]$JavaHome = "D:\jdk17",

    [int]$LocalPostgresPort = 15432,

    [int]$LocalRedisPort = 16379,

    [string]$RemoteHost = "127.0.0.1",

    [int]$RemotePostgresPort = 5432,

    [int]$RemoteRedisPort = 6379,

    [switch]$SkipAppStart,

    [switch]$ReuseExistingTunnel
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

function Test-LocalPort {
    param(
        [Parameter(Mandatory = $true)]
        [int]$Port
    )

    try {
        return (Test-NetConnection -ComputerName "127.0.0.1" -Port $Port -InformationLevel Quiet -WarningAction SilentlyContinue)
    } catch {
        return $false
    }
}

function Wait-PortReady {
    param(
        [Parameter(Mandatory = $true)]
        [int]$Port,

        [int]$RetryCount = 20
    )

    for ($index = 0; $index -lt $RetryCount; $index++) {
        if (Test-LocalPort -Port $Port) {
            return $true
        }
        Start-Sleep -Seconds 1
    }

    return $false
}

function Start-SshTunnel {
    param(
        [Parameter(Mandatory = $true)]
        [string]$User,

        [Parameter(Mandatory = $true)]
        [string]$Host
    )

    $sshTarget = "${User}@${Host}"
    $arguments = @()

    if ($SshKeyPath) {
        $arguments += @(
            "-i"
            $SshKeyPath
            "-o"
            "IdentitiesOnly=yes"
        )
    }

    $arguments += @(
        "-N"
        "-L"
        ("{0}:{1}:{2}" -f $LocalPostgresPort, $RemoteHost, $RemotePostgresPort)
        "-L"
        ("{0}:{1}:{2}" -f $LocalRedisPort, $RemoteHost, $RemoteRedisPort)
        $sshTarget
    )

    Write-Host ("Starting SSH tunnel: {0}" -f ($arguments -join " "))
    return Start-Process -FilePath "ssh" -ArgumentList $arguments -PassThru -WindowStyle Hidden
}

if (-not $SshKeyPath) {
    $defaultCustomKey = Join-Path $HOME ".ssh\openclaw_tunnel_ed25519"
    if (Test-Path -LiteralPath $defaultCustomKey) {
        $SshKeyPath = $defaultCustomKey
        Write-Host ("Detected SSH key: {0}" -f $SshKeyPath)
    }
}

if ($SshKeyPath -and -not (Test-Path -LiteralPath $SshKeyPath)) {
    throw "SSH key does not exist: $SshKeyPath"
}

$postgresReady = Test-LocalPort -Port $LocalPostgresPort
$redisReady = Test-LocalPort -Port $LocalRedisPort

if (-not ($postgresReady -and $redisReady)) {
    if ($ReuseExistingTunnel) {
        throw "ReuseExistingTunnel was set, but tunnel ports are not ready."
    }

    $tunnelProcess = Start-SshTunnel -User $SshUser -Host $SshHost

    if (-not (Wait-PortReady -Port $LocalPostgresPort)) {
        throw "SSH tunnel started, but local PostgreSQL port $LocalPostgresPort is not ready."
    }

    if (-not (Wait-PortReady -Port $LocalRedisPort)) {
        throw "SSH tunnel started, but local Redis port $LocalRedisPort is not ready."
    }

    Write-Host ("SSH tunnel is ready. PID: {0}" -f $tunnelProcess.Id)
} else {
    Write-Host "Detected ready local tunnel ports. Skipping tunnel creation."
}

if ($SkipAppStart) {
    Write-Host "SkipAppStart was set. Spring Boot startup skipped."
    return
}

if (-not (Test-Path -LiteralPath $JavaHome)) {
    throw "JAVA_HOME does not exist: $JavaHome"
}

$env:JAVA_HOME = $JavaHome
$env:Path = "$JavaHome\bin;$env:Path"

Write-Host "Starting Spring Boot with profile=tunnel-local."
& mvn spring-boot:run "-Dspring-boot.run.profiles=tunnel-local"
