"""Release check: the RELEASE jars from build/libs on real (production) installs of each loader, the way players get
them - no Gradle, no dev classpath, no merged client/server code.

    py tools/releasetest/releasetest.py check <loader> <server port> <rcon port> [--set <mod set>] [--tour-only]
        The whole check for one loader and one mod set, unattended:
          1. the loader's dedicated server (official installer or launcher) with the release jar, Architectury API, on
             Fabric Fabric API, and the set's server-capable mods; EULA accepted, whitelist off (both keys), a fresh
             world. Plain and server-mod sets run the content + mechanics check over RCON (tools/servertest/
             servertest.py: every entity and loot table, kitty bed, kitty night, MoC x vanilla breeding, led animals
             over a step, no animal cap, 12.0.5 biome placement and spawn census); the other sets only build its
             test platform;
          2. a real client (the loader's launcher profile, offline, the test-client agent: minimized, never focused,
             prompts answered) JOINS that server and runs the client tour (tools/testclient/moctour.py: horse taming
             hint, vanilla horse mount, no rearing while ridden, scorpions, the food-bowl click, kitty bed, the Wyvern
             Lair sky, ambient insects);
          3. the server stopped, its world copied into the client's saves, the same tour in singleplayer;
          4. the server and both client logs scanned; with a shader pack, Iris must have built its pipeline and logged
             no shader failure (the pack was really ON); the screenshots show it, the Wyvern Lair's included.
        --tour-only skips step 1's content and mechanics check (only the tour's platform is built): for re-running
        the client legs after a change that touched nothing on the server.
        Results in build/releasetest/<loader>[-<set>]/summary.txt, with the logs, screenshots (client/screenshots),
        the window guard's log and modset.txt (what was picked from Modrinth, with versions and sha1s).
    py tools/releasetest/releasetest.py sets <loader>
        Prints the mod sets as they would be picked from Modrinth right now.
    py tools/releasetest/releasetest.py probe <connect|refused|window> <loader> [port]
        Verifies the failure detection (machine rules "Server testing" and "Failed launches"): connect = a client sent
        to a closed port must be reported within seconds; refused = a client whose mods/ lacks Architectury API must
        be refused by the loader, reported from its log, with no window on screen; window = a minimized, unfocusable
        window titled like a loader's error box, opened by a java process of this project, must be flagged by the
        window guard and that one PID stopped.
    py tools/releasetest/releasetest.py server <loader> <server port> <rcon port>
    py tools/releasetest/releasetest.py client <loader> [--join <port>] [--world <name>]
        One server or client by hand (attached until it exits).

Mod sets (shaders / performance rule; the lists are picked FRESH from Modrinth for 26.2 and the loader on every run,
newest version, sha1-checked, required dependencies included):
  plain           the release jar and its dependencies only (default)
  sodium          + Sodium
  shaders:<slug>  + Sodium + Iris + that shader pack, shaders ON (proved by "Creating pipeline for dimension" and none
                  of "Shaders are disabled because enableShaders" / "Failed to create iris render pipeline" /
                  "Pipeline data already bound"); `sets` prints the top packs
  perf            + the 15 most-downloaded "optimization" mods for 26.2 and the loader (server-capable ones on the
                  server too)
  popular         + the most popular mods in the mod's area (mobs) and the usual neighbours (JEI, Jade, ModMenu,
                  Xaero's Minimap, Distant Horizons)

loader is fabric or neoforge (the loaders Mo'Creatures ships for; the quilt and forge code paths are kept from the
script this was adapted from, Alex's Caves 26.2's, with the mod sets and probes from WaterBending 26.2's). Installs live
in build/releasetest/<loader>/server and <results>/client; the vanilla client, the libraries and the loader profiles in
build/releasetest/launcher (a .minecraft-like folder the Forge and NeoForge installers can install into). Mojang's
files come from Loom's cache when it has them, everything else from the official sources (downloads are pre-approved by
the user). Every java command carries -Dmoctest.releasetest=<this project>, so its process can be matched to this
project (machine rule), and every launch passes the loaders' no-GUI switches. Processes are stopped by PID with
Stop-Process (machine rules: own project only, no taskkill /F).
"""
import glob, hashlib, json, os, re, shutil, subprocess, sys, time, urllib.parse, urllib.request, uuid

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(os.path.dirname(HERE))
sys.path.insert(0, os.path.join(ROOT, 'tools', 'testclient'))
sys.path.insert(0, os.path.join(ROOT, 'tools', 'servertest'))
# Loader messages carry characters like the infinity sign; the Windows console code page cannot print them.
sys.stdout.reconfigure(encoding='utf-8', errors='replace')
import testclient  # noqa: E402
import servertest  # noqa: E402
import launchwatch  # noqa: E402


def read_properties(path):
    props = {}
    for line in open(path, encoding='utf-8'):
        line = line.strip()
        if line and not line.startswith('#') and '=' in line:
            key, value = line.split('=', 1)
            props[key.strip()] = value.strip()
    return props


PROPS = read_properties(os.path.join(ROOT, 'gradle.properties'))
MC = PROPS['minecraft_version']
WORK = os.path.join(ROOT, 'build', 'releasetest')
LAUNCHER = os.path.join(WORK, 'launcher')
DOWNLOADS = os.path.join(WORK, 'downloads')
HOME = os.path.expanduser('~')
M2 = os.path.join(HOME, '.gradle', 'caches', 'modules-2', 'files-2.1')
LOOM = os.path.join(HOME, '.gradle', 'caches', 'fabric-loom')
PLAYER = 'Tester'
MARKER = f'-Dmoctest.releasetest={ROOT}'
FORGE = f"{MC}-{PROPS.get('forge_version', '')}"
NEOFORGE = PROPS['neoforge_version']
QUILT_RUN = PROPS.get('quilt_run_loader_version', PROPS.get('quilt_loader_version', ''))
FABRIC_API = PROPS['fabric_api_version']
ARCHITECTURY = PROPS['architectury_api_version']
# Fabric and Quilt Loader: log the error and exit instead of opening a "failed to launch" window (launchwatch.py). The
# agent sets them too; they are on the command line as well so a launch without the agent is covered.
NO_GUI = ['-Dfabric.noGui=true', '-Dloader.noGui=true']
# Server-testing rule, item 3: test SERVERS run the old select()-based selector (a wepoll server now and then leaves a
# login handshake unsent); clients never do (a failed connect then hangs instead of showing "Connection refused").
SERVER_SELECTOR = '-Djava.nio.channels.spi.SelectorProvider=sun.nio.ch.WindowsSelectorProvider'
UA = {'User-Agent': 'mocreatures-releasetest/1.0 (local mod release check)'}
# The mod's own area on Modrinth, for the popular-mods set, and the usual neighbours of any mod that draws in the world.
AREA = 'mobs'
NEIGHBOURS = {'fabric': ['jei', 'jade', 'modmenu', 'xaeros-minimap', 'distanthorizons'],
              'neoforge': ['jei', 'jade', 'xaeros-minimap', 'distanthorizons']}
# The mod's own dependencies come from build properties, never from a Modrinth pick.
OWN_DEPENDENCIES = ('fabric-api', 'qsl', 'architectury-api')
SHADER_FAILURE = re.compile(r'Shaders are disabled because enableShaders|Failed to create iris render pipeline'
                            r'|Pipeline data already bound')


def java():
    return os.path.join(testclient.find_jdk_bin(), 'java.exe')


_fabric_loader = []


def fabric_loader():
    """The Fabric Loader players get: the newest stable release (gradle property release_check_fabric_loader_version
    pins one), not the build's minimum. Mods in the sets can need a newer loader: Distant Horizons 3.3.3 refused to load
    on 0.19.3 ("depends fabricloader >=0.19.5", 2026-10-10)."""
    if not _fabric_loader:
        pinned = PROPS.get('release_check_fabric_loader_version')
        if pinned:
            _fabric_loader.append(pinned)
        else:
            req = urllib.request.Request('https://meta.fabricmc.net/v2/versions/loader', headers=UA)
            with urllib.request.urlopen(req, timeout=60) as r:
                _fabric_loader.append(next(v['version'] for v in json.load(r) if v.get('stable')))
        print(f'[releasetest] Fabric Loader {_fabric_loader[0]}', flush=True)
    return _fabric_loader[0]


def sha1(path):
    h = hashlib.sha1()
    with open(path, 'rb') as f:
        for chunk in iter(lambda: f.read(1 << 20), b''):
            h.update(chunk)
    return h.hexdigest()


def download(url, path, expected_sha1=None):
    if os.path.exists(path) and (expected_sha1 is None or sha1(path) == expected_sha1):
        return path
    os.makedirs(os.path.dirname(path), exist_ok=True)
    tmp = path + '.part'
    with urllib.request.urlopen(urllib.request.Request(url, headers=UA), timeout=300) as r, open(tmp, 'wb') as f:
        shutil.copyfileobj(r, f)
    if expected_sha1 and sha1(tmp) != expected_sha1:
        os.remove(tmp)
        raise RuntimeError(f'checksum mismatch for {url}')
    os.replace(tmp, path)
    return path


def from_m2(group, artifact, version, classifier=None):
    name = f'{artifact}-{version}' + (f'-{classifier}' if classifier else '') + '.jar'
    hits = glob.glob(os.path.join(M2, group, artifact, version, '*', name))
    return hits[0] if hits else None


def offline_uuid(name):
    # Java's UUID.nameUUIDFromBytes("OfflinePlayer:<name>"): an MD5 name-based (version 3) UUID
    digest = bytearray(hashlib.md5(('OfflinePlayer:' + name).encode('utf-8')).digest())
    digest[6] = (digest[6] & 0x0f) | 0x30
    digest[8] = (digest[8] & 0x3f) | 0x80
    return str(uuid.UUID(bytes=bytes(digest)))


def release_jar(loader):
    path = os.path.join(ROOT, 'build', 'libs', f"{PROPS['mod_id']}-{loader}-{PROPS['mod_version']}+{MC}.jar")
    if not os.path.exists(path):
        raise SystemExit(f'No release jar {path}; run gradlew assemble first')
    return path


_fabric_api = []


def fabric_api_jar():
    """The Fabric API players get: the newest Modrinth release for MC (gradle property release_check_fabric_api_version
    pins one, e.g. the build's own). Mods in the sets can need a newer one than the build's: BCLib 26.201.2 refused
    to load with 0.153.0 ("depends fabric-api >=0.155.0", 2026-10-10)."""
    if not _fabric_api:
        pinned = PROPS.get('release_check_fabric_api_version')
        if pinned:
            jar = from_m2('net.fabricmc.fabric-api', 'fabric-api', pinned) or download(
                f"https://maven.fabricmc.net/net/fabricmc/fabric-api/fabric-api/{pinned}/fabric-api-{pinned}.jar",
                os.path.join(DOWNLOADS, f"fabric-api-{pinned}.jar"))
            _fabric_api.append((pinned, jar))
        else:
            found, _ = newest_file('fabric-api', 'fabric')
            _fabric_api.append((found[0].version, found[0].path()))
        print(f'[releasetest] Fabric API {_fabric_api[0][0]}', flush=True)
    return _fabric_api[0][1]


def architectury_jar(loader):
    """The released Architectury API for this loader (a required dependency), from Gradle's cache or its Maven."""
    artifact = f'architectury-{loader}'
    jar = from_m2('dev.architectury', artifact, ARCHITECTURY)
    if not jar:
        jar = download(f'https://maven.architectury.dev/dev/architectury/{artifact}/{ARCHITECTURY}/'
                       f'{artifact}-{ARCHITECTURY}.jar', os.path.join(DOWNLOADS, f'{artifact}-{ARCHITECTURY}.jar'))
    return jar


# --- mod sets from Modrinth (shaders / performance rule) -----------------------------------------------------------

def modrinth(path, **params):
    query = urllib.parse.urlencode({k: json.dumps(v) if isinstance(v, (list, dict)) else v for k, v in params.items()})
    url = f'https://api.modrinth.com/v2/{path}' + (f'?{query}' if query else '')
    with urllib.request.urlopen(urllib.request.Request(url, headers=UA), timeout=60) as r:
        return json.load(r)


def top_slugs(loader, facets, limit):
    return [h['slug'] for h in modrinth('search', facets=facets, index='downloads', limit=limit)['hits']]


class ModFile:
    def __init__(self, slug, project_id, version, filename, url, sha, client_side, server_side):
        self.slug, self.project_id, self.version, self.filename = slug, project_id, version, filename
        self.url, self.sha, self.client_side, self.server_side = url, sha, client_side, server_side

    def path(self):
        return download(self.url, os.path.join(DOWNLOADS, 'modrinth', self.slug, self.filename), self.sha)


def newest_file(slug_or_id, loader, kind='mod'):
    """The newest Modrinth version of a project for MC and the loader (shader packs: the Iris loader)."""
    project = modrinth(f'project/{slug_or_id}')
    loaders = ['iris'] if kind == 'shader' else [loader]
    versions = modrinth(f'project/{project["id"]}/version', game_versions=[MC], loaders=loaders)
    if not versions:
        return None, project
    v = versions[0]
    f = next((x for x in v['files'] if x.get('primary')), v['files'][0])
    return (ModFile(project['slug'], project['id'], v['version_number'], f['filename'], f['url'], f['hashes']['sha1'],
                    project.get('client_side'), project.get('server_side')), v), project


def resolve_set(loader, slugs):
    """ModFiles for the slugs plus their required dependencies (newest for MC + loader); missing ones are reported."""
    chosen, missing, seen = [], [], set()
    queue = list(slugs)
    while queue:
        slug = queue.pop(0)
        try:
            found, project = newest_file(slug, loader)
        except Exception as e:  # a slug that does not exist
            missing.append(f'{slug} ({e})')
            continue
        if project['id'] in seen or project['slug'] in OWN_DEPENDENCIES or project['slug'] == PROPS['mod_id']:
            continue
        seen.add(project['id'])
        if found is None:
            missing.append(f'{project["slug"]} (no {MC} {loader} version)')
            continue
        mod, version = found
        chosen.append(mod)
        for dep in version.get('dependencies', []):
            if dep.get('dependency_type') == 'required' and dep.get('project_id') and dep['project_id'] not in seen:
                queue.append(dep['project_id'])
    return chosen, missing


def pick_set(loader, name):
    """(client ModFiles, server ModFiles, shader pack ModFile or None, notes) for a mod set, picked fresh."""
    if name == 'plain':
        return [], [], None, []
    if name == 'sodium':
        slugs = ['sodium']
    elif name.startswith('shaders:'):
        slugs = ['sodium', 'iris']
    elif name == 'perf':
        slugs = top_slugs(loader, [['categories:optimization'], [f'versions:{MC}'], [f'categories:{loader}'],
                                   ['project_type:mod']], 15)
    elif name == 'popular':
        slugs = top_slugs(loader, [[f'categories:{AREA}'], [f'versions:{MC}'], [f'categories:{loader}'],
                                   ['project_type:mod']], 6) + NEIGHBOURS[loader]
    else:
        raise SystemExit(f'unknown mod set {name}')
    mods, missing = resolve_set(loader, slugs)
    pack = None
    if name.startswith('shaders:'):
        found, _ = newest_file(name.split(':', 1)[1], loader, 'shader')
        if found is None:
            raise SystemExit(f'no {MC} version of shader pack {name}')
        pack = found[0]
    # On the dedicated server: what is not client-only (Modrinth's server_side), never Sodium/Iris.
    server_mods = [m for m in mods if m.server_side != 'unsupported' and m.slug not in ('sodium', 'iris')]
    return mods, server_mods, pack, [f'not available: {m}' for m in missing]


def write_modset(out_dir, loader, name, mods, server_mods, pack, notes):
    lines = [f'mod set {name} for {loader} {MC} (picked from Modrinth {time.strftime("%Y-%m-%d %H:%M")})']
    for m in mods:
        lines.append(f'  client{" + server" if m in server_mods else ""}: {m.slug} {m.version} {m.filename} '
                     f'sha1 {m.sha}')
    if pack:
        lines.append(f'  shader pack: {pack.slug} {pack.version} {pack.filename} sha1 {pack.sha}')
    lines += [f'  {n}' for n in notes]
    open(os.path.join(out_dir, 'modset.txt'), 'w', encoding='utf-8').write('\n'.join(lines) + '\n')
    print('\n'.join(lines), flush=True)


def set_mods(loader, game_dir, extra=(), without_architectury=False):
    """A clean mods/ holding only the release jar under test, what it needs on that loader and the set's mods."""
    mods = os.path.join(game_dir, 'mods')
    os.makedirs(mods, exist_ok=True)
    for old in glob.glob(os.path.join(mods, '*.jar')):
        os.remove(old)
    if loader == 'quilt':
        # Quilt Loader 0.31 fails to read a transform cache written before the mods changed; start from a clean one.
        shutil.rmtree(os.path.join(game_dir, '.cache', 'quilt_loader'), ignore_errors=True)
    shutil.copy(release_jar(loader), mods)
    if not without_architectury:
        shutil.copy(architectury_jar('fabric' if loader in ('fabric', 'quilt') else loader), mods)
    if loader in ('fabric', 'quilt'):
        shutil.copy(fabric_api_jar(), mods)
    for m in extra:
        shutil.copy(m.path(), mods)


def set_shaderpack(game_dir, pack=None):
    """Shaders ON with the set's pack, otherwise no iris.properties (Iris's default: off)."""
    cfg = os.path.join(game_dir, 'config')
    os.makedirs(cfg, exist_ok=True)
    props = os.path.join(cfg, 'iris.properties')
    if os.path.exists(props):
        os.remove(props)
    shutil.rmtree(os.path.join(game_dir, 'shaderpacks'), ignore_errors=True)
    if pack is None:
        return None
    os.makedirs(os.path.join(game_dir, 'shaderpacks'), exist_ok=True)
    shutil.copy(pack.path(), os.path.join(game_dir, 'shaderpacks', pack.filename))
    with open(props, 'w', encoding='utf-8', newline='\n') as f:
        f.write(f'enableShaders=true\nshaderPack={pack.filename}\ndisableUpdateMessage=true\n')
    return pack.filename


def set_dynamic_fps(game_dir, mods=()):
    """Dynamic FPS (in the perf set) stops rendering a minimized window and drops an unfocused one to 1 fps; a
    singleplayer world load, which advances one frame at a time, then never finishes on the minimized test client
    (HUNG on LevelLoadingScreen, 2026-10-10; a test-setup effect, a player's window is not minimized). Those two
    states get no limit; the mod stays loaded and enabled (its config keeps only values that differ from its defaults;
    WaterBending 26.2's recipe)."""
    cfg = os.path.join(game_dir, 'config', 'dynamic_fps.json')
    if any(m.slug == 'dynamic-fps' for m in mods):
        os.makedirs(os.path.dirname(cfg), exist_ok=True)
        with open(cfg, 'w', encoding='utf-8') as f:
            json.dump({'states': {'unfocused': {'frame_rate_target': -1}, 'invisible': {'frame_rate_target': -1}}}, f)
    elif os.path.exists(cfg):
        os.remove(cfg)


def set_wover(game_dir, mods=()):
    """WorldWeaver (WoVer, the library under BetterEnd/BetterNether, in the popular set) opens a "Welcome to ..."
    screen on a fresh game folder that waits for a click: the unattended client sat on it until the watcher reported
    it STUCK (2026-10-10). Its own config says the screen was already shown, and its online version check is off."""
    cfg = os.path.join(game_dir, 'config', 'wover', 'client.json')
    if any(m.slug == 'worldweaver' for m in mods):
        data = {}
        if os.path.exists(cfg):
            try:
                data = json.load(open(cfg, encoding='utf-8'))
            except ValueError:
                data = {}
        data.setdefault('internal', {})['did_present_welcome_screen'] = True
        data.setdefault('general', {})['check_for_new_versions'] = False
        os.makedirs(os.path.dirname(cfg), exist_ok=True)
        with open(cfg, 'w', encoding='utf-8') as f:
            json.dump(data, f, indent=2)


def start(args, cwd, log_name):
    log = open(os.path.join(cwd, log_name), 'w', encoding='utf-8')
    proc = subprocess.Popen(args, cwd=cwd, stdout=log, stderr=subprocess.STDOUT, stdin=subprocess.DEVNULL,
                            creationflags=getattr(subprocess, 'CREATE_NO_WINDOW', 0))
    print(f'[releasetest] started PID {proc.pid}, log {os.path.join(cwd, log_name)}', flush=True)
    return proc


def run_logged(args, cwd, log_path):
    with open(log_path, 'w', encoding='utf-8') as log:
        return subprocess.call(args, cwd=cwd, stdout=log, stderr=subprocess.STDOUT, stdin=subprocess.DEVNULL,
                               creationflags=getattr(subprocess, 'CREATE_NO_WINDOW', 0))


def installer_jar(group, artifact, version, url):
    cached = from_m2(group, artifact, version, 'installer')
    return cached or download(url, os.path.join(DOWNLOADS, os.path.basename(url)))


# --- servers ------------------------------------------------------------------------------------------------------

def install_server(loader, server_dir):
    """Installs the loader's dedicated server into server_dir; returns the java arguments that start it there."""
    os.makedirs(server_dir, exist_ok=True)
    if loader == 'fabric':
        meta = json.load(urllib.request.urlopen('https://meta.fabricmc.net/v2/versions/installer'))
        installer = next(v['version'] for v in meta if v.get('stable'))
        # The official server launcher: on its first start it fetches the vanilla server and the loader's libraries.
        launcher = f'fabric-server-launch-{fabric_loader()}.jar'  # one per loader version: download() keeps a file
        download(f"https://meta.fabricmc.net/v2/versions/loader/{MC}/{fabric_loader()}/{installer}/server/jar",
                 os.path.join(server_dir, launcher))
        return ['-jar', launcher, 'nogui']
    if loader == 'quilt':
        if not os.path.exists(os.path.join(server_dir, 'quilt-server-launch.jar')):
            meta = urllib.request.urlopen('https://maven.quiltmc.org/repository/release/org/quiltmc/quilt-installer/maven-metadata.xml').read().decode()
            version = re.search(r'<release>([^<]+)</release>', meta).group(1)
            installer = download(f'https://maven.quiltmc.org/repository/release/org/quiltmc/quilt-installer/{version}/quilt-installer-{version}.jar',
                                 os.path.join(DOWNLOADS, f'quilt-installer-{version}.jar'))
            # The install dir is given relative to its parent: this installer splits a path with a space in it.
            code = run_logged([java(), '-jar', installer, 'install', 'server', MC, QUILT_RUN, '--download-server',
                               '--install-dir=' + os.path.basename(server_dir)], os.path.dirname(server_dir),
                              os.path.join(os.path.dirname(server_dir), 'install-server.log'))
            if code != 0 or not os.path.exists(os.path.join(server_dir, 'quilt-server-launch.jar')):
                raise SystemExit(f'Quilt server install failed ({code}); see install-server.log')
        return ['-jar', 'quilt-server-launch.jar', 'nogui']
    if loader == 'neoforge':
        args_file = f'libraries/net/neoforged/neoforge/{NEOFORGE}/win_args.txt'
        if not os.path.exists(os.path.join(server_dir, args_file)):
            installer = installer_jar('net.neoforged', 'neoforge', NEOFORGE,
                                      f'https://maven.neoforged.net/releases/net/neoforged/neoforge/{NEOFORGE}/neoforge-{NEOFORGE}-installer.jar')
            code = run_logged([java(), '-jar', installer, '--install-server', server_dir], DOWNLOADS,
                              os.path.join(os.path.dirname(server_dir), 'install-server.log'))
            if code != 0:
                raise SystemExit(f'NeoForge server install failed ({code}); see install-server.log')
        return ['@user_jvm_args.txt', '@' + args_file, 'nogui']
    if loader == 'forge':
        shim = f'forge-{FORGE}-shim.jar'
        args_file = f'libraries/net/minecraftforge/forge/{FORGE}/win_args.txt'
        if not os.path.exists(os.path.join(server_dir, shim)) and not os.path.exists(os.path.join(server_dir, args_file)):
            installer = installer_jar('net.minecraftforge', 'forge', FORGE,
                                      f'https://maven.minecraftforge.net/net/minecraftforge/forge/{FORGE}/forge-{FORGE}-installer.jar')
            code = run_logged([java(), '-jar', installer, '--installServer', server_dir], DOWNLOADS,
                              os.path.join(os.path.dirname(server_dir), 'install-server.log'))
            if code != 0:
                raise SystemExit(f'Forge server install failed ({code}); see install-server.log')
        if os.path.exists(os.path.join(server_dir, shim)):
            return ['@user_jvm_args.txt', '-jar', shim, 'nogui']
        return ['@user_jvm_args.txt', '@' + args_file, 'nogui']
    raise SystemExit(f'unknown loader {loader}')


def server(loader, port, rcon_port, extra=()):
    os.makedirs(DOWNLOADS, exist_ok=True)
    server_dir = os.path.join(WORK, loader, 'server')
    args = install_server(loader, server_dir)
    set_mods(loader, server_dir, extra)
    servertest.prepare(server_dir, int(port), int(rcon_port))  # EULA, whitelist off (both keys), RCON
    with open(os.path.join(server_dir, 'ops.json'), 'w', encoding='utf-8') as f:
        json.dump([{'uuid': offline_uuid(PLAYER), 'name': PLAYER, 'level': 4, 'bypassesPlayerLimit': False}], f)
    return start([java(), MARKER, SERVER_SELECTOR, '-Xmx3G'] + NO_GUI + args, server_dir, 'server.log')


def wait_for_server(proc, server_dir, rcon_port, guard_log=None, timeout=900):
    """Waits for the server's RCON, failing at once on a loader error, a crash, an error window or an early exit."""
    log = os.path.join(server_dir, 'server.log')
    t0 = time.time()
    while time.time() - t0 < timeout:
        text = open(log, encoding='utf-8', errors='replace').read() if os.path.exists(log) else ''
        failure = launchwatch.check(text, guard_log)
        if failure or proc.poll() is not None:
            launchwatch.stop_tree(proc.pid)
            raise RuntimeError(f'the server did not start: {failure or "exited with " + str(proc.poll())}; '
                               f'last lines: {" | ".join(text.splitlines()[-6:])}')
        try:
            return servertest.Rcon(int(rcon_port))
        except OSError:
            time.sleep(2)
    raise TimeoutError('the server never opened RCON')


# --- clients ------------------------------------------------------------------------------------------------------

def install_vanilla():
    vdir = os.path.join(LAUNCHER, 'versions', MC)
    os.makedirs(vdir, exist_ok=True)
    version_json = os.path.join(vdir, MC + '.json')
    if not os.path.exists(version_json):
        cached = os.path.join(LOOM, MC, 'mojang_minecraft_info.json')
        if os.path.exists(cached):
            shutil.copy(cached, version_json)
        else:
            manifest = json.load(urllib.request.urlopen('https://piston-meta.mojang.com/mc/game/version_manifest_v2.json'))
            entry = next(v for v in manifest['versions'] if v['id'] == MC)
            download(entry['url'], version_json, entry['sha1'])
    client = json.load(open(version_json, encoding='utf-8'))['downloads']['client']
    jar = os.path.join(vdir, MC + '.jar')
    if not os.path.exists(jar) or sha1(jar) != client['sha1']:
        cached = os.path.join(LOOM, MC, 'minecraft-client.jar')
        if os.path.exists(cached) and sha1(cached) == client['sha1']:
            shutil.copy(cached, jar)
        else:
            download(client['url'], jar, client['sha1'])
    profiles = os.path.join(LAUNCHER, 'launcher_profiles.json')
    if not os.path.exists(profiles):
        # The Forge and NeoForge installers only install into a folder a launcher has been run in.
        with open(profiles, 'w', encoding='utf-8') as f:
            f.write('{"profiles":{},"settings":{},"version":3}')


def install_client(loader):
    """The loader's launcher profile in LAUNCHER; returns the version id to start."""
    install_vanilla()
    if loader in ('fabric', 'quilt'):
        if loader == 'fabric':
            vid = f"fabric-loader-{fabric_loader()}-{MC}"
            url = f"https://meta.fabricmc.net/v2/versions/loader/{MC}/{fabric_loader()}/profile/json"
        else:
            vid = f'quilt-loader-{QUILT_RUN}-{MC}'
            url = f'https://meta.quiltmc.org/v3/versions/loader/{MC}/{QUILT_RUN}/profile/json'
        download(url, os.path.join(LAUNCHER, 'versions', vid, vid + '.json'))
        return vid
    if loader == 'neoforge':
        vid = f'neoforge-{NEOFORGE}'
        if not os.path.exists(os.path.join(LAUNCHER, 'versions', vid, vid + '.json')):
            installer = installer_jar('net.neoforged', 'neoforge', NEOFORGE,
                                      f'https://maven.neoforged.net/releases/net/neoforged/neoforge/{NEOFORGE}/neoforge-{NEOFORGE}-installer.jar')
            code = run_logged([java(), '-jar', installer, '--install-client', LAUNCHER], DOWNLOADS,
                              os.path.join(WORK, 'install-client-neoforge.log'))
            if code != 0:
                raise SystemExit(f'NeoForge client install failed ({code})')
        return vid
    if loader == 'forge':
        vid = f"{MC}-forge-{PROPS['forge_version']}"
        if not os.path.exists(os.path.join(LAUNCHER, 'versions', vid, vid + '.json')):
            installer = installer_jar('net.minecraftforge', 'forge', FORGE,
                                      f'https://maven.minecraftforge.net/net/minecraftforge/forge/{FORGE}/forge-{FORGE}-installer.jar')
            code = run_logged([java(), '-jar', installer, '--installClient', LAUNCHER], DOWNLOADS,
                              os.path.join(WORK, 'install-client-forge.log'))
            if code != 0:
                raise SystemExit(f'Forge client install failed ({code})')
        return vid
    raise SystemExit(f'unknown loader {loader}')


def rules_allow(rules):
    if not rules:
        return True
    allowed = False
    for rule in rules:
        applies = True
        if rule.get('features'):
            applies = False  # demo mode, custom resolution, quick play: the launcher's own features, none in use
        os_rule = rule.get('os', {})
        if os_rule.get('name') and os_rule['name'] != 'windows':
            applies = False
        if os_rule.get('arch') and os_rule['arch'] not in ('x86_64', 'amd64'):
            applies = False
        if applies:
            allowed = rule['action'] == 'allow'
    return allowed


def expand(entries):
    out = []
    for entry in entries:
        if isinstance(entry, str):
            out.append(entry)
        elif rules_allow(entry.get('rules')):
            value = entry['value']
            out.extend(value if isinstance(value, list) else [value])
    return out


def maven_path(name):
    ext = 'jar'
    if '@' in name:
        name, ext = name.split('@', 1)
    parts = name.split(':')
    file = f'{parts[1]}-{parts[2]}' + (f'-{parts[3]}' if len(parts) > 3 else '')
    return f"{parts[0].replace('.', '/')}/{parts[1]}/{parts[2]}/{file}.{ext}"


def library_key(name):
    parts = name.split('@')[0].split(':')
    return ':'.join([parts[0], parts[1]] + ([parts[3]] if len(parts) > 3 else []))


def resolve(vid):
    data = json.load(open(os.path.join(LAUNCHER, 'versions', vid, vid + '.json'), encoding='utf-8'))
    version = {'main': data.get('mainClass'), 'libraries': list(data.get('libraries', [])),
               'jvm': list(data.get('arguments', {}).get('jvm', [])), 'game': list(data.get('arguments', {}).get('game', [])),
               'assets': data.get('assetIndex', {}).get('id')}
    if data.get('inheritsFrom'):
        parent = resolve(data['inheritsFrom'])
        own = {library_key(lib['name']) for lib in version['libraries']}
        version['libraries'] += [lib for lib in parent['libraries'] if library_key(lib['name']) not in own]
        version['jvm'] = parent['jvm'] + version['jvm']
        version['game'] = parent['game'] + version['game']
        version['main'] = version['main'] or parent['main']
        version['assets'] = version['assets'] or parent['assets']
    return version


def library_file(lib):
    artifact = lib.get('downloads', {}).get('artifact')
    rel = artifact['path'] if artifact and artifact.get('path') else maven_path(lib['name'])
    path = os.path.join(LAUNCHER, 'libraries', rel.replace('/', os.sep))
    if not os.path.exists(path):
        parts = lib['name'].split('@')[0].split(':')
        cached = from_m2(parts[0], parts[1], parts[2], parts[3] if len(parts) > 3 else None)
        if cached:
            os.makedirs(os.path.dirname(path), exist_ok=True)
            shutil.copy(cached, path)
        elif artifact and artifact.get('url'):
            download(artifact['url'], path, artifact.get('sha1'))
        elif lib.get('url'):
            download(lib['url'].rstrip('/') + '/' + rel, path)
        else:
            raise SystemExit(f"library {lib['name']} is neither installed nor downloadable")
    return path


def client(loader, game_dir=None, mods=(), pack=None, join_port=None, world=None, without_architectury=False):
    os.makedirs(DOWNLOADS, exist_ok=True)
    vid = install_client(loader)
    version = resolve(vid)
    game_dir = game_dir or os.path.join(WORK, loader, 'client')
    os.makedirs(game_dir, exist_ok=True)
    set_mods(loader, game_dir, mods, without_architectury)
    set_shaderpack(game_dir, pack)
    set_dynamic_fps(game_dir, mods)
    set_wover(game_dir, mods)
    testclient.prepare(game_dir, loader)
    classpath = []
    for lib in version['libraries']:
        if rules_allow(lib.get('rules')) and not lib.get('natives'):
            path = library_file(lib)
            if path not in classpath:
                classpath.append(path)
    # Like the launcher: the profile's own client jar is a copy of the vanilla one it inherits.
    jar = os.path.join(LAUNCHER, 'versions', vid, vid + '.jar')
    if not os.path.exists(jar) or os.path.getsize(jar) == 0:
        shutil.copy(os.path.join(LAUNCHER, 'versions', MC, MC + '.jar'), jar)
    classpath.append(jar)
    natives = os.path.join(game_dir, 'natives')
    os.makedirs(natives, exist_ok=True)
    assets_root = os.path.join(LOOM, 'assets')
    index = version['assets']
    if not os.path.exists(os.path.join(assets_root, 'indexes', index + '.json')):
        # Loom keeps its copy of the index as <mc>-<id>.json; the objects folder is shared.
        index = f'{MC}-{index}'
    values = {
        'natives_directory': natives, 'launcher_name': 'mocreatures-releasetest', 'launcher_version': '1',
        'classpath': os.pathsep.join(classpath), 'classpath_separator': os.pathsep,
        'library_directory': os.path.join(LAUNCHER, 'libraries'), 'version_name': vid,
        'auth_player_name': PLAYER, 'game_directory': game_dir, 'assets_root': assets_root,
        'assets_index_name': index, 'auth_uuid': offline_uuid(PLAYER).replace('-', ''), 'auth_access_token': '0',
        'clientid': '0', 'auth_xuid': '0', 'version_type': 'release', 'user_type': 'legacy',
    }
    extra_game = []
    if join_port:
        extra_game += ['--quickPlayMultiplayer', f'127.0.0.1:{join_port}']
    if world:
        extra_game += ['--quickPlaySingleplayer', world]
    args = ['-Xmx4G', MARKER, f'-javaagent:{testclient.build_agent()}'] + NO_GUI + expand(version['jvm']) \
        + [version['main']] + expand(version['game']) + extra_game
    lines = []
    for arg in args:
        text = re.sub(r'\$\{(\w+)\}', lambda m: values.get(m.group(1), m.group(0)), arg)
        # In a java @argument file a quoted string takes backslash as an escape character.
        lines.append('"' + text.replace('\\', '\\\\').replace('"', '\\"') + '"')
    argfile = os.path.join(game_dir, 'launch-arguments.txt')
    with open(argfile, 'w', encoding='utf-8', newline='\n') as f:  # no BOM: java would read it as part of an argument
        f.write('\n'.join(lines) + '\n')
    return start([java(), '@' + argfile], game_dir, 'client.log')


def copy_world(loader, client_dir, name):
    src = os.path.join(WORK, loader, 'server', 'world')
    dst = os.path.join(client_dir, 'saves', name)
    shutil.rmtree(dst, ignore_errors=True)
    shutil.copytree(src, dst, ignore=shutil.ignore_patterns('session.lock'))
    testclient.patch_world(dst)
    print(f'[releasetest] copied {src} to {dst}', flush=True)


# --- the whole check ----------------------------------------------------------------------------------------------

# Case-sensitive on purpose: "ERROR" as a log level, not inside words like removeErroringEntities, and no mod names
# such as fabric-crash-report-info.
PROBLEMS = re.compile(r'Mixin apply failed|Critical injection failure|InvalidInjectionException|MixinTransformerError'
                      r'|InvalidAccessorException|InvalidMixinException'
                      r'|ClassNotFoundException|NoClassDefFoundError|NoSuchMethodError|NoSuchFieldError'
                      r'|Reference map|refmap|Exception|/ERROR\]|/FATAL\]|---- Minecraft Crash Report|crash-reports[/\\]'
                      r'|Preparing crash report|Failed to load|Mod loading has failed|Loading errors encountered'
                      r'|Failed to complete lifecycle event|Missing mods|Incompatible mods|which is missing'
                      r'|deprecated `logoFile`|Shaders are disabled because enableShaders'
                      r'|Failed to create iris render pipeline|Pipeline data already bound')
# Seen on every run on this machine and unrelated to the mod: OSHI's process queries, the offline account against
# Mojang's and Realms' services, vanilla's unprimed-heightmap log line, Fabric API's optional Amecs mixin, netty
# probing its Linux/macOS transports on Windows (Forge/NeoForge log that through log4j's debug appender), and Iris
# 1.11.x naming refmaps it does not ship (26.x is unobfuscated).
NOISE = re.compile(r'oshi|OSProcess|Perflib|SystemReport|Win32Exception: The parameter is incorrect|Realms|realms'
                   r'|SignedJWT|InvalidCredentialsException|MinecraftClientHttpException|status=401|Status: 401'
                   r'|Failed to fetch user properties|Failed to retrieve profile key pair|Unprimed heightmap'
                   r'|de/siphalor/amecs|minecraft\.client\.MinecraftClient|authlib|CrashReport\.(<init>|preload)'
                   r'|io\.netty\.channel\.(epoll|kqueue)|Only supported on (Linux|OSX/BSD)|AppenderLoggingException'
                   r"|Reference map 'iris[-a-z]*\.refmap\.json'"
                   # another mod's optional mixin whose target mod is not installed (Iris 1.11.4 for NeoForge ships
                   # mixins for Mekanism, Immersive Engineering, Just Dire Things, Cable Facades); a missing class of
                   # Mo'Creatures itself (drzhark/...) still counts
                   # (NeoForge tags the line [mixin/], Fabric does not)
                   r"|Error loading class: (?!drzhark/)\S+ \(java\.lang\.ClassNotFoundException"
                   # other mods' own notices in the compatibility sets: their refmaps (26.x needs none), Fabric Loader's
                   # "recommends ... which is missing", Distant Horizons closing its database at shutdown
                   r"|Reference map '(?!mocreatures)[^']+' for \S+ could not be read|recommends (any )?version"
                   r"|SQLException: stmt pointer is closed"
                   # BCLib/WorldWeaver (popular set) patching the level before a fresh world has its level.dat
                   r"|Failed fixing Level-Data|There were Errors while fixing the Level|WorldPresetInfoRegistry: Registry not read"
                   r"|NoSuchFileException: \S*world\S*level\.dat")


def scan_log(path):
    """(problem lines, whether Mo'Creatures loaded). Stack frames under a known-noise line are skipped with it."""
    if not os.path.exists(path):
        return [f'no log at {path}'], False
    lines = open(path, encoding='utf-8', errors='replace').read().splitlines()
    problems, in_noise = [], False
    for line in lines:
        # a stack frame, also one printed through System.err (Fabric logs it as "[STDERR]: <tab>at ...")
        frame = line.startswith('\t') or line.startswith('Caused by') \
            or re.search(r'\[STDERR\]: \s*(at |Caused by)', line) is not None
        if frame and in_noise:
            continue
        in_noise = False
        if NOISE.search(line):
            in_noise = True
        elif PROBLEMS.search(line) and not frame:
            problems.append(line[:300])
    # A mod list line with the version (Fabric, Quilt, NeoForge) or the mod's own boot line.
    loaded = any(('mocreatures' in l.lower() and PROPS['mod_version'] in l)
                 or re.search(r'initialized with \d+ creatures', l) for l in lines)
    return problems, loaded


def retire_log(client_dir):
    """Moves the client's last latest.log away, so the log kept after a session is that session's (a client that dies
    before it starts logging leaves no latest.log, and the copy then fails loudly instead of keeping an old log)."""
    latest = os.path.join(client_dir, 'logs', 'latest.log')
    os.makedirs(os.path.dirname(latest), exist_ok=True)
    if os.path.exists(latest):
        os.replace(latest, os.path.join(client_dir, 'logs', 'previous-latest.log'))


def wait_or_kill(proc, seconds, label):
    try:
        return proc.wait(timeout=seconds)
    except subprocess.TimeoutExpired:
        # Our own child (its command line carries MARKER with this project's path), so stopping it is safe.
        print(f'[check] {label} did not exit within {seconds}s; stopping PID {proc.pid}', flush=True)
        launchwatch.stop_tree(proc.pid)
        return proc.wait()


def start_guard(out_dir):
    """windowguard.ps1 for this run, with a fresh log (an old DIALOG line would fail the new run)."""
    log = os.path.join(out_dir, 'windowguard.log')
    if os.path.exists(log):
        os.replace(log, log + '.previous')
    proc = subprocess.Popen(['powershell', '-NoProfile', '-ExecutionPolicy', 'Bypass', '-File',
                             os.path.join(ROOT, 'tools', 'testclient', 'windowguard.ps1'), '-Seconds', '7200',
                             '-Log', log], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL,
                            creationflags=getattr(subprocess, 'CREATE_NO_WINDOW', 0))
    return proc, log


def check(loader, port, rcon_port, set_name='plain', tour_only=False):
    import moctour  # tools/testclient
    out_dir = os.path.join(WORK, loader if set_name == 'plain' else f'{loader}-{set_name.replace(":", "-")}')
    server_dir = os.path.join(WORK, loader, 'server')
    client_dir = os.path.join(out_dir, 'client')
    server_log = os.path.join(server_dir, 'server.log')
    os.makedirs(out_dir, exist_ok=True)
    results, notes = [], []

    def record(name, ok, detail=''):
        results.append((name, bool(ok), detail))
        print(f'[check] {"PASS" if ok else "FAIL"} {name} {detail}', flush=True)

    mods, server_mods, pack, set_notes = pick_set(loader, set_name)
    write_modset(out_dir, loader, set_name, mods, server_mods, pack, set_notes)
    if loader == 'fabric':
        fabric_api_jar()
    set_notes = [f'loader: ' + (f'Fabric Loader {fabric_loader()}, Fabric API {_fabric_api[0][0]}'
                                if loader == 'fabric' else f'NeoForge {NEOFORGE}')] + set_notes
    guard, guard_log = start_guard(out_dir)
    for stale in ('screenshots',):
        shutil.rmtree(os.path.join(client_dir, stale), ignore_errors=True)
    for f in glob.glob(os.path.join(out_dir, 'check-*.log')) + glob.glob(os.path.join(out_dir, 'dump-*.txt')):
        os.remove(f)

    def session(tag, srv=None, rcon=None, **where):
        """One client session running the tour. A login that stalls or a whitelist kick is reported, its cause dealt
        with, and the leg joined once more (server-testing rule); any other launch failure ends the leg at once."""
        for attempt in (1, 2):
            retire_log(client_dir)
            proc = client(loader, client_dir, mods, pack, **where)
            watch = moctour.Watch(guard_log, proc.pid, srv.pid if srv else None, server_log if srv else None,
                                  os.path.join(out_dir, f'dump-{tag}'))
            try:
                failed = moctour.run(client_dir, os.path.join(client_dir, 'client.log'), f'{loader}_{tag}', watch)
            except moctour.LaunchFailure as e:
                launchwatch.stop_tree(proc.pid)
                proc.wait()
                reason = str(e)
                retry = attempt == 1 and ('login stalled' in reason or 'white-listed' in reason)
                if retry and 'white-listed' in reason and rcon is not None:
                    rcon.cmd('whitelist off')
                if not retry:
                    record(f'{tag}: the client got in and ran the tour', False, reason[:600])
                    return
                notes.append(f'{tag}: attempt 1 failed and was joined again: {reason[:300]}')
                print(f'[check] {tag}: {reason[:300]} - joining again', flush=True)
                continue
            record(f'{tag}: client tour (tools/testclient/moctour.py)', failed == 0,
                   'all checks passed' if not failed else f'{failed} check(s) FAILED, see the [moctour] lines')
            code = wait_or_kill(proc, 180, f'{tag} client')
            record(f'{tag}: client quit cleanly', code == 0, f'exit code {code}')
            return

    srv = None
    try:
        # 1. the dedicated server with the release jar (and the set's server-capable mods) and a fresh world
        shutil.rmtree(os.path.join(server_dir, 'world'), ignore_errors=True)
        srv = server(loader, port, rcon_port, server_mods)
        rcon = wait_for_server(srv, server_dir, rcon_port, guard_log)
        record('dedicated server started with the release jar' + (f' + {len(server_mods)} mods of the set'
                                                                     if server_mods else ''), True)
        if (set_name == 'plain' or server_mods) and not tour_only:
            fails = servertest.run(int(rcon_port), server_dir, os.path.join(out_dir, 'servertest.txt'))
            record('server content and mechanics (tools/servertest/servertest.py)', not fails,
                   'all passed' if not fails else str(fails))
        else:
            # the server is the plain one, or only the client tour is being re-run: just the tour's platform
            servertest.platform(rcon)
            rcon.cmd('save-all flush')

        # 2. a real client joins it
        session('mp', srv, rcon, join_port=port)
        shutil.copy(os.path.join(client_dir, 'logs', 'latest.log'), os.path.join(out_dir, 'check-client-mp.log'))
        shutil.copy(os.path.join(client_dir, 'client.log'), os.path.join(out_dir, 'check-client-mp-console.log'))

        print('[check] stopping the server: ' + rcon.cmd('stop').strip(), flush=True)
        code = wait_or_kill(srv, 180, 'server')
        record('server stopped cleanly', code == 0, f'exit code {code}')
        srv = None
        shutil.copy(os.path.join(server_dir, 'logs', 'latest.log'), os.path.join(out_dir, 'check-server.log'))

        # 3. singleplayer in a copy of that world
        copy_world(loader, client_dir, 'moctest')
        session('sp', world='moctest')
        shutil.copy(os.path.join(client_dir, 'logs', 'latest.log'), os.path.join(out_dir, 'check-client-sp.log'))
        shutil.copy(os.path.join(client_dir, 'client.log'), os.path.join(out_dir, 'check-client-sp-console.log'))
    except Exception as e:
        record('check ran to the end', False, repr(e)[:600])
    finally:
        if srv is not None and srv.poll() is None:
            try:
                servertest.Rcon(int(rcon_port)).cmd('stop')
            except Exception:
                pass
            wait_or_kill(srv, 120, 'server')

    # 4. the logs
    for name in ('server', 'client-mp', 'client-sp'):
        problems, loaded = scan_log(os.path.join(out_dir, f'check-{name}.log'))
        record(f'{name} log: no problem lines', not problems, ' || '.join(problems[:10]))
        record(f"{name} log: Mo'Creatures loaded", loaded)
    if pack is not None:
        for name in ('client-mp', 'client-sp'):
            path = os.path.join(out_dir, f'check-{name}.log')
            text = open(path, encoding='utf-8', errors='replace').read() if os.path.exists(path) else ''
            dims = sorted(set(re.findall(r'Creating pipeline for dimension (\S+)', text)))
            bad = SHADER_FAILURE.findall(text)
            # Iris builds one pipeline per dimension FOLDER of the pack; a modded dimension like the Wyvern Lair has
            # none in any pack, so it shares the overworld's and no second "Creating pipeline" line is logged (the
            # Lair screenshot shows the pack on there).
            record(f'{name}: shader pack {pack.filename} ON (Iris built its pipeline, no shader failures)',
                   'minecraft:overworld' in dims and not bad,
                   f'Iris pipelines: {dims}' + (f'; failures: {bad[:3]}' if bad else ''))
    time.sleep(2)
    launchwatch.stop_pid(guard.pid)
    if os.path.exists(guard_log):
        text = open(guard_log, encoding='utf-8', errors='replace').read()
        bad = [l for l in text.splitlines() if 'VIOLATION' in l or 'DIALOG' in l]
        record('client windows stayed minimized and unfocused, no error window (windowguard)', not bad,
               ' | '.join(bad[:4]))
    shots_dir = os.path.join(client_dir, 'screenshots')
    shots = sorted(os.listdir(shots_dir)) if os.path.isdir(shots_dir) else []

    passed = sum(1 for _, ok, _ in results if ok)
    lines = [f'release check {loader} [{set_name}{", tour only" if tour_only else ""}]: '
             f'{passed}/{len(results)} passed'] + \
            [f'  {"PASS" if ok else "FAIL"} {n} {d}' for n, ok, d in results] + \
            [f'  note: {n}' for n in notes + set_notes] + [f'  {len(shots)} screenshots in {shots_dir}']
    open(os.path.join(out_dir, 'summary.txt'), 'w', encoding='utf-8').write('\n'.join(lines) + '\n')
    print('\n'.join(lines), flush=True)
    return 0 if passed == len(results) else 1


# --- probes: is a failed launch really caught? (verify once per project and version) -------------------------------

PROBE_WINDOW = '''import javax.swing.*;

/** The release check's window probe: a loader-like error box that must be caught by the window guard. */
public class ProbeWindow {
    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            JFrame frame = new JFrame("Quilt Loader - probe window (Mo'Creatures release check)");
            frame.setFocusableWindowState(false);
            frame.setAutoRequestFocus(false);
            frame.setSize(360, 120);
            frame.setExtendedState(JFrame.ICONIFIED);
            frame.setVisible(true);
        });
        Thread.sleep(180_000L);
        System.exit(0);
    }
}
'''


def probe(kind, loader, port=None):
    import moctour  # tools/testclient
    out_dir = os.path.join(WORK, f'{loader}-probe-{kind}')
    client_dir = os.path.join(out_dir, 'client')
    os.makedirs(client_dir, exist_ok=True)
    guard, guard_log = start_guard(out_dir)
    t0 = time.time()
    result = None
    proc = None
    try:
        if kind == 'window':
            src = os.path.join(out_dir, 'ProbeWindow.java')
            with open(src, 'w', encoding='utf-8', newline='\n') as f:
                f.write(PROBE_WINDOW)
            proc = start([java(), MARKER, src], out_dir, 'probe.log')
            while time.time() - t0 < 60:
                failure = launchwatch.check('', guard_log)
                if failure:
                    launchwatch.stop_tree(proc.pid)
                    code = proc.wait(timeout=30)
                    result = (f'PASS: flagged {time.time() - t0:.1f} s after the probe started (JVM start-up included) '
                              f'and PID {proc.pid} stopped (exit {code}): {failure}')
                    break
                time.sleep(0.5)
            else:
                result = 'FAIL: the window guard never flagged the probe window'
        else:
            retire_log(client_dir)
            proc = client(loader, client_dir, join_port=port if kind == 'connect' else None,
                          without_architectury=kind == 'refused')
            watch = moctour.Watch(guard_log, proc.pid, dump_prefix=os.path.join(out_dir, 'dump'))
            log = os.path.join(client_dir, 'client.log')
            try:
                reached = moctour.wait_for(log, r'\[testclient\] player in world', 600, watch=watch)
                result = f'FAIL: no failure reported ({"the player got into a world" if reached else "timed out"})'
            except moctour.LaunchFailure as e:
                text = moctour.read_from(log)
                m = re.search(r'\[testclient\] (?:heartbeat t=\d+ \S+ \S+ \S+ screen=\S*ConnectScreen|screen -> \S*ConnectScreen)',
                              text)
                since = '' if kind != 'connect' else \
                    (' (the connect screen was up before it)' if m else ' (no connect screen line seen before it)')
                launchwatch.stop_tree(proc.pid)
                proc.wait(timeout=60)
                dialog = 'DIALOG' in (open(guard_log, encoding='utf-8', errors='replace').read()
                                       if os.path.exists(guard_log) else '')
                ok = (kind == 'connect') or not dialog
                result = (f'{"PASS" if ok else "FAIL"}: reported {time.time() - t0:.0f} s after the launch{since}; '
                          f'error window seen: {dialog}; {str(e)[:700]}')
    finally:
        if proc is not None and proc.poll() is None:
            launchwatch.stop_tree(proc.pid)
        time.sleep(1)
        launchwatch.stop_pid(guard.pid)
    print(f'[probe {kind}] {loader}: {result}', flush=True)
    open(os.path.join(out_dir, 'summary.txt'), 'w', encoding='utf-8').write(result + '\n')
    return 0 if result.startswith('PASS') else 1


if __name__ == '__main__':
    args = sys.argv[1:]
    set_name = 'plain'
    if '--set' in args:
        i = args.index('--set')
        set_name = args[i + 1]
        del args[i:i + 2]
    tour_only = '--tour-only' in args
    if tour_only:
        args.remove('--tour-only')
    if len(args) == 4 and args[0] == 'check':
        sys.exit(check(args[1], args[2], args[3], set_name, tour_only))
    if len(args) >= 3 and args[0] == 'probe':
        sys.exit(probe(args[1], args[2], args[3] if len(args) > 3 else None))
    if len(args) == 2 and args[0] == 'sets':
        print('top shader packs:', top_slugs(args[1], [['project_type:shader'], [f'versions:{MC}']], 5))
        for name in ('perf', 'popular'):
            mods, server_mods, pack, notes = pick_set(args[1], name)
            print(name, [f'{m.slug} {m.version}' for m in mods], 'server:', [m.slug for m in server_mods], notes)
        sys.exit(0)
    if len(args) == 4 and args[0] == 'server':
        code = server(args[1], args[2], args[3]).wait()
        print(f'[releasetest] server exited with code {code}')
        sys.exit(0)
    if len(args) >= 2 and args[0] == 'client':
        opts = args[2:]
        code = client(args[1], join_port=opts[opts.index('--join') + 1] if '--join' in opts else None,
                      world=opts[opts.index('--world') + 1] if '--world' in opts else None).wait()
        print(f'[releasetest] client exited with code {code}')
        sys.exit(0)
    raise SystemExit(__doc__)
