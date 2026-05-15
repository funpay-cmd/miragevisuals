# MirageVisuals SMTC command dispatcher.
#
# Fired once per user action (play / pause / toggle / next / prev). Spawning
# a short-lived process per command keeps the long-running poll script
# single-threaded; the latency is fine for media-control buttons.

param([Parameter(Mandatory = $true)][string]$cmd)

$ErrorActionPreference = 'SilentlyContinue'

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
$mgr = Await ($mgrType::RequestAsync()) $mgrType
$session = $mgr.GetCurrentSession()
if ($null -eq $session) { exit 1 }

$boolType = [bool]

switch ($cmd) {
    'play'   { Await ($session.TryPlayAsync()) $boolType | Out-Null }
    'pause'  { Await ($session.TryPauseAsync()) $boolType | Out-Null }
    'toggle' { Await ($session.TryTogglePlayPauseAsync()) $boolType | Out-Null }
    'next'   { Await ($session.TrySkipNextAsync()) $boolType | Out-Null }
    'prev'   { Await ($session.TrySkipPreviousAsync()) $boolType | Out-Null }
    default  { exit 1 }
}
