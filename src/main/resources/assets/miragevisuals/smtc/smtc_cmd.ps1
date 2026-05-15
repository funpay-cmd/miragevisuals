# MirageVisuals SMTC command dispatcher.
#
# Fired once per user action (play / pause / toggle / next / prev). Spawning
# a short-lived process per command keeps the long-running poll script
# single-threaded; the latency is fine for media-control buttons.

param(
    [Parameter(Mandatory = $true)][string]$cmd,
    [string]$LogPath
)

$ErrorActionPreference = 'Continue'

if (-not $LogPath) {
    $LogPath = Join-Path $PSScriptRoot 'smtc_cmd.log'
}

function Log {
    param([string]$msg)
    try {
        $stamp = (Get-Date).ToString('yyyy-MM-dd HH:mm:ss')
        Add-Content -Path $LogPath -Value "[$stamp] $msg" -ErrorAction SilentlyContinue
    } catch { }
}

try {
    Add-Type -AssemblyName System.Runtime.WindowsRuntime -ErrorAction Stop
} catch {
    Log "Failed to load System.Runtime.WindowsRuntime: $_"
    exit 1
}

try {
    $null = [Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager,Windows.Media.Control,ContentType=WindowsRuntime]
    $null = [Windows.Foundation.IAsyncOperation`1,Windows.Foundation,ContentType=WindowsRuntime]
} catch {
    Log "Failed to project WinRT types: $_"
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
    Log "Could not locate WindowsRuntimeSystemExtensions.AsTask(IAsyncOperation<T>)."
    exit 1
}

function Await {
    param($task, $resultType)
    $netTask = $asTaskMethod.MakeGenericMethod($resultType).Invoke($null, @($task))
    $netTask.Wait(-1) | Out-Null
    return $netTask.Result
}

try {
    $mgrType = [Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager]
    $mgr = Await ($mgrType::RequestAsync()) $mgrType
    $session = $mgr.GetCurrentSession()
    if ($null -eq $session) {
        Log "No active SMTC session for command '$cmd'."
        exit 1
    }

    $boolType = [bool]

    switch ($cmd) {
        'play'   { Await ($session.TryPlayAsync()) $boolType | Out-Null }
        'pause'  { Await ($session.TryPauseAsync()) $boolType | Out-Null }
        'toggle' { Await ($session.TryTogglePlayPauseAsync()) $boolType | Out-Null }
        'next'   { Await ($session.TrySkipNextAsync()) $boolType | Out-Null }
        'prev'   { Await ($session.TrySkipPreviousAsync()) $boolType | Out-Null }
        default  { Log "Unknown command '$cmd'."; exit 1 }
    }
    Log "Dispatched '$cmd' OK."
} catch {
    Log "Command '$cmd' failed: $_"
    exit 1
}
