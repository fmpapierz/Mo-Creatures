# Safety net for unattended test clients (user rule: a test window never comes to the front). Run it next to a test
# launch: it watches the top-level windows of this project's java processes and, whenever one shows un-minimized or
# in front on its own, puts it away at once and records the failure. A correct launch (the agent in
# testclient-agent.jar) never trips it, so any "VIOLATION" line means the agent needs fixing. Every change of a
# window's state is logged with a timestamp, to line it up with the game log.
#
#     powershell -NoProfile -ExecutionPolicy Bypass -File tools/testclient/windowguard.ps1 [-Seconds 900] [-Log <file>]
#
# The user may look at a test window (click its taskbar button, Alt+Tab to it), so a change that follows the user's
# own keyboard or mouse input within $UserInputMs is logged as "USER" and left alone, and that window is never put
# away again. Only a change with no input just before it is the game's doing. A foreground window is minimized with
# SW_MINIMIZE, which hands the focus back to the next window in the Z order (the user's); a window that is only
# un-minimized gets SW_SHOWMINNOACTIVE, which never activates anything. Each window is put away at most once (an
# always-on re-minimizer makes windows impossible to look at); later violations are only logged. Only java processes
# whose command line contains this project's folder are looked at (machine rule on processes), plus their java
# children (Fabric forks a JVM for its error window). Stops after -Seconds, or 30 seconds after the last such process
# has exited.
# Two rules added 2026-10-09 after a Quilt "Minecraft failed to launch!" window slipped through while the user typed:
#   - a window seen for the FIRST time is never the user's, whatever input came just before it;
#   - any visible window whose title is not the game's ("Minecraft...") is logged as "DIALOG PID ..." - a loader error
#     window or crash box. tools/testclient/launchwatch.py reads that line and the test scripts then kill the launch.
param([int]$Seconds = 900, [string]$Log = '', [int]$UserInputMs = 1500)
$project = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
Add-Type @"
using System;
using System.Runtime.InteropServices;
using System.Text;
public static class TestClientGuard {
    public delegate bool EnumProc(IntPtr hwnd, IntPtr lParam);
    [DllImport("user32.dll")] public static extern bool EnumWindows(EnumProc callback, IntPtr lParam);
    [DllImport("user32.dll")] public static extern uint GetWindowThreadProcessId(IntPtr hwnd, out uint pid);
    [DllImport("user32.dll")] public static extern bool IsWindowVisible(IntPtr hwnd);
    [DllImport("user32.dll")] public static extern bool IsIconic(IntPtr hwnd);
    [DllImport("user32.dll")] public static extern IntPtr GetForegroundWindow();
    [DllImport("user32.dll")] public static extern bool ShowWindow(IntPtr hwnd, int command);
    [DllImport("user32.dll", CharSet = CharSet.Unicode)] public static extern int GetWindowText(IntPtr hwnd, StringBuilder text, int max);
    [StructLayout(LayoutKind.Sequential)] public struct LastInputInfo { public uint cbSize; public uint dwTime; }
    [DllImport("user32.dll")] static extern bool GetLastInputInfo(ref LastInputInfo info);
    [DllImport("kernel32.dll")] static extern uint GetTickCount();
    // Milliseconds since the user's last keyboard or mouse input (in this session).
    public static uint IdleMs() {
        LastInputInfo info = new LastInputInfo();
        info.cbSize = (uint) Marshal.SizeOf(info);
        return GetLastInputInfo(ref info) ? GetTickCount() - info.dwTime : uint.MaxValue;
    }
}
"@
function Write-Guard([string]$line) {
    $stamped = '{0:HH:mm:ss.fff} {1}' -f (Get-Date), $line
    $stamped
    if ($Log) { Add-Content -Path $Log -Value $stamped -Encoding utf8 }
}
$states = @{}
$putAways = @{}
$userWindows = @{}
$dialogs = @{}
$violations = 0
$deadline = (Get-Date).AddSeconds($Seconds)
$lastAlive = $null
$sawProcess = $false
Write-Guard "watching java windows of $project"
while ((Get-Date) -lt $deadline) {
    $javas = @(Get-CimInstance Win32_Process -Filter "Name='java.exe' OR Name='javaw.exe'")
    $own = @($javas | Where-Object { $_.CommandLine -and $_.CommandLine.Contains($project) } | ForEach-Object { [uint32]$_.ProcessId })
    # Plus their java children: Fabric Loader opens its error window in a forked JVM whose command line need not
    # name this project.
    $ids = @($own + @($javas | Where-Object { $own -contains [uint32]$_.ParentProcessId } |
            ForEach-Object { [uint32]$_.ProcessId }) | Select-Object -Unique)
    if ($ids.Count -gt 0) { $sawProcess = $true; $lastAlive = Get-Date }
    elseif ($sawProcess -and ((Get-Date) - $lastAlive).TotalSeconds -gt 30) { break }
    # Several quick looks per process listing (the CIM query is the slow part).
    for ($tick = 0; $tick -lt 8; $tick++) {
        $foreground = [TestClientGuard]::GetForegroundWindow()
        $script:events = New-Object System.Collections.Generic.List[string]
        [void][TestClientGuard]::EnumWindows({
            param($hwnd, $lParam)
            $windowPid = [uint32]0
            [void][TestClientGuard]::GetWindowThreadProcessId($hwnd, [ref]$windowPid)
            if ($ids -contains $windowPid) {
                $title = New-Object System.Text.StringBuilder 256
                [void][TestClientGuard]::GetWindowText($hwnd, $title, 256)
                if ($title.Length -gt 0) {
                    $key = $hwnd.ToInt64()
                    $visible = [TestClientGuard]::IsWindowVisible($hwnd)
                    $iconic = [TestClientGuard]::IsIconic($hwnd)
                    $front = $hwnd -eq $foreground
                    $state = "visible=$visible minimized=$iconic foreground=$front"
                    $idle = [TestClientGuard]::IdleMs()
                    $firstSighting = -not $states.ContainsKey($key)
                    $changed = $states[$key] -ne $state
                    # Anything visible that is not the game window (its title starts with "Minecraft") is a dialog: a
                    # loader's "failed to launch" / incompatible-mods window, a crash or error box. Reported once as
                    # DIALOG, which the test scripts treat as a failed launch (tools/testclient/launchwatch.py).
                    if ($visible -and -not $title.ToString().StartsWith('Minecraft') -and -not $dialogs[$key]) {
                        $dialogs[$key] = $true
                        $script:events.Add(("DIALOG PID {0}: '{1}' {2}" -f $windowPid, $title, $state))
                    }
                    if ($changed) {
                        $states[$key] = $state
                        $script:events.Add(("PID {0}: '{1}' {2} (user input {3} ms ago)" -f $windowPid, $title, $state, $idle))
                    }
                    if ($visible -and (-not $iconic -or $front) -and -not $userWindows[$key]) {
                        # A window seen for the first time cannot have been raised by the user, whatever they typed
                        # just before (a Quilt error dialog was once waved through that way while the user typed).
                        if ($changed -and $idle -lt $UserInputMs -and -not $firstSighting) {
                            # The user brought it up (taskbar click, Alt+Tab): theirs from now on.
                            $userWindows[$key] = $true
                            $script:events.Add(("USER PID {0}: '{1}' {2} right after user input; left alone from now on" -f $windowPid, $title, $state))
                        } elseif (-not $putAways[$key]) {
                            $putAways[$key] = $true
                            # SW_MINIMIZE (6) gives the focus back to the next window; SW_SHOWMINNOACTIVE (7) activates nothing.
                            [void][TestClientGuard]::ShowWindow($hwnd, $(if ($front) { 6 } else { 7 }))
                            $script:events.Add(("VIOLATION PID {0}: '{1}' was {2}; put away" -f $windowPid, $title, $state))
                        } elseif ($changed) {
                            $script:events.Add(("VIOLATION PID {0}: '{1}' {2} again; left alone (already put away once)" -f $windowPid, $title, $state))
                        }
                    }
                }
            }
            return $true
        }, [IntPtr]::Zero)
        foreach ($event in $script:events) {
            if ($event.StartsWith('VIOLATION') -or $event.StartsWith('DIALOG')) { $violations++ }
            Write-Guard $event
        }
        Start-Sleep -Milliseconds 60
    }
}
Write-Guard "done ($violations violation(s))"
