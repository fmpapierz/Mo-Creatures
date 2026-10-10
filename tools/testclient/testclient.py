"""Unattended test clients for this project (user rule: test windows open minimized and never steal focus).

    py tools/testclient/testclient.py agent                 build tools/testclient/build/testclient-agent.jar
    py tools/testclient/testclient.py prepare <run dir> [forge|neoforge|fabric|quilt]
    py tools/testclient/testclient.py patch-world <world dir>
    py tools/testclient/testclient.py send <run dir> "<line>" ["<line>" ...]   (screenshot <name> | cmd <command>)

`prepare` makes a client run directory safe to launch unattended:
  - options.txt: no accessibility/narrator onboarding screen, no pause when the (minimized) window is unfocused,
    narrator off, sound muted, multiplayer warning and tutorial skipped;
  - config/fml.toml (Forge/NeoForge): the early loading window is disabled, so Minecraft creates its own window,
    which the agent opens minimized with the right framebuffer size;
  - every world in saves/: level.dat gets Data.confirmedExperimentalSettings = 1b and allowCommands = 1b (cheats,
    so the agent's remote control can /tp and /summon as the player).
Client runs launched with `-PtestClient` get the agent (see the loader build scripts), which creates the game window
hidden and unfocused and then shows it minimized without activating it, and which answers the narrator onboarding and
"Experimental Settings" prompts by itself.
"""
import gzip, io, os, struct, subprocess, sys

HERE = os.path.dirname(os.path.abspath(__file__))
AGENT = os.path.join(HERE, 'build', 'testclient-agent.jar')

OPTIONS = {
    'onboardAccessibility': 'false',
    'pauseOnLostFocus': 'false',
    'narrator': '0',
    'skipMultiplayerWarning': 'true',
    'joinedFirstServer': 'true',
    'tutorialStep': 'none',
    'soundCategory_master': '0.0',
}


def jdk_feature_version(home):
    """The JDK's major version from its `release` file, or 0 if it cannot be read."""
    try:
        with open(os.path.join(home, 'release'), encoding='utf-8') as f:
            for line in f:
                if line.startswith('JAVA_VERSION='):
                    return int(line.split('=', 1)[1].strip().strip('"').split('.')[0])
    except (OSError, ValueError):
        pass
    return 0


def find_jdk_bin():
    # The agent uses java.lang.classfile, final since Java 24; Minecraft 26.2 runs on Java 25 anyway. JAVA_HOME is
    # often an older JDK on this machine, so each candidate's version is checked.
    for home in (os.environ.get('TESTCLIENT_JDK'), os.environ.get('JAVA_HOME'),
                 r'C:\Users\warwa\ModDev\_toolchain\jdk-25.0.3+9', r'C:\Program Files\Zulu\zulu-25'):
        if home and os.path.exists(os.path.join(home, 'bin', 'javac.exe')) and jdk_feature_version(home) >= 25:
            return os.path.join(home, 'bin')
    raise SystemExit('No JDK 25 found (set TESTCLIENT_JDK)')


def build_agent():
    src = os.path.join(HERE, 'src', 'testclient')
    sources = [os.path.join(src, f) for f in os.listdir(src) if f.endswith('.java')]
    if os.path.exists(AGENT) and os.path.getmtime(AGENT) >= max(os.path.getmtime(s) for s in sources):
        return AGENT
    jdk = find_jdk_bin()
    classes = os.path.join(HERE, 'build', 'classes')
    os.makedirs(classes, exist_ok=True)
    subprocess.check_call([os.path.join(jdk, 'javac'), '--release', '25', '-d', classes] + sources)
    manifest = os.path.join(HERE, 'build', 'MANIFEST.MF')
    with open(manifest, 'w', newline='\n') as f:
        f.write('Premain-Class: testclient.MinimizedWindowAgent\n')
    subprocess.check_call([os.path.join(jdk, 'jar'), 'cfm', AGENT, manifest, '-C', classes, '.'])
    return AGENT


def seed_options(run_dir):
    os.makedirs(run_dir, exist_ok=True)
    path = os.path.join(run_dir, 'options.txt')
    lines = open(path, encoding='utf-8').read().splitlines() if os.path.exists(path) else []
    seen = set()
    out = []
    for line in lines:
        key = line.split(':', 1)[0]
        if key in OPTIONS:
            out.append(f'{key}:{OPTIONS[key]}')
            seen.add(key)
        else:
            out.append(line)
    out += [f'{k}:{v}' for k, v in OPTIONS.items() if k not in seen]
    with open(path, 'w', encoding='utf-8', newline='\n') as f:
        f.write('\n'.join(out) + '\n')


def disable_early_window(run_dir):
    cfg = os.path.join(run_dir, 'config')
    os.makedirs(cfg, exist_ok=True)
    path = os.path.join(cfg, 'fml.toml')
    lines = open(path, encoding='utf-8').read().splitlines() if os.path.exists(path) else []
    lines = [l for l in lines if not l.strip().startswith('earlyWindowControl')]
    lines.insert(0, 'earlyWindowControl = false')
    with open(path, 'w', encoding='utf-8', newline='\n') as f:
        f.write('\n'.join(lines) + '\n')


# --- minimal NBT (level.dat) reader/writer -------------------------------------------------------------------
def _read_payload(t, s):
    if t == 1: return struct.unpack('>b', s.read(1))[0]
    if t == 2: return struct.unpack('>h', s.read(2))[0]
    if t == 3: return struct.unpack('>i', s.read(4))[0]
    if t == 4: return struct.unpack('>q', s.read(8))[0]
    if t == 5: return struct.unpack('>f', s.read(4))[0]
    if t == 6: return struct.unpack('>d', s.read(8))[0]
    if t == 7:
        n = struct.unpack('>i', s.read(4))[0]; return s.read(n)
    if t == 8:
        n = struct.unpack('>H', s.read(2))[0]; return s.read(n).decode('utf-8', 'surrogatepass')
    if t == 9:
        et = s.read(1)[0]; n = struct.unpack('>i', s.read(4))[0]
        return (et, [_read_payload(et, s) for _ in range(n)])
    if t == 10:
        d = []
        while True:
            ct = s.read(1)[0]
            if ct == 0: return d
            nl = struct.unpack('>H', s.read(2))[0]; name = s.read(nl).decode('utf-8', 'surrogatepass')
            d.append([ct, name, _read_payload(ct, s)])
    if t == 11:
        n = struct.unpack('>i', s.read(4))[0]; return list(struct.unpack(f'>{n}i', s.read(4 * n)))
    if t == 12:
        n = struct.unpack('>i', s.read(4))[0]; return list(struct.unpack(f'>{n}q', s.read(8 * n)))
    raise ValueError(f'bad tag {t}')


def _write_payload(t, v, s):
    if t == 1: s.write(struct.pack('>b', v))
    elif t == 2: s.write(struct.pack('>h', v))
    elif t == 3: s.write(struct.pack('>i', v))
    elif t == 4: s.write(struct.pack('>q', v))
    elif t == 5: s.write(struct.pack('>f', v))
    elif t == 6: s.write(struct.pack('>d', v))
    elif t == 7: s.write(struct.pack('>i', len(v))); s.write(v)
    elif t == 8:
        b = v.encode('utf-8', 'surrogatepass'); s.write(struct.pack('>H', len(b))); s.write(b)
    elif t == 9:
        et, items = v; s.write(bytes([et])); s.write(struct.pack('>i', len(items)))
        for it in items: _write_payload(et, it, s)
    elif t == 10:
        for ct, name, cv in v:
            s.write(bytes([ct])); b = name.encode('utf-8', 'surrogatepass')
            s.write(struct.pack('>H', len(b))); s.write(b); _write_payload(ct, cv, s)
        s.write(b'\x00')
    elif t == 11: s.write(struct.pack('>i', len(v))); s.write(struct.pack(f'>{len(v)}i', *v))
    elif t == 12: s.write(struct.pack('>i', len(v))); s.write(struct.pack(f'>{len(v)}q', *v))
    else: raise ValueError(f'bad tag {t}')


def patch_world(world_dir):
    path = os.path.join(world_dir, 'level.dat')
    raw = gzip.decompress(open(path, 'rb').read())
    s = io.BytesIO(raw)
    t = s.read(1)[0]
    name = s.read(struct.unpack('>H', s.read(2))[0])
    root = _read_payload(t, s)
    data = next(v for ct, n, v in root if n == 'Data')
    # confirmedExperimentalSettings: Forge's skip flag for the "Experimental Settings" screen;
    # allowCommands: cheats on, so the agent's remote control can /tp and /summon as the player.
    for key in ('confirmedExperimentalSettings', 'allowCommands'):
        for entry in data:
            if entry[1] == key:
                entry[0], entry[2] = 1, 1
                break
        else:
            data.append([1, key, 1])
    out = io.BytesIO()
    out.write(bytes([t])); out.write(struct.pack('>H', len(name))); out.write(name)
    _write_payload(t, root, out)
    open(path, 'wb').write(gzip.compress(out.getvalue()))


def send(run_dir, lines):
    """Queue commands for the agent's RemoteControl (screenshot <name> / cmd <command>)."""
    inbox = os.path.join(run_dir, 'testclient', 'inbox')
    os.makedirs(inbox, exist_ok=True)
    import time
    name = os.path.join(inbox, f'{time.time_ns()}.tmp')
    nl = chr(10)
    with open(name, 'w', encoding='utf-8', newline=nl) as f:
        f.write(nl.join(lines) + nl)
    # rename last, so the agent never reads a half-written file (it only picks up *.txt)
    os.replace(name, name[:-4] + '.txt')


def prepare(run_dir, loader=None):
    build_agent()
    seed_options(run_dir)
    if loader in ('forge', 'neoforge'):
        disable_early_window(run_dir)
    saves = os.path.join(run_dir, 'saves')
    if os.path.isdir(saves):
        for w in os.listdir(saves):
            if os.path.exists(os.path.join(saves, w, 'level.dat')):
                patch_world(os.path.join(saves, w))
    print(f'[testclient] prepared {run_dir} (agent: {AGENT})')


if __name__ == '__main__':
    if len(sys.argv) < 2:
        raise SystemExit(__doc__)
    cmd = sys.argv[1]
    if cmd == 'agent':
        print(build_agent())
    elif cmd == 'prepare':
        prepare(sys.argv[2], sys.argv[3] if len(sys.argv) > 3 else None)
    elif cmd == 'patch-world':
        patch_world(sys.argv[2])
    elif cmd == 'send':
        send(sys.argv[2], sys.argv[3:])
    else:
        raise SystemExit(__doc__)
