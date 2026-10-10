# Reports the top-level windows of this project's running game clients: minimized or not, visible, and whether one
# is the foreground window. A test client should be minimized and never in front (user rule).
#
#     powershell -NoProfile -ExecutionPolicy Bypass -File tools/testclient/windowstate.ps1
#
# Only java processes whose command line contains this project's folder are looked at (machine rule on processes).
$project = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
Add-Type @"
using System;
using System.Runtime.InteropServices;
using System.Text;
public static class TestClientWindows {
    public delegate bool EnumProc(IntPtr hwnd, IntPtr lParam);
    [DllImport("user32.dll")] public static extern bool EnumWindows(EnumProc callback, IntPtr lParam);
    [DllImport("user32.dll")] public static extern uint GetWindowThreadProcessId(IntPtr hwnd, out uint pid);
    [DllImport("user32.dll")] public static extern bool IsWindowVisible(IntPtr hwnd);
    [DllImport("user32.dll")] public static extern bool IsIconic(IntPtr hwnd);
    [DllImport("user32.dll")] public static extern IntPtr GetForegroundWindow();
    [DllImport("user32.dll", CharSet = CharSet.Unicode)] public static extern int GetWindowText(IntPtr hwnd, StringBuilder text, int max);
}
"@
$ids = @(Get-CimInstance Win32_Process -Filter "Name='java.exe' OR Name='javaw.exe'" |
        Where-Object { $_.CommandLine -and $_.CommandLine.Contains($project) } | ForEach-Object { [uint32]$_.ProcessId })
$foreground = [TestClientWindows]::GetForegroundWindow()
# Output written inside the callback would be lost (its value is the delegate's return), so collect it.
$script:report = New-Object System.Collections.Generic.List[string]
[void][TestClientWindows]::EnumWindows({
    param($hwnd, $lParam)
    $windowPid = [uint32]0
    [void][TestClientWindows]::GetWindowThreadProcessId($hwnd, [ref]$windowPid)
    if ($ids -contains $windowPid -and [TestClientWindows]::IsWindowVisible($hwnd)) {
        $title = New-Object System.Text.StringBuilder 256
        [void][TestClientWindows]::GetWindowText($hwnd, $title, 256)
        if ($title.Length -gt 0) {
            $script:report.Add(("PID {0}: '{1}' minimized={2} foreground={3}" -f $windowPid, $title,
                    [TestClientWindows]::IsIconic($hwnd), ($hwnd -eq $foreground)))
        }
    }
    return $true
}, [IntPtr]::Zero)
if ($script:report.Count -eq 0) { "no visible window of this project's java processes" } else { $script:report }
