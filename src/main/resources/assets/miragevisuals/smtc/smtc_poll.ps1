# MirageVisuals SMTC poller.
#
# Long-running PowerShell process that streams Windows.Media.Control system
# media transport state to stdout as one compact JSON line per poll (default
# 750ms cadence). The Java side parses each line into a MusicState.
#
# On any failure or when no active session exists we emit the literal
# "null" so the mod side can clearly distinguish "I have no track" from
# "I haven't heard from PowerShell yet".
#
# All diagnostics go into a sibling log file so stdout stays pure JSON
# even when something errors out. Find the log next to the script under
# %LOCALAPPDATA%\MirageVisuals\smtc\smtc_poll.log on Windows.

param(
    [string]$LogPath
)

$ErrorActionPreference = 'Continue'

if (-not $LogPath) {
    $LogPath = Join-Path $PSScriptRoot 'smtc_poll.log'
}

function Log {
    param([string]$msg)
    try {
        $stamp = (Get-Date).ToString('yyyy-MM-dd HH:mm:ss')
        Add-Content -Path $LogPath -Value "[$stamp] $msg" -ErrorAction SilentlyContinue
    } catch { }
}

Log "smtc_poll starting; PS version: $($PSVersionTable.PSVersion); OS: $([Environment]::OSVersion.VersionString)"

try {
    # WinRT projection assembly. Required for AsTask() on IAsyncOperation`1.
    Add-Type -AssemblyName System.Runtime.WindowsRuntime -ErrorAction Stop
} catch {
    Log "Failed to load System.Runtime.WindowsRuntime: $_"
    Write-Output 'null'
    [Console]::Out.Flush()
    exit 1
}

# Resolve WinRT types up-front. The square-bracket type literal with
# ContentType=WindowsRuntime triggers the WinRT projection loader.
try {
    $null = [Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager,Windows.Media.Control,ContentType=WindowsRuntime]
    $null = [Windows.Foundation.IAsyncOperation`1,Windows.Foundation,ContentType=WindowsRuntime]
} catch {
    Log "Failed to project WinRT types: $_"
    Write-Output 'null'
    [Console]::Out.Flush()
    exit 1
}

$asTaskMethod = [System.WindowsRuntimeSystemExtensions].GetMethods() |
    Where-Object {
        $_.Name -eq 'AsTask' -and
        $_.GetParameters().Count -eq 1 -and
        $_.GetParameters()[0].ParameterType.Name -eq 'IAsyncOperation`1'
    } |
    Select-Object -First 1

if ($null -eq $asTaskMethod) {
    Log "Could not locate WindowsRuntimeSystemExtensions.AsTask(IAsyncOperation<T>); aborting."
    Write-Output 'null'
    [Console]::Out.Flush()
    exit 1
}

function Await {
    param($task, $resultType)
    $netTask = $asTaskMethod.MakeGenericMethod($resultType).Invoke($null, @($task))
    $netTask.Wait(-1) | Out-Null
    return $netTask.Result
}

$mgrType    = [Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager]
$propsType  = [Windows.Media.Control.GlobalSystemMediaTransportControlsSessionMediaProperties]
$playingEnum = [Windows.Media.Control.GlobalSystemMediaTransportControlsSessionPlaybackStatus]::Playing

try {
    $mgr = Await ($mgrType::RequestAsync()) $mgrType
    Log "Acquired session manager: $($mgr.GetType().FullName)"
} catch {
    Log "RequestAsync failed: $_"
    Write-Output 'null'
    [Console]::Out.Flush()
    exit 1
}

while ($true) {
    try {
        $session = $mgr.GetCurrentSession()
        if ($null -ne $session) {
            $props    = Await ($session.TryGetMediaPropertiesAsync()) $propsType
            $timeline = $session.GetTimelineProperties()
            $playback = $session.GetPlaybackInfo()

            $obj = [PSCustomObject]@{
                title      = [string]$props.Title
                artist     = [string]$props.Artist
                albumTitle = [string]$props.AlbumTitle
                positionMs = [long]$timeline.Position.TotalMilliseconds
                durationMs = [long]$timeline.EndTime.TotalMilliseconds
                isPlaying  = ($playback.PlaybackStatus -eq $playingEnum)
                source     = [string]$session.SourceAppUserModelId
            }
            Write-Output (ConvertTo-Json $obj -Compress)
        } else {
            Write-Output 'null'
        }
    } catch {
        Log "Poll iteration failed: $_"
        Write-Output 'null'
    }
    [Console]::Out.Flush()
    Start-Sleep -Milliseconds 750
}
