"""Detects a failed or stuck test launch within seconds and deals with it (user rule: never leave an error window on
the user's screen, never wait out a long timeout on a launch that already failed).

Used by tools/devtest/devtest.py and tools/releasetest/releasetest.py while they wait for a client or server:

  check(console_text, guard_log)  -> a description of the failure, or None. A failure is
      - a fatal line in the console/log: a loader refusing the mod set (Fabric "Incompatible mods found!",
        Quilt "Crashed! The full crash report", "requires ... version"), Forge/NeoForge mod-loading errors, a
        Minecraft crash report, a JVM that cannot start;
      - a DIALOG line from tools/testclient/windowguard.ps1: a visible non-game window of this project's java
        processes (or their forked children), e.g. the Quilt/Fabric "Minecraft failed to launch!" window.
  kill_project_games(root)        -> kills this project's game JVMs (and their forked children) by PID, matched by
      command line (machine rule: only processes whose command line names this project; never Gradle daemons,
      never the Gradle wrapper, never another project's process).

Prevention comes first: test launches pass -Dfabric.noGui=true and -Dloader.noGui=true, so Fabric and Quilt Loader
print their error to the log and exit instead of opening a window (root build.gradle `testClientAgentArgs`,
releasetest.py). Forge and NeoForge show mod-loading errors inside the (minimized) game window, so for them the log
lines are what gives the failure away.
"""
import os, re, subprocess, time

FATAL = re.compile(
    r'Incompatible mods found|Mod resolution failed|Mod resolution encountered an incompatible mod set'
    r'|Crashed! The full crash report|A critical error occurred|Minecraft failed to launch'
    r'|requires (at least )?(version|any version)|but only (a different version|the wrong version) is present'
    r'|Loading errors encountered|Mod loading has failed|Failed to complete lifecycle event|Mod loading failed'
    r'|Encountered an unexpected exception|---- Minecraft Crash Report ----|This crash report has been saved to'
    r'|#@!@# Game crashed!|Exception in thread "main"|Could not find or load main class'
    r'|UnsupportedClassVersionError|Error occurred during initialization of VM'
    r'|\[testclient\] ERROR SCREEN'
    # a server whose data packs fail stops loading but its JVM stays up (seen on Forge 65 with a mod pack missing
    # its pack.mcmeta), so the log line is the only sign
    r'|Failed to load datapacks, can.t proceed with server load'
    # no window: the graphics driver refused OpenGL and Vulkan (seen 2026-10-12 while the GPU was briefly gone);
    # the game logs it and stops, but no heartbeat ever comes
    r'|No supported graphics backend was found')


def check(console_text, guard_log=None):
    m = FATAL.search(console_text or '')
    if m:
        start = console_text.rfind('\n', 0, m.start()) + 1
        lines = console_text[start:].splitlines()[:12]
        return 'fatal launch error in the log: ' + ' | '.join(l.strip() for l in lines if l.strip())[:1500]
    if guard_log and os.path.exists(guard_log):
        try:
            with open(guard_log, encoding='utf-8', errors='replace') as f:
                for line in f:
                    if ' DIALOG PID ' in line:
                        return 'error window on screen: ' + line.strip()
        except PermissionError:
            pass  # windowguard.ps1 is appending (Add-Content locks the file for that instant): read it next time
    return None


def project_game_pids(root):
    """PIDs of this project's game/server JVMs and their java children - never Gradle's own processes."""
    ps = (
        "$p = @(Get-CimInstance Win32_Process -Filter \"Name='java.exe' OR Name='javaw.exe'\"); "
        "$own = @($p | Where-Object { $_.CommandLine -and $_.CommandLine.Contains('" + root.replace("'", "''") + "') "
        "-and $_.CommandLine -notmatch 'GradleDaemon|GradleWrapperMain|gradle-launcher|GradleWorkerMain' } "
        "| ForEach-Object { $_.ProcessId }); "
        "$kids = @($p | Where-Object { $own -contains $_.ParentProcessId } | ForEach-Object { $_.ProcessId }); "
        "($own + $kids) | Sort-Object -Unique"
    )
    out = subprocess.run(['powershell', '-NoProfile', '-Command', ps], capture_output=True, text=True).stdout
    return [int(x) for x in out.split() if x.strip().isdigit()]


def stop_pid(pid):
    """Stops one process by PID with PowerShell's Stop-Process (machine rule: no taskkill /F, which triggers an
    approval prompt). Only ever called with a PID matched to this project's command line."""
    subprocess.run(['powershell', '-NoProfile', '-Command', f'Stop-Process -Id {int(pid)}'],
                   capture_output=True)


def stop_tree(pid, log=print):
    """Stops one launch: its JVM and that JVM's java children (Fabric forks a JVM for its error window), by PID. Used
    on a launch the test script started itself, so another launch of this project (the server while a client fails)
    keeps running."""
    ps = ("@(Get-CimInstance Win32_Process -Filter \"Name='java.exe' OR Name='javaw.exe'\" "
          f"| Where-Object {{ $_.ParentProcessId -eq {int(pid)} }} | ForEach-Object {{ $_.ProcessId }})")
    out = subprocess.run(['powershell', '-NoProfile', '-Command', ps], capture_output=True, text=True).stdout
    for child in [int(x) for x in out.split() if x.strip().isdigit()] + [int(pid)]:
        stop_pid(child)
        log(f'[launchwatch] stopped PID {child}')


def kill_project_games(root, log=print):
    pids = project_game_pids(root)
    for pid in pids:
        stop_pid(pid)
        log(f'[launchwatch] stopped PID {pid} (this project\'s game process)')
    return pids


# --- the live watcher: is the client still making progress? --------------------------------------------------
# The agent (RemoteControl) prints a heartbeat every 5 s:
#   [testclient] heartbeat t=<epoch s> ticks=<client ticks> fps=<fps> inWorld=<bool> screen=<class|none> title='<t>'
# stall_check reads them while a test script waits. Costs nothing: one log line every 5 s, a regex every 2 s.
HEARTBEAT = re.compile(r"\[testclient\] heartbeat t=(\d+) ticks=(-?\d+) fps=(\d+) inWorld=(true|false) "
                       r"screen=(\S+) title='([^'\n]*)'")
# Screens that legitimately stay up for a while (world generation, joining, resource loading).
SLOW_SCREENS = ('LevelLoadingScreen', 'ReceivingLevelScreen', 'ProgressScreen', 'GenericMessageScreen',
                'ConnectScreen', 'LoadingOverlay')
LIMITS = {'frozen': 45, 'hung': 90, 'stuck': 120, 'stuck_slow': 420}


def stall_check(text, now=None, limits=None):
    """'FROZEN ...' (no heartbeat: JVM or agent blocked), 'HUNG ...' (client ticks stopped: game thread blocked),
    'STUCK ...' (the same screen up too long), or None while the client makes progress."""
    lim = dict(LIMITS, **(limits or {}))
    beats = HEARTBEAT.findall(text or '')
    if not beats:
        return None
    now = now or time.time()
    last = beats[-1]
    age = now - int(last[0])
    if age > lim['frozen']:
        return f'FROZEN: no heartbeat for {int(age)} s (last: screen={last[4]} title="{last[5]}" inWorld={last[3]})'
    # ticks stopped: walk back while the tick count stays the same
    if last[1] != '-1':
        since = int(last[0])
        for b in reversed(beats):
            if b[1] != last[1]:
                break
            since = int(b[0])
        if int(last[0]) - since >= lim['hung']:
            return (f'HUNG: client ticks stuck at {last[1]} for {int(last[0]) - since} s '
                    f'(screen={last[4]} title="{last[5]}" fps={last[2]})')
    # the same screen up too long, in a world or not (a popup in a world blocks a test just as well)
    if last[4] != 'none':
        since = int(last[0])
        for b in reversed(beats):
            if b[4] != last[4]:
                break
            since = int(b[0])
        limit = lim['stuck_slow'] if last[4].rsplit('.', 1)[-1] in SLOW_SCREENS else lim['stuck']
        if int(last[0]) - since >= limit:
            return f'STUCK: on {last[4]} "{last[5]}" for {int(last[0]) - since} s'
    return None


def thread_dump(pid, path):
    """Saves the JVM's thread stacks (jcmd Thread.print) for a STUCK/HUNG/FROZEN report. Best effort."""
    try:
        import testclient
        jcmd = os.path.join(testclient.find_jdk_bin(), 'jcmd.exe')
        out = subprocess.run([jcmd, str(pid), 'Thread.print'], capture_output=True, text=True, timeout=60).stdout
        with open(path, 'w', encoding='utf-8') as f:
            f.write(out)
        return path
    except Exception as e:
        return f'(no thread dump: {e})'
