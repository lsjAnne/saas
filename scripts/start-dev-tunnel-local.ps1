[CmdletBinding()]
param(
    [ValidateSet("server-local", "tunnel-local")]
    [string]$Mode = "server-local",

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

    [switch]$ReuseExistingTunnel,

    [switch]$EnableStage13Local
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

function Set-EnvDefault {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Name,

        [Parameter(Mandatory = $true)]
        [string]$Value
    )

    $currentValue = [Environment]::GetEnvironmentVariable($Name, "Process")
    if (-not [string]::IsNullOrWhiteSpace($currentValue)) {
        return
    }

    [Environment]::SetEnvironmentVariable($Name, $Value, "Process")
    Write-Host ("Using local startup default for {0}." -f $Name)
}

if ($Mode -eq "tunnel-local") {
    if (-not $SshUser) {
        throw "SshUser is required when Mode=tunnel-local."
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
}

if ($Mode -eq "tunnel-local") {
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
} else {
    if (-not (Wait-PortReady -Port $RemotePostgresPort)) {
        throw "Local PostgreSQL port $RemotePostgresPort is not ready. Please start the local Docker database first."
    }

    if (-not (Wait-PortReady -Port $RemoteRedisPort)) {
        throw "Local Redis port $RemoteRedisPort is not ready. Please start the local Docker Redis first."
    }

    Write-Host ("Detected local Docker dependencies. PostgreSQL={0}, Redis={1}" -f $RemotePostgresPort, $RemoteRedisPort)
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

Set-EnvDefault -Name "APP_AUTH_TOKEN_SECRET" -Value "server-local-token-secret-1234567890"
Set-EnvDefault -Name "APP_AUTH_BOOTSTRAP_PASSWORD" -Value "server-local-bootstrap-password-123"

$profiles = $Mode
if ($EnableStage13Local) {
    $profiles = "{0},stage13-local" -f $profiles
}

Write-Host ("Starting Spring Boot with profiles={0}." -f $profiles)
& mvn spring-boot:run "-Dspring-boot.run.profiles=$profiles"
