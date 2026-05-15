# MirageVisuals SMTC poller.
#
# Long-running PowerShell process that streams Windows.Media.Control system
# media transport state to stdout as one compact JSON line per poll (default
# 750ms cadence). The Java side parses each line into a MusicState.
#
# On any failure or when no active session exists we emit the literal
# "null" so the mod side can clearly distinguish "I have no track" from
# "I haven't heard from PowerShell yet".

$ErrorActionPreference = 'SilentlyContinue'

# Force the WinRT type to load up-front; subsequent ::asm-loads are no-ops.
$null = [Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager, Windows.Media.Control, ContentType = WindowsRuntime]
$null = [Windows.Foundation.IAsyncOperation`1, Windows.Foundation, ContentType = WindowsRuntime]

function Await {
    param($task, $resultType)
    $asTaskMethod = [System.WindowsRuntimeSystemExtensions].GetMethods() |
        Where-Object {
            $_.Name -eq 'AsTask' -and
            $_.GetParameters().Count -eq 1 -and
            $_.GetParameters()[0].ParameterType.Name -eq 'IAsyncOperation`1'
        } |
        Select-Object -First 1
    $netTask = $asTaskMethod.MakeGenericMethod($resultType).Invoke($null, @($task))
    $netTask.Wait(-1) | Out-Null
    return $netTask.Result
}

$mgrType = [Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager]
$propsType = [Windows.Media.Control.GlobalSystemMediaTransportControlsSessionMediaProperties]

try {
    $mgr = Await ($mgrType::RequestAsync()) $mgrType
} catch {
    Write-Output 'null'
    [Console]::Out.Flush()
    exit
}

while ($true) {
    try {
        $session = $mgr.GetCurrentSession()
        if ($null -ne $session) {
            $props = Await ($session.TryGetMediaPropertiesAsync()) $propsType
            $timeline = $session.GetTimelineProperties()
            $playback = $session.GetPlaybackInfo()
            $obj = [PSCustomObject]@{
                title      = [string]$props.Title
                artist     = [string]$props.Artist
                albumTitle = [string]$props.AlbumTitle
                positionMs = [long]$timeline.Position.TotalMilliseconds
                durationMs = [long]$timeline.EndTime.TotalMilliseconds
                isPlaying  = ($playback.PlaybackStatus -eq [Windows.Media.Control.GlobalSystemMediaTransportControlsSessionPlaybackStatus]::Playing)
                source     = [string]$session.SourceAppUserModelId
            }
            Write-Output (ConvertTo-Json $obj -Compress)
        } else {
            Write-Output 'null'
        }
    } catch {
        Write-Output 'null'
    }
    [Console]::Out.Flush()
    Start-Sleep -Milliseconds 750
}
