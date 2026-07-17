[CmdletBinding()]
param(
    [ValidateSet("server-local", "tunnel-local", "standalone-local")]
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

function Resolve-JavaHome {
    param(
        [Parameter(Mandatory = $true)]
        [string]$PreferredJavaHome
    )

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

function Ensure-MavenRepoAlias {
    $aliasPath = "E:\ideaCode\dianShangPingTai\backend\target\m2repo"
    $preferredTarget = "E:\开发工具\maven\repository"
    $aliasParent = Split-Path -Parent $aliasPath

    if (-not (Test-Path -LiteralPath $aliasParent)) {
        New-Item -ItemType Directory -Path $aliasParent -Force | Out-Null
    }

    if (Test-Path -LiteralPath $aliasPath) {
        return $aliasPath
    }

    if (Test-Path -LiteralPath $preferredTarget) {
        New-Item -ItemType Junction -Path $aliasPath -Target $preferredTarget | Out-Null
        Write-Host ("Using ASCII Maven repo alias: {0}" -f $aliasPath)
        return $aliasPath
    }

    New-Item -ItemType Directory -Path $aliasPath -Force | Out-Null
    Write-Host ("Using local ASCII Maven repo directory: {0}" -f $aliasPath)
    return $aliasPath
}

function Test-StandaloneJarNeedsRebuild {
    param(
        [Parameter(Mandatory = $true)]
        [string]$JarPath
    )

    if (-not (Test-Path -LiteralPath $JarPath)) {
        return $true
    }

    $jarTimestamp = (Get-Item -LiteralPath $JarPath).LastWriteTimeUtc
    $watchTargets = @(
        "backend\pom.xml",
        "backend\src\main",
        "backend\src\main\resources"
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
} elseif ($Mode -eq "server-local") {
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

$resolvedJavaHome = Resolve-JavaHome -PreferredJavaHome $JavaHome
$env:JAVA_HOME = $resolvedJavaHome
$env:Path = "$resolvedJavaHome\bin;$env:Path"

Set-EnvDefault -Name "APP_AUTH_TOKEN_SECRET" -Value "server-local-token-secret-1234567890"
Set-EnvDefault -Name "APP_AUTH_BOOTSTRAP_PASSWORD" -Value "server-local-bootstrap-password-123"

$mavenRepoPath = Ensure-MavenRepoAlias
$mavenRepoArg = "-Dmaven.repo.local=$mavenRepoPath"

$standaloneJar = "backend\target\dianShangPingTai-1.0.0-SNAPSHOT.jar"
if ($Mode -eq "standalone-local") {
    Set-EnvDefault -Name "MANAGEMENT_HEALTH_REDIS_ENABLED" -Value "false"
    if (Test-StandaloneJarNeedsRebuild -JarPath $standaloneJar) {
        Write-Host "Standalone backend jar is missing or stale. Packaging backend jar first."
        & mvn $mavenRepoArg -q -f backend/pom.xml -DskipTests package
        if ($LASTEXITCODE -ne 0) {
            throw "Standalone backend package failed."
        }
    } else {
        Write-Host "Using existing standalone backend jar."
    }

    Write-Host "Starting standalone backend jar with default profile."
    & java -jar $standaloneJar
    exit $LASTEXITCODE
}

$profiles = $Mode
if ($EnableStage13Local) {
    $profiles = "{0},stage13-local" -f $profiles
}

Write-Host ("Starting Spring Boot with profiles={0}." -f $profiles)
& mvn $mavenRepoArg -f backend/pom.xml spring-boot:run "-Dspring-boot.run.profiles=$profiles"
