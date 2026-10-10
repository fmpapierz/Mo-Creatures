"""Release check: the RELEASE jars from build/libs on real (production) installs of each loader, the way players get
them - no Gradle, no dev classpath, no merged client/server code.

    py tools/releasetest/releasetest.py server <loader> <server port> <rcon port>
        Installs the loader's dedicated server (official installer or launcher), puts the release jar, Architectury
        API and (on Fabric) Fabric API in mods/, accepts the EULA (user rule), enables RCON and starts it. Prints its PID and log
        and waits for it to exit (drive it with tools/servertest/servertest.py over RCON; `stop` ends it).
    py tools/releasetest/releasetest.py client <loader> [--join <port>] [--world <name>]
        Installs the loader's client launcher profile next to the vanilla 26.2 client, puts the release jar and its
        dependencies in mods/ and starts the game offline with the test-client agent: minimized, unfocused, prompts answered,
        scriptable through tools/testclient (screenshot / cmd / quit). --join quick-plays into the local server on that
        port, --world into a save of that name (copy one into the client's saves/ first).
    py tools/releasetest/releasetest.py copy-world <loader> <world name>
        Copies the loader's release-test server world into its client's saves/ (cheats on, see testclient.patch_world).
    py tools/releasetest/releasetest.py check <loader> <server port> <rcon port>
        The whole check for one loader, unattended: a fresh server world and the content + mechanics check over RCON
        (tools/servertest/servertest.py: every entity, loot table, the kitty bed, MoC x vanilla horse breeding, led
        animals over a step), a client that joins the server for the client checks (tools/testclient/moctour.py:
        the horse taming hint, a vanilla horse mount, scorpion and kitty-bed screenshots), the server stopped, then a
        singleplayer session in a copy of its world (the same client checks). Ends with what the server and both client
        logs showed; keeps the logs as build/releasetest/<loader>/check-{server,client-mp,client-sp}.log and the
        screenshots in the client's screenshots/.

loader is fabric or neoforge (the loaders Mo'Creatures ships for; the quilt and forge code paths are kept from
the script this was adapted from, Alex's Caves 26.2's). Installs live in build/releasetest/<loader>/{server,client}; the vanilla
client, the libraries and the loader profiles in build/releasetest/launcher (a .minecraft-like folder the Forge and
NeoForge installers can install into). Mojang's files come from Loom's cache when it has them. Every java command
carries -Dmoctest.releasetest=<this project>, so its process can be matched to this project (machine rule).
"""
import glob, hashlib, json, os, re, shutil, subprocess, sys, urllib.request, uuid

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(os.path.dirname(HERE))
sys.path.insert(0, os.path.join(ROOT, 'tools', 'testclient'))
sys.path.insert(0, os.path.join(ROOT, 'tools', 'servertest'))
import testclient  # noqa: E402
import servertest  # noqa: E402


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


def java():
    return os.path.join(testclient.find_jdk_bin(), 'java.exe')


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
    urllib.request.urlretrieve(url, tmp)
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


def fabric_api_jar():
    jar = from_m2('net.fabricmc.fabric-api', 'fabric-api', FABRIC_API)
    if not jar:
        jar = download(f"https://maven.fabricmc.net/net/fabricmc/fabric-api/fabric-api/{FABRIC_API}/"
                       f"fabric-api-{FABRIC_API}.jar", os.path.join(DOWNLOADS, f"fabric-api-{FABRIC_API}.jar"))
    return jar


def architectury_jar(loader):
    """The released Architectury API for this loader (a required dependency), from Gradle's cache or its Maven."""
    artifact = f'architectury-{loader}'
    jar = from_m2('dev.architectury', artifact, ARCHITECTURY)
    if not jar:
        jar = download(f'https://maven.architectury.dev/dev/architectury/{artifact}/{ARCHITECTURY}/'
                       f'{artifact}-{ARCHITECTURY}.jar', os.path.join(DOWNLOADS, f'{artifact}-{ARCHITECTURY}.jar'))
    return jar


def set_mods(loader, game_dir):
    """A clean mods/ holding only the release jar under test and what it needs on that loader."""
    mods = os.path.join(game_dir, 'mods')
    os.makedirs(mods, exist_ok=True)
    for old in glob.glob(os.path.join(mods, '*.jar')):
        os.remove(old)
    if loader == 'quilt':
        # Quilt Loader 0.31 fails to read a transform cache written before the mods changed; start from a clean one.
        shutil.rmtree(os.path.join(game_dir, '.cache', 'quilt_loader'), ignore_errors=True)
    shutil.copy(release_jar(loader), mods)
    shutil.copy(architectury_jar('fabric' if loader in ('fabric', 'quilt') else loader), mods)
    if loader in ('fabric', 'quilt'):
        shutil.copy(fabric_api_jar(), mods)


def start(args, cwd, log_name):
    log = open(os.path.join(cwd, log_name), 'w', encoding='utf-8')
    proc = subprocess.Popen(args, cwd=cwd, stdout=log, stderr=subprocess.STDOUT, stdin=subprocess.DEVNULL,
                            creationflags=getattr(subprocess, 'CREATE_NO_WINDOW', 0))
    print(f'[releasetest] started PID {proc.pid}, log {os.path.join(cwd, log_name)}')
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
        download(f"https://meta.fabricmc.net/v2/versions/loader/{MC}/{PROPS['fabric_loader_version']}/{installer}/server/jar",
                 os.path.join(server_dir, 'fabric-server-launch.jar'))
        return ['-jar', 'fabric-server-launch.jar', 'nogui']
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


def server(loader, port, rcon_port):
    os.makedirs(DOWNLOADS, exist_ok=True)
    server_dir = os.path.join(WORK, loader, 'server')
    args = install_server(loader, server_dir)
    set_mods(loader, server_dir)
    servertest.prepare(server_dir, int(port), int(rcon_port))
    with open(os.path.join(server_dir, 'ops.json'), 'w', encoding='utf-8') as f:
        json.dump([{'uuid': offline_uuid(PLAYER), 'name': PLAYER, 'level': 4, 'bypassesPlayerLimit': False}], f)
    return start([java(), MARKER, '-Xmx3G'] + args, server_dir, 'server.log')


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
            vid = f"fabric-loader-{PROPS['fabric_loader_version']}-{MC}"
            url = f"https://meta.fabricmc.net/v2/versions/loader/{MC}/{PROPS['fabric_loader_version']}/profile/json"
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


def client(loader, join_port=None, world=None):
    os.makedirs(DOWNLOADS, exist_ok=True)
    vid = install_client(loader)
    version = resolve(vid)
    game_dir = os.path.join(WORK, loader, 'client')
    os.makedirs(game_dir, exist_ok=True)
    set_mods(loader, game_dir)
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
    args = ['-Xmx4G', MARKER, f'-javaagent:{testclient.build_agent()}'] + expand(version['jvm']) + [version['main']] \
        + expand(version['game']) + extra_game
    lines = []
    for arg in args:
        text = re.sub(r'\$\{(\w+)\}', lambda m: values.get(m.group(1), m.group(0)), arg)
        # In a java @argument file a quoted string takes backslash as an escape character.
        lines.append('"' + text.replace('\\', '\\\\').replace('"', '\\"') + '"')
    argfile = os.path.join(game_dir, 'launch-arguments.txt')
    with open(argfile, 'w', encoding='utf-8', newline='\n') as f:  # no BOM: java would read it as part of an argument
        f.write('\n'.join(lines) + '\n')
    return start([java(), '@' + argfile], game_dir, 'client.log')


def copy_world(loader, name):
    src = os.path.join(WORK, loader, 'server', 'world')
    dst = os.path.join(WORK, loader, 'client', 'saves', name)
    shutil.rmtree(dst, ignore_errors=True)
    shutil.copytree(src, dst, ignore=shutil.ignore_patterns('session.lock'))
    testclient.patch_world(dst)
    print(f'[releasetest] copied {src} to {dst}')


# --- the whole check ----------------------------------------------------------------------------------------------

# Case-sensitive on purpose: "ERROR" as a log level, not inside words like removeErroringEntities, and no mod names
# such as fabric-crash-report-info.
PROBLEMS = re.compile(r'Mixin apply failed|Critical injection failure|InvalidInjectionException|MixinTransformerError'
                      r'|ClassNotFoundException|NoClassDefFoundError|NoSuchMethodError|NoSuchFieldError'
                      r'|Reference map|refmap|Exception|/ERROR\]|/FATAL\]|---- Minecraft Crash Report|crash-reports[/\\]'
                      r'|Preparing crash report|Failed to load|Mod loading has failed|Loading errors encountered'
                      r'|Failed to complete lifecycle event|Missing mods|Incompatible mods|which is missing')
# Seen on every run on this machine and unrelated to the mod: OSHI's process queries, the offline account against
# Mojang's and Realms' services, vanilla's unprimed-heightmap log line, Fabric API's optional Amecs mixin, and netty
# probing its Linux/macOS transports on Windows (Forge/NeoForge log that through log4j's debug appender).
NOISE = re.compile(r'oshi|OSProcess|Perflib|SystemReport|Win32Exception: The parameter is incorrect|Realms|realms'
                   r'|SignedJWT|InvalidCredentialsException|MinecraftClientHttpException|status=401|Status: 401'
                   r'|Failed to fetch user properties|Failed to retrieve profile key pair|Unprimed heightmap'
                   r'|de/siphalor/amecs|minecraft\.client\.MinecraftClient|authlib|CrashReport\.(<init>|preload)'
                   r'|io\.netty\.channel\.(epoll|kqueue)|Only supported on (Linux|OSX/BSD)|AppenderLoggingException')


def scan_log(path, label):
    """Prints the log's problem lines (stack frames under a known-noise line are skipped with it)."""
    if not os.path.exists(path):
        print(f'[check] {label}: no log at {path}')
        return 1
    lines = open(path, encoding='utf-8', errors='replace').read().splitlines()
    problems, noise, in_noise = [], 0, False
    for line in lines:
        frame = line.startswith('\t') or line.startswith('Caused by')
        if frame and in_noise:
            continue
        in_noise = False
        if NOISE.search(line):
            noise += 1
            in_noise = True
        elif PROBLEMS.search(line) and not frame:
            problems.append(line)
    # A mod list line with the version (Fabric, Quilt, NeoForge) or the mod's own boot line (Forge prints no
    # versioned list in latest.log).
    loaded = any(('mocreatures' in l.lower() and PROPS['mod_version'] in l)
                 or re.search(r'initialized with \d+ creatures', l) for l in lines)
    missing = '' if loaded else " - Mo'Creatures NOT seen in the mod list"
    print(f'[check] {label}: {len(lines)} lines, {len(problems)} problem lines, {noise} known-noise lines{missing}')
    for line in problems[:40]:
        print('   ' + line[:300])
    return len(problems)


def retire_log(client_dir):
    """Moves the client's last latest.log away, so the log kept after a session is that session's (a client that dies
    before it starts logging leaves no latest.log, and the copy then fails loudly instead of keeping an old log)."""
    latest = os.path.join(client_dir, 'logs', 'latest.log')
    if os.path.exists(latest):
        os.replace(latest, os.path.join(client_dir, 'logs', 'previous-latest.log'))


def wait_or_kill(proc, seconds, label):
    try:
        return proc.wait(timeout=seconds)
    except subprocess.TimeoutExpired:
        # Our own child (its command line carries MARKER with this project's path), so killing it is safe.
        print(f'[check] {label} did not exit within {seconds}s; killing PID {proc.pid}')
        proc.kill()
        return proc.wait()


def check(loader, port, rcon_port):
    import moctour  # tools/testclient
    loader_dir = os.path.join(WORK, loader)
    server_dir = os.path.join(loader_dir, 'server')
    client_dir = os.path.join(loader_dir, 'client')
    report = os.path.join(loader_dir, 'servertest.txt')
    # A fresh world, so worldgen and every check run with the jar under test.
    shutil.rmtree(os.path.join(server_dir, 'world'), ignore_errors=True)
    srv = server(loader, port, rcon_port)
    fails = servertest.run(int(rcon_port), server_dir, report)
    print(f'[check] content and mechanics check: {"all passed" if not fails else fails}')

    os.makedirs(os.path.join(client_dir, 'logs'), exist_ok=True)
    retire_log(client_dir)
    mp = client(loader, join_port=port)
    # The client checks follow the console log (client.log): Forge sends System.out (the agent's lines) only there,
    # and every loader also writes its game log there.
    client_failures = moctour.run(client_dir, os.path.join(client_dir, 'client.log'), f'{loader}_mp')
    print(f'[check] multiplayer client exited with {wait_or_kill(mp, 180, "multiplayer client")}')
    shutil.copy(os.path.join(client_dir, 'logs', 'latest.log'), os.path.join(loader_dir, 'check-client-mp.log'))
    shutil.copy(os.path.join(client_dir, 'client.log'), os.path.join(loader_dir, 'check-client-mp-console.log'))

    print('[check] stopping the server: ' + servertest.Rcon(int(rcon_port)).cmd('stop').strip())
    print(f'[check] server exited with {wait_or_kill(srv, 180, "server")}')
    shutil.copy(os.path.join(server_dir, 'logs', 'latest.log'), os.path.join(loader_dir, 'check-server.log'))

    copy_world(loader, 'moctest')
    retire_log(client_dir)
    sp = client(loader, world='moctest')
    client_failures += moctour.run(client_dir, os.path.join(client_dir, 'client.log'), f'{loader}_sp')
    print(f'[check] singleplayer client exited with {wait_or_kill(sp, 180, "singleplayer client")}')
    shutil.copy(os.path.join(client_dir, 'logs', 'latest.log'), os.path.join(loader_dir, 'check-client-sp.log'))
    shutil.copy(os.path.join(client_dir, 'client.log'), os.path.join(loader_dir, 'check-client-sp-console.log'))

    print(f'== release check, {loader}: what the logs showed')
    problems = sum(scan_log(os.path.join(loader_dir, f'check-{name}.log'), name)
                   for name in ('server', 'client-mp', 'client-sp'))
    shots_dir = os.path.join(client_dir, 'screenshots')
    shots = sorted(f for f in os.listdir(shots_dir) if f.startswith(loader + '_')) if os.path.isdir(shots_dir) else []
    print(f'[check] {len(shots)} screenshots in {shots_dir}')
    verdict = 'all passed in multiplayer and singleplayer' if not client_failures else \
        f'{client_failures} FAILED (see the [moctour] lines)'
    print(f'[check] client checks: {verdict}')
    return 1 if fails or problems or client_failures else 0


if __name__ == '__main__':
    if len(sys.argv) < 3:
        raise SystemExit(__doc__)
    cmd, loader = sys.argv[1], sys.argv[2]
    # server and client stay attached to the game process until it exits, so a test harness can run them in the
    # background and be told when the game ends.
    if cmd == 'server':
        code = server(loader, sys.argv[3], sys.argv[4]).wait()
        print(f'[releasetest] server exited with code {code}')
    elif cmd == 'client':
        opts = sys.argv[3:]
        code = client(loader, opts[opts.index('--join') + 1] if '--join' in opts else None,
                      opts[opts.index('--world') + 1] if '--world' in opts else None).wait()
        print(f'[releasetest] client exited with code {code}')
    elif cmd == 'copy-world':
        copy_world(loader, sys.argv[3])
    elif cmd == 'check':
        sys.exit(check(loader, sys.argv[3], sys.argv[4]))
    else:
        raise SystemExit(__doc__)
