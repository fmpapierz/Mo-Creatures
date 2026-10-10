"""Headless runtime check of a Mo'Creatures server (dev or production) over RCON.

    py tools/servertest/servertest.py prepare <run dir> <server port> <rcon port>
    py tools/servertest/servertest.py run <rcon port> <run dir> [report file]
    py tools/servertest/servertest.py cmd <rcon port> <command...>
    py tools/servertest/servertest.py stop <rcon port>

`prepare` writes eula.txt (the user accepts the Minecraft EULA for every test server), turns the whitelist off
(white-list and enforce-whitelist, rewritten before every start) and writes a server.properties bound to
127.0.0.1 on the given ports (other sessions run their own servers, so never 25565), with RCON enabled, a fixed seed and
small view distances.

`run` waits for RCON, then on a stone platform in the sky:
  - /summon + /kill for every Mo'Creatures entity type (registration, attributes, constructor crashes),
  - /loot spawn for every Mo'Creatures loot table,
  - mechanics, each with a pass/fail:
      kitty_bed     a hungry tamed kitty walks to a bed stocked with pet food, climbs in, eats until the bed is empty
                    and is then fed (KittyState 4 while eating, back to 0 with its hunger reset; the bed's food gone),
      kitty_night   at midnight, three fed tamed kitties and two empty beds: each bed takes one kitty, the third
                    curls up on the floor; all stay asleep for a minute with no hunger rise; daybreak wakes
                    them and they leave the beds,
      kitty_unreachable  a kitty whose nearest bed is walled off sleeps in a reachable one further away,
      crossbreed    a tamed, pumpkin-fed black MoC horse in love next to a tamed black vanilla horse in love foals a
                    tamed black (TypeMoC 5) MoC foal (12.0.5's TranslateVanillaHorseType),
      lead_step     MoC horse, bear, elephant, ostrich and big cat led by a holder that keeps walking away over a
                    1-block step all climb it (legacy step height 1.0), next to a vanilla horse doing the same;
                    a vanilla cow rides along as a reference only,
      animal_cap    120 wild adult deer (over the old maxAnimals cap of 90) all survive three mob-cap sweeps,
      biome_placement  /moc spawnlist for a dozen biomes holds exactly 12.0.5's species, pools and weights
                    (kitties on plains only, polar bears in snow, crabs on beaches, hell rats in dark forests,
                    fire ogres and nightmares in the Nether, insects in the ambient pool),
      spawn_census  256 never-visited chunks are generated and every animal type in them counted (the restored
                    12.0.5 spawn weights), then no Mo'Creatures animal may vanish over three mob-cap sweeps,
  - finally scans logs/latest.log for errors, exceptions and mixin failures.
"""
import os, re, socket, struct, sys, time

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
RES = os.path.join(ROOT, 'common', 'src', 'main', 'resources')
ENTITIES_JAVA = os.path.join(ROOT, 'common', 'src', 'main', 'java', 'drzhark', 'mocreatures', 'registry', 'MoCEntities.java')
PASSWORD = 'moctest'
# Projectiles and the like: registered, but not meant to stand on a platform; a failed summon is only noted.
NOT_SUMMONABLE_OK = {'mocreatures:throwable_rock', 'mocreatures:thrown_egg', 'mocreatures:thrown_duck_egg', 'mocreatures:rock'}


class Rcon:
    def __init__(self, port, host='127.0.0.1'):
        self.sock = socket.create_connection((host, port), timeout=300)
        self.req = 0
        self._send(3, PASSWORD)
        rid, _, _ = self._recv()
        if rid == -1:
            raise RuntimeError('RCON auth failed')

    def _send(self, kind, payload):
        self.req += 1
        data = struct.pack('<ii', self.req, kind) + payload.encode('utf-8') + b'\x00\x00'
        self.sock.sendall(struct.pack('<i', len(data)) + data)

    def _recvn(self, n):
        buf = b''
        while len(buf) < n:
            chunk = self.sock.recv(n - len(buf))
            if not chunk:
                raise ConnectionError('RCON closed')
            buf += chunk
        return buf

    def _recv(self):
        (length,) = struct.unpack('<i', self._recvn(4))
        body = self._recvn(length)
        rid, kind = struct.unpack('<ii', body[:8])
        return rid, kind, body[8:-2].decode('utf-8', 'replace')

    def cmd(self, command):
        # One packet per TCP read: vanilla's RconClient drops the connection if a single read holds more than one
        # packet, so no trailing marker command. Output longer than 4096 bytes arrives as several packets with the
        # same id; collect them with a short follow-up timeout.
        self._send(2, command)
        rid, _, text = self._recv()
        out = [text]
        self.sock.settimeout(0.4)
        try:
            while True:
                rid2, _, more = self._recv()
                if rid2 != rid:
                    break
                out.append(more)
        except (socket.timeout, TimeoutError, ConnectionError):
            pass  # no more parts; or, after "stop", the server has already closed the connection
        finally:
            self.sock.settimeout(300)
        return ''.join(out)


# A test-only datapack (never part of the mod): moves the lead holders and times the led animals tick by tick, which
# RCON round trips are far too slow for.
DATAPACK = {
    'pack.mcmeta': '{"pack": {"description": "MoCreatures test helpers", "min_format": 107, "max_format": 107}}',
    'data/minecraft/tags/function/tick.json': '{"values": ["moctest:tick"]}',
    # The objectives must exist before the first tick: with "mt" missing, the guard line below errors instead of
    # returning, and the rest of the function (the holders' walk) runs every tick.
    'data/minecraft/tags/function/load.json': '{"values": ["moctest:load"]}',
    'data/moctest/function/load.mcfunction': '\n'.join([
        'scoreboard objectives add mt dummy',
        'scoreboard objectives add mt_y dummy',
        'scoreboard objectives add mt_up dummy',
        'scoreboard players set #drag mt 0',
    ]) + '\n',
    'data/moctest/function/tick.mcfunction': '\n'.join([
        'execute unless score #drag mt matches 1.. run return 0',
        'scoreboard players add #t mt 1',
        # the holders walk east at 0.2 blocks a tick (4 blocks/s, a player's walking pace) up to x 60.5
        'execute as @e[tag=mt_holder] at @s if entity @s[x=-100,dx=160.3] run tp @s ~0.2 ~ ~',
        'execute as @e[tag=mt_led] store result score @s mt_y run data get entity @s Pos[1] 10',
        'execute as @e[tag=mt_led] unless score @s mt_up matches 0.. if score @s mt_y matches 1019.. '
        'run scoreboard players operation @s mt_up = #t mt',
        'execute if score #t mt matches 130.. run scoreboard players set #drag mt 0',
    ]) + '\n',
}


def install_datapack(run_dir):
    base = os.path.join(run_dir, 'world', 'datapacks', 'moctest')
    for rel, text in DATAPACK.items():
        path = os.path.join(base, *rel.split('/'))
        os.makedirs(os.path.dirname(path), exist_ok=True)
        with open(path, 'w', encoding='utf-8', newline='\n') as f:
            f.write(text)


def prepare(run_dir, server_port, rcon_port):
    os.makedirs(run_dir, exist_ok=True)
    install_datapack(run_dir)
    with open(os.path.join(run_dir, 'eula.txt'), 'w', newline='\n') as f:
        f.write('eula=true\n')
    props = {
        'server-ip': '127.0.0.1', 'server-port': str(server_port), 'enable-rcon': 'true',
        'rcon.port': str(rcon_port), 'rcon.password': PASSWORD, 'online-mode': 'false',
        'level-seed': '262100', 'view-distance': '6', 'simulation-distance': '6', 'spawn-protection': '0',
        'max-players': '4', 'motd': "Mo'Creatures 26.2 test server", 'sync-chunk-writes': 'false',
        'enforce-secure-profile': 'false', 'difficulty': 'easy',
        # machine rule: the whitelist off before every start, both keys, whatever an earlier run left
        'white-list': 'false', 'enforce-whitelist': 'false',
        # 26.2 stops ticking an empty server after 60 s ("Server empty for 60 seconds, pausing"), which froze every
        # mechanic checked here with no player online; 0 turns the pause off.
        'pause-when-empty-seconds': '0',
    }
    path = os.path.join(run_dir, 'server.properties')
    lines = open(path, encoding='utf-8').read().splitlines() if os.path.exists(path) else []
    seen = set()
    out = []
    for line in lines:
        k = line.split('=', 1)[0]
        if k in props:
            out.append(f'{k}={props[k]}'); seen.add(k)
        else:
            out.append(line)
    out += [f'{k}={v}' for k, v in props.items() if k not in seen]
    with open(path, 'w', encoding='utf-8', newline='\n') as f:
        f.write('\n'.join(out) + '\n')
    print(f'[servertest] prepared {run_dir}: server {server_port}, rcon {rcon_port}')


def entity_ids():
    src = open(ENTITIES_JAVA, encoding='utf-8').read()
    return sorted({'mocreatures:' + m for m in re.findall(r'register\(\s*"([a-z0-9_]+)"', src)})


def loot_tables():
    base = os.path.join(RES, 'data', 'mocreatures', 'loot_table')
    result = []
    for d, _, fs in os.walk(base):
        for f in fs:
            if f.endswith('.json'):
                rel = os.path.relpath(os.path.join(d, f), base).replace(os.sep, '/')[:-5]
                result.append('mocreatures:' + rel)
    return sorted(result)


def wait_for_rcon(port, timeout=900):
    t0 = time.time()
    while time.time() - t0 < timeout:
        try:
            return Rcon(port)
        except OSError:
            time.sleep(3)
    raise TimeoutError('server never opened RCON')


def nbt_value(r, selector, path):
    """The value of one NBT path of the first entity matching selector, as text, or None."""
    out = r.cmd(f'data get entity {selector} {path}')
    m = re.search(r'has the following entity data: (.+)$', out.strip())
    return m.group(1).strip() if m else None


def nbt_number(r, selector, path):
    v = nbt_value(r, selector, path)
    if v is None:
        return None
    m = re.match(r'(-?[\d.]+)', v)
    return float(m.group(1)) if m else None


def count(r, selector):
    out = r.cmd(f'execute if entity {selector}')
    m = re.search(r'Count: (\d+)', out, re.I)  # "Test passed. Count: 1"
    return int(m.group(1)) if m else 0


def check_kitty_bed(r, log):
    """A hungry tamed kitty eats from a stocked bed (12.0.5 kittyState 3 -> 4 -> fed)."""
    r.cmd('kill @e[tag=mt_kitty]')
    r.cmd('kill @e[tag=mt_bed]')
    r.cmd('summon mocreatures:kitty_bed 6.5 101 6.5 {HasFood:1b,SheetColour:14,Tags:["mt_bed"]}')
    r.cmd('summon mocreatures:kitty 2.5 101 6.5 {TypeMoC:1,Tamed:1b,Adult:1b,AgeMoC:100,OwnerName:"Tester",'
          'KittyHunger:5000,PersistenceRequired:1b,Tags:["mt_kitty"]}')
    kitty = '@e[tag=mt_kitty,limit=1]'
    bed = '@e[tag=mt_bed,limit=1]'
    ate_at = None
    t0 = time.time()
    while time.time() - t0 < 60:
        if nbt_number(r, kitty, 'KittyState') == 4:
            ate_at = time.time() - t0
            break
        time.sleep(1)
    if ate_at is None:
        log(f'[kitty_bed] FAIL: the hungry kitty never climbed into the stocked bed (KittyState '
            f'{nbt_value(r, kitty, "KittyState")}, hunger {nbt_value(r, kitty, "KittyHunger")})')
        return False
    riding = r.cmd(f'execute as {kitty} on vehicle if entity @s[tag=mt_bed]').strip()
    log(f'[kitty_bed] climbed into the bed after {ate_at:.0f}s (eating, KittyState 4; riding the bed: {riding!r})')
    fed_at = None
    t1 = time.time()
    while time.time() - t1 < 70:
        state = nbt_number(r, kitty, 'KittyState')
        food = nbt_value(r, bed, 'HasFood')
        if state == 0 and food == '0b':
            fed_at = time.time() - t1
            break
        time.sleep(1)
    hunger = nbt_number(r, kitty, 'KittyHunger')
    if fed_at is None or hunger is None or hunger > 2000:
        log(f'[kitty_bed] FAIL: not fed after the bed drained (KittyState {nbt_value(r, kitty, "KittyState")}, '
            f'bed HasFood {nbt_value(r, bed, "HasFood")}, hunger {hunger})')
        return False
    log(f'[kitty_bed] OK: bed emptied and the kitty was fed {fed_at:.0f}s later (KittyState 0, hunger {hunger:.0f}, '
        f'HasFood 0b)')
    return True


def check_kitty_night(r, log):
    """At night every tamed, fed kitty turns in and stays asleep until morning: two empty beds for three kitties, so
    each bed takes one kitty and the third curls up where it stands (no two kitties on one bed). The sleepers must
    hold through a minute without getting hungrier, and daybreak must wake them all and get them out of the beds."""
    for tag in ('mt_kitty', 'mt_bed', 'mt_nk', 'mt_nb'):
        r.cmd(f'kill @e[tag={tag}]')
    r.cmd('time set midnight')
    r.cmd('summon mocreatures:kitty_bed 42.5 101 42.5 {SheetColour:1,Tags:["mt_nb","mt_nb1"]}')
    r.cmd('summon mocreatures:kitty_bed 47.5 101 42.5 {SheetColour:5,Tags:["mt_nb","mt_nb2"]}')
    for i, x in enumerate((41.5, 44.5, 47.5)):
        r.cmd(f'summon mocreatures:kitty {x} 101 48.5 {{TypeMoC:{i + 1},Tamed:1b,Adult:1b,AgeMoC:100,'
              f'OwnerName:"Tester",KittyHunger:0,PersistenceRequired:1b,Tags:["mt_nk","mt_nk{i + 1}"]}}')
    kitties = [f'@e[tag=mt_nk{i},limit=1]' for i in (1, 2, 3)]

    def passed(command):
        m = re.search(r'Count: (\d+)', r.cmd(command), re.I)  # "Test passed. Count: 1"; "Test failed" -> 0
        return int(m.group(1)) if m else 0

    def picture():
        """(each kitty's KittyState, how many ride a test bed, how many kitties ride each bed)"""
        states = [nbt_value(r, k, 'KittyState') for k in kitties]
        in_bed = sum(passed(f'execute as {k} on vehicle if entity @s[tag=mt_nb]') for k in kitties)
        per_bed = [passed(f'execute as @e[tag={b},limit=1] on passengers if entity @s[tag=mt_nk]')
                   for b in ('mt_nb1', 'mt_nb2')]
        return states, in_bed, per_bed

    settled_at = None
    t0 = time.time()
    while time.time() - t0 < 150:
        states, in_bed, per_bed = picture()
        if states == ['3', '3', '3'] and in_bed == 2 and per_bed == [1, 1]:
            settled_at = time.time() - t0
            break
        time.sleep(2)
    if settled_at is None:
        log(f'[kitty_night] FAIL: not all settled in 150 s (KittyState {states}, in a bed {in_bed}, '
            f'kitties per bed {per_bed})')
        r.cmd('time set noon')
        return False
    hunger0 = [nbt_number(r, k, 'KittyHunger') for k in kitties]
    log(f'[kitty_night] all three asleep after {settled_at:.0f}s: one in each bed, the third on the floor '
        f'(hunger {hunger0})')
    t1 = time.time()
    while time.time() - t1 < 60:
        states, in_bed, per_bed = picture()
        if states != ['3', '3', '3'] or in_bed != 2 or per_bed != [1, 1]:
            log(f'[kitty_night] FAIL: a sleeper got up after {time.time() - t1:.0f}s (KittyState {states}, '
                f'in a bed {in_bed}, kitties per bed {per_bed})')
            r.cmd('time set noon')
            return False
        time.sleep(3)
    hunger1 = [nbt_number(r, k, 'KittyHunger') for k in kitties]
    if hunger1 != hunger0:
        log(f'[kitty_night] FAIL: hunger rose while asleep ({hunger0} -> {hunger1})')
        r.cmd('time set noon')
        return False
    log(f'[kitty_night] all three stayed asleep for 60s, hunger unchanged {hunger1}')
    r.cmd('time set noon')
    woke_at = None
    t2 = time.time()
    while time.time() - t2 < 20:
        states, in_bed, per_bed = picture()
        if '3' not in states and in_bed == 0:
            woke_at = time.time() - t2
            break
        time.sleep(1)
    if woke_at is None:
        log(f'[kitty_night] FAIL: not awake and out of bed 20 s after daybreak (KittyState {states}, in a bed '
            f'{in_bed})')
        return False
    log(f'[kitty_night] OK: daybreak woke all three and got them out of bed within {woke_at:.0f}s')
    r.cmd('kill @e[tag=mt_nk]')
    r.cmd('kill @e[tag=mt_nb]')
    return True


def check_kitty_unreachable(r, log):
    """A kitty whose nearest bed is walled off (a glass ring it cannot jump) gives that bed up and sleeps in a
    reachable one further away, instead of standing awake all night facing the wall."""
    for tag in ('mt_uk', 'mt_ub'):
        r.cmd(f'kill @e[tag={tag}]')
    r.cmd('fill 19 101 45 21 102 47 minecraft:glass')
    r.cmd('fill 20 101 46 20 102 46 minecraft:air')
    r.cmd('summon mocreatures:kitty_bed 20.5 101 46.5 {SheetColour:2,Tags:["mt_ub","mt_ub_walled"]}')
    r.cmd('summon mocreatures:kitty_bed 30.5 101 52.5 {SheetColour:9,Tags:["mt_ub","mt_ub_open"]}')
    r.cmd('summon mocreatures:kitty 22.5 101 50.5 {TypeMoC:4,Tamed:1b,Adult:1b,AgeMoC:100,OwnerName:"Tester",'
          'KittyHunger:0,PersistenceRequired:1b,Tags:["mt_uk"]}')
    r.cmd('time set midnight')
    kitty = '@e[tag=mt_uk,limit=1]'
    slept_at = None
    t0 = time.time()
    while time.time() - t0 < 150:
        in_open = 'passed' in r.cmd(f'execute as {kitty} on vehicle if entity @s[tag=mt_ub_open]')
        if in_open and nbt_value(r, kitty, 'KittyState') == '3':
            slept_at = time.time() - t0
            break
        time.sleep(2)
    state, pos = nbt_value(r, kitty, 'KittyState'), nbt_value(r, kitty, 'Pos')
    r.cmd('time set noon')
    r.cmd('kill @e[tag=mt_uk]')
    r.cmd('kill @e[tag=mt_ub]')
    r.cmd('fill 19 101 45 21 102 47 minecraft:air')
    if slept_at is None:
        log(f'[kitty_unreachable] FAIL: not asleep in the reachable bed after 150 s (KittyState {state}, at {pos})')
        return False
    log(f'[kitty_unreachable] OK: gave up the walled-off bed and fell asleep in the open one after {slept_at:.0f}s')
    return True


def check_crossbreed(r, log):
    """A MoC horse foals with a vanilla horse (12.0.5 onLivingUpdate + TranslateVanillaHorseType)."""
    r.cmd('kill @e[type=mocreatures:horse,x=10,y=95,z=0,dx=15,dy=15,dz=15]')
    r.cmd('kill @e[tag=mt_vanilla_parent]')
    r.cmd('summon mocreatures:horse 16.5 101 6.5 {TypeMoC:5,Tamed:1b,Adult:1b,AgeMoC:100,EatenPumpkin:1b,InLove:600,'
          'OwnerName:"Tester",PersistenceRequired:1b,Tags:["mt_moc_parent"]}')
    r.cmd('summon minecraft:horse 19.5 101 6.5 {Tame:1b,InLove:600,Variant:4,PersistenceRequired:1b,Tags:["mt_vanilla_parent"]}')
    foal = '@e[type=mocreatures:horse,tag=!mt_moc_parent,limit=1]'
    t0 = time.time()
    while time.time() - t0 < 40:
        if count(r, '@e[type=mocreatures:horse,tag=!mt_moc_parent]') > 0:
            break
        time.sleep(1)
    else:
        log('[crossbreed] FAIL: no foal within 40s (MoC InLove ' + str(nbt_value(r, '@e[tag=mt_moc_parent,limit=1]', 'InLove'))
            + ', vanilla InLove ' + str(nbt_value(r, '@e[tag=mt_vanilla_parent,limit=1]', 'InLove')) + ')')
        return False
    took = time.time() - t0
    coat = nbt_number(r, foal, 'TypeMoC')
    tamed = nbt_value(r, foal, 'Tamed')
    age = nbt_number(r, foal, 'Age')
    vanilla_age = nbt_number(r, '@e[tag=mt_vanilla_parent,limit=1]', 'Age')
    ok = coat == 5 and tamed == '1b' and age is not None and age < 0 and vanilla_age is not None and vanilla_age > 0
    log(f'[crossbreed] {"OK" if ok else "FAIL"}: foal after {took:.0f}s, TypeMoC {coat} (want 5, black x black), '
        f'Tamed {tamed}, Age {age} (a baby), vanilla parent on breeding cooldown Age {vanilla_age}')
    return ok


LEAD_LANES = [  # (label, summon spec, must climb)
    ('moc_horse', 'mocreatures:horse {TypeMoC:6,Tamed:1b,Adult:1b,AgeMoC:100,OwnerName:"Tester"', True),
    ('moc_bear', 'mocreatures:bear {TypeMoC:1,Tamed:1b,Adult:1b,AgeMoC:100,OwnerName:"Tester"', True),
    ('moc_elephant', 'mocreatures:elephant {TypeMoC:1,Tamed:1b,Adult:1b,AgeMoC:100,OwnerName:"Tester"', True),
    ('moc_ostrich', 'mocreatures:ostrich {TypeMoC:2,Tamed:1b,Adult:1b,AgeMoC:100,OwnerName:"Tester"', True),
    ('moc_bigcat', 'mocreatures:big_cat {TypeMoC:1,Tamed:1b,Adult:1b,AgeMoC:100,OwnerName:"Tester"', True),
    ('vanilla_horse', 'minecraft:horse {Tame:1b', True),
    ('vanilla_cow', 'minecraft:cow {', None),
    # the same MoC horse with the step height forced back to the old 0.6: what a led horse did before the fix
    ('moc_horse_step_0.6', 'mocreatures:horse {TypeMoC:6,Tamed:1b,Adult:1b,AgeMoC:100,OwnerName:"Tester",'
     'attributes:[{id:"minecraft:step_height",base:0.6d}]', None),
]


def check_lead_step(r, log):
    """Led animals walking over a 1-block step behind a holder (x 28-45 low, 46+ one block up).

    A walking player keeps the lead stretched: a led animal trails about 6 blocks behind and so meets each step while
    only the lead's spring pulls it, since vanilla's pathing (which hops a mob up a block) runs only in close range.
    Each animal starts 3.5 blocks behind its holder on flat ground, the lead slack (so the time RCON takes to set the
    lanes up changes nothing); the holder walks east at a player's pace (4 blocks/s,
    a test datapack moving it every tick) over the step and on for 6 seconds. An animal that cannot walk up the step
    stays at its face until the lead snaps at 12 blocks."""
    # a lane leashed to an earlier run's holder would snap at once
    r.cmd('kill @e[tag=mt_led]')
    r.cmd('kill @e[tag=mt_holder]')
    for objective in ('mt', 'mt_y', 'mt_up'):
        r.cmd(f'scoreboard objectives add {objective} dummy')
    r.cmd('scoreboard players set #drag mt 0')
    r.cmd('fill 28 101 0 63 106 55 minecraft:air')
    r.cmd('fill 46 101 0 63 101 55 minecraft:stone')
    for i, (label, spec, _) in enumerate(LEAD_LANES):
        z = 3 + i * 6
        # walls between the lanes, so no animal wanders into its neighbour's
        r.cmd(f'fill 28 101 {z + 3} 63 105 {z + 3} minecraft:glass')
        r.cmd(f'summon minecraft:armor_stand 34.0 101 {z}.5 {{NoGravity:1b,Invisible:1b,Tags:["mt_holder","mt_holder_{label}"]}}')
        holder_uuid = nbt_value(r, f'@e[tag=mt_holder_{label},limit=1]', 'UUID')
        etype, nbt = spec.split(' ', 1)  # nbt is an unterminated compound: "{" or "{Key:..."
        sep = '' if nbt == '{' else ','
        out = r.cmd(f'summon {etype} 30.5 101 {z}.5 {nbt}{sep}PersistenceRequired:1b,Rotation:[-90f,0f],'
                    f'Tags:["mt_led","mt_led_{label}"],leash:{{UUID:{holder_uuid}}}}}')
        if 'Summoned' not in out:
            log(f'[lead_step] {label}: summon failed: {out.strip()[:160]}')
    for objective in ('mt', 'mt_y', 'mt_up'):
        r.cmd(f'scoreboard objectives add {objective} dummy')
    r.cmd('scoreboard players reset * mt_up')
    r.cmd('scoreboard players set #t mt 0')
    for label, _, _ in LEAD_LANES:
        sel = f'@e[tag=mt_led_{label},limit=1]'
        log(f'[lead_step] {label}: at the start x {nbt_number(r, sel, "Pos[0]")}, y {nbt_number(r, sel, "Pos[1]")}')
    r.cmd('scoreboard players set #drag mt 1')
    time.sleep(7.5)
    up_at = {}
    for label, _, _ in LEAD_LANES:
        out = r.cmd(f'scoreboard players get @e[tag=mt_led_{label},limit=1] mt_up')
        m = re.search(r'has (\d+) \[mt_up\]', out)
        if m:
            up_at[label] = int(m.group(1)) / 20.0
    ok = True
    for i, (label, _, must) in enumerate(LEAD_LANES):
        sel = f'@e[tag=mt_led_{label},limit=1]'
        y = nbt_number(r, sel, 'Pos[1]')
        px = nbt_number(r, sel, 'Pos[0]')
        leashed = nbt_value(r, sel, 'leash') is not None
        climbed = y is not None and y >= 101.9 and leashed
        verdict = ('reference: ' + ('followed' if climbed else 'stuck')) if must is None else ('OK' if climbed else 'FAIL')
        if must and not climbed:
            ok = False
        when = f'up the step {up_at[label]:.2f}s ({up_at[label] * 20:.0f} ticks) into the drag' if label in up_at else 'never up the step'
        log(f'[lead_step] {label}: {verdict}: {when}; ends at y {y}, x {px:.1f}, still on the lead: {leashed}')
    return ok


MOC_ANIMALS = ['bear', 'big_cat', 'bird', 'boar', 'bunny', 'chimpanzee', 'crocodile', 'deer', 'duck', 'elephant',
               'ent', 'fox', 'goat', 'horse', 'kitty', 'komodo', 'mole', 'mouse', 'ostrich', 'raccoon', 'snake',
               'turkey', 'turtle', 'ant', 'bee', 'butterfly', 'crab', 'cricket', 'dragonfly', 'firefly', 'fly',
               'maggot', 'roach', 'snail']
VANILLA_ANIMALS = ['cow', 'sheep', 'pig', 'chicken', 'horse', 'donkey', 'rabbit', 'fox', 'wolf', 'goat', 'llama',
                   'cat', 'panda', 'parrot', 'polar_bear', 'frog', 'armadillo', 'camel', 'mooshroom', 'ocelot']


def check_spawn_census(r, log):
    """Fresh chunks get Mo'Creatures animals at the restored 12.0.5 weights, and none of them is ever removed.

    Forceloads a never-visited 16x16-chunk area, so chunk generation places its animals; counts every animal type
    there; then waits through three of MoCMobCap's 200-tick sweeps. The port's old animal cap (maxAnimals, 90 per
    dimension, 40 in old configs) discarded the excess farthest-first, so with more than that loaded the count fell.
    """
    # A never-visited plains, so the box is land (the fixed seed's 1024,1024 is ocean). 16x16 chunks around it.
    # a random far-off start, so a rerun on the same world still finds chunks that were never generated
    import random
    sx, sz = random.randrange(4000, 30000, 512), random.randrange(4000, 30000, 512)
    out = r.cmd(f'execute positioned {sx} 100 {sz} run locate biome minecraft:plains')
    m = re.search(r'is at \[(-?\d+), (-?\d+), (-?\d+)\]', out)
    if not m:
        log(f'[spawn_census] FAIL: no plains found: {out.strip()[:160]}')
        return False
    x0 = ((int(m.group(1)) >> 4) - 8) * 16
    z0 = ((int(m.group(3)) >> 4) - 8) * 16
    box = f'x={x0},y=-64,z={z0},dx=255,dy=420,dz=255'

    def total(ids, ns):
        return sum(count(r, f'@e[type={ns}:{i},{box}]') for i in ids)
    # In 26.2 chunk generation places animals only while spawn_mobs is on (NaturalSpawner.
    # spawnMobsForChunkGeneration); the run turns it off to keep the test platform quiet.
    r.cmd('gamerule spawn_mobs true')
    r.cmd(f'forceload add {x0} {z0} {x0 + 255} {z0 + 255}')
    last, stable, t0 = -1, 0, time.time()
    while time.time() - t0 < 240 and stable < 3:  # generation done once the count holds for ~15 s
        time.sleep(5)
        now = total(MOC_ANIMALS, 'mocreatures') + total(VANILLA_ANIMALS, 'minecraft')
        stable = stable + 1 if now == last and now > 0 else 0
        last = now
    r.cmd('gamerule spawn_mobs false')  # from here on only what generation placed is counted
    moc = {i: count(r, f'@e[type=mocreatures:{i},{box}]') for i in MOC_ANIMALS}
    van = {i: count(r, f'@e[type=minecraft:{i},{box}]') for i in VANILLA_ANIMALS}
    moc_total, van_total = sum(moc.values()), sum(van.values())
    log(f'[spawn_census] 256 fresh chunks around the plains at {m.group(1)}, {m.group(3)}: {moc_total} '
        f'Mo\'Creatures animals, {van_total} vanilla animals')
    log('[spawn_census]   Mo\'Creatures: ' + ', '.join(f'{k} {v}' for k, v in sorted(moc.items(), key=lambda kv: -kv[1]) if v))
    log('[spawn_census]   vanilla: ' + ', '.join(f'{k} {v}' for k, v in sorted(van.items(), key=lambda kv: -kv[1]) if v))
    time.sleep(32)  # three MoCMobCap sweeps (every 200 ticks)
    after = total(MOC_ANIMALS, 'mocreatures')
    r.cmd(f'forceload remove {x0} {z0} {x0 + 255} {z0 + 255}')
    ok = moc_total > 0 and after >= moc_total * 0.95 - 2  # a few may die (a fall, a hunting big cat)
    note = ('the old animal cap would have cut them to 90 per dimension' if moc_total > 90
            else 'below the old cap of 90, so this run cannot tell the old code apart')
    log(f'[spawn_census] {"OK" if ok else "FAIL"}: {after} Mo\'Creatures animals left after three cap sweeps '
        f'(were {moc_total}; {note})')
    return ok


def check_animal_cap(r, log):
    """No cap deletes Mo'Creatures animals: 120 wild adult deer (more than the old maxAnimals cap of 90, which
    discarded the excess every 200 ticks) are all still there after three sweeps. NoAI keeps them on the platform."""
    r.cmd('kill @e[tag=mt_cap]')
    r.cmd('kill @e[tag=mt_cap_control]')
    for i in range(12):
        for j in range(10):
            r.cmd(f'summon mocreatures:deer {2.5 + 2 * i} 101 {14.5 + 2 * j} '
                  '{NoAI:1b,Adult:1b,AgeMoC:100,TypeMoC:1,Tags:["mt_cap"]}')
    # Control: monsters keep their cap (maxMobs 70; they respawn), so 100 wild rats must be trimmed. Shows the
    # sweeps really ran and that this check would see a deletion.
    for i in range(10):
        for j in range(10):
            r.cmd(f'summon mocreatures:rat {2.5 + 2 * i} 101 {38.5 + 1.5 * j} {{NoAI:1b,Tags:["mt_cap_control"]}}')
    before = count(r, '@e[tag=mt_cap]')
    rats_before = count(r, '@e[tag=mt_cap_control]')
    time.sleep(32)  # three MoCMobCap sweeps (every 200 ticks)
    after = count(r, '@e[tag=mt_cap]')
    rats_after = count(r, '@e[tag=mt_cap_control]')
    r.cmd('kill @e[tag=mt_cap]')
    r.cmd('kill @e[tag=mt_cap_control]')
    ok = before == 120 and after == 120 and rats_after <= 70 < rats_before
    log(f'[animal_cap] {"OK" if ok else "FAIL"}: {after} of {before} wild adult deer left after three cap sweeps '
        f'(the old animal cap kept at most 90); control: wild rats {rats_before} -> {rats_after} (monster cap 70)')
    return ok


def spawn_list(r, biome):
    """{entity id: (category, weight, min, max)} of the Mo'Creatures entries in a biome's live spawn lists."""
    out = r.cmd(f'moc spawnlist {biome}')
    found = {}
    for m in re.finditer(r'(\w+): (\w+) weight (\d+), packs (\d+)-(\d+)', out):
        found[m.group(2)] = (m.group(1), int(m.group(3)), int(m.group(4)), int(m.group(5)))
    return found, out


# 12.0.5's biome placement and weights, as the live spawn lists must show them: (biome, present {id: (category,
# weight)}, absent ids). Each line is a fact from 12.0.5's MoCEntities lists + Forge 1.12.2's biome types.
PLACEMENT = [
    ('minecraft:plains', {'kitty': ('creature', 8), 'bear': ('creature', 18), 'bird': ('creature', 15),
                          'big_cat': ('creature', 12), 'turkey': ('creature', 8), 'butterfly': ('ambient', 8),
                          'ant': ('ambient', 7), 'horse': ('creature', 8)},
     ['chimpanzee', 'crocodile', 'crab', 'dolphin']),
    ('minecraft:forest', {'bear': ('creature', 18), 'mole': ('creature', 7), 'deer': ('creature', 8),
                          'chimpanzee': ('creature', 8)},
     ['kitty', 'ostrich', 'turkey']),
    ('minecraft:desert', {'ostrich': ('creature', 4), 'elephant': ('creature', 4), 'snake': ('creature', 8),
                          'scorpion': ('monster', 6)},
     ['kitty', 'ant', 'mole', 'raccoon', 'ent', 'bear', 'butterfly']),
    ('minecraft:snowy_plains', {'bear': ('creature', 6), 'big_cat': ('creature', 6), 'bunny': ('creature', 8),
                                'fox': ('creature', 8), 'elephant': ('creature', 4), 'ant': ('ambient', 7),
                                'ogre': ('monster', 19)},  # 1.12's ice plains were WASTELAND, on all 3 ogre lists
     ['ent', 'mole', 'raccoon', 'kitty']),
    ('minecraft:jungle', {'big_cat': ('creature', 24), 'turtle': ('creature', 6), 'chimpanzee': ('creature', 8)},
     ['elephant', 'kitty', 'bear']),
    ('minecraft:beach', {'crab': ('ambient', 8), 'dragonfly': ('ambient', 6), 'dolphin': ('water_creature', 6),
                         'small_fish': ('water_creature', 88), 'fishy': ('water_creature', 12)},
     ['bear', 'kitty', 'horse', 'shark']),
    ('minecraft:river', {'crab': ('ambient', 8), 'shark': ('water_creature', 6), 'ray': ('water_creature', 10),
                         'small_fish': ('water_creature', 88), 'medium_fish': ('water_creature', 30)},
     ['bear', 'kitty']),
    ('minecraft:ocean', {'ray': ('water_creature', 20), 'jellyfish': ('water_creature', 8),
                         'small_fish': ('water_creature', 88), 'dolphin': ('water_creature', 6)},
     ['bear']),
    ('minecraft:swamp', {'crocodile': ('creature', 6), 'komodo': ('creature', 8), 'turtle': ('creature', 6),
                         'ray': ('water_creature', 10), 'fishy': ('water_creature', 12)},
     ['kitty', 'bear']),
    ('minecraft:dark_forest', {'hell_rat': ('monster', 6), 'bear': ('creature', 18)}, []),
    ('minecraft:nether_wastes', {'ogre': ('monster', 6), 'horse_mob': ('monster', 8), 'hell_rat': ('monster', 6),
                                 'flame_wraith': ('monster', 5), 'scorpion': ('monster', 6),
                                 'manticore': ('monster', 8)},
     ['werewolf', 'bear']),
    ('minecraft:mushroom_fields', {}, ['bear', 'kitty', 'ogre', 'butterfly', 'werewolf']),
]


def check_biome_placement(r, log):
    """Every biome's live spawn lists hold exactly 12.0.5's species and weights (the /moc spawnlist command)."""
    ok = True
    for biome, present, absent in PLACEMENT:
        found, out = spawn_list(r, biome)
        bad = []
        for i, (cat, weight) in present.items():
            got = found.get(i)
            if got is None or got[0] != cat or got[1] != weight:
                bad.append(f'{i} want {cat} {weight}, got {got[0] + " " + str(got[1]) if got else "nothing"}')
        bad += [f'{i} should not spawn here' for i in absent if i in found]
        types = re.search(r'1\.12 types: ([^)]*)', out)
        log(f'[biome_placement] {biome} ({types.group(1) if types else "?"}): {"OK" if not bad else "FAIL"}, '
            f'{len(found)} Mo\'Creatures entries' + ('' if not bad else ': ' + '; '.join(bad)))
        ok = ok and not bad
    return ok


MECHANICS = (('kitty_bed', check_kitty_bed), ('kitty_night', check_kitty_night),
             ('kitty_unreachable', check_kitty_unreachable),
             ('crossbreed', check_crossbreed), ('lead_step', check_lead_step),
             ('animal_cap', check_animal_cap), ('biome_placement', check_biome_placement),
             ('spawn_census', check_spawn_census))


def platform(r):
    """Calm weather and time, no stray mobs, and the stone platform in the sky (x 0-63, z 0-55, floor y 100) that
    every check here and the client tour (tools/testclient/moctour.py) stand on."""
    r.cmd('gamerule spawn_mobs false')
    r.cmd('gamerule advance_time false')
    r.cmd('time set noon')
    r.cmd('weather clear')
    r.cmd('kill @e[type=!minecraft:player]')  # nothing left over from an earlier run on this world
    # a stone platform in the sky, away from spawn's terrain
    r.cmd('forceload add 0 0 63 55')
    # Cleared in two slabs: one fill may change at most 32768 blocks (gamerule max_block_modifications), and a single
    # 64 x 15 x 56 fill was refused, which left the walls and the step of an earlier lead_step run standing on a
    # reused world.
    for y0, y1 in ((101, 107), (108, 115)):
        out = r.cmd(f'fill 0 {y0} 0 63 {y1} 55 minecraft:air')
        if 'Too many blocks' in out:
            raise RuntimeError(f'platform not cleared: {out.strip()}')
    r.cmd('fill 0 100 0 63 100 55 minecraft:stone')


def run(port, run_dir, report=None):
    r = wait_for_rcon(port)
    lines = []

    def log(s):
        print(s, flush=True); lines.append(s)
    log('== server up: ' + r.cmd('list'))
    platform(r)
    fails = {}
    # entities
    for e in entity_ids():
        out = r.cmd(f'summon {e} 8 101 40')
        ok = 'Summoned' in out
        note = ''
        if not ok and e in NOT_SUMMONABLE_OK:
            note = ' (not a standing entity; noted only)'
        log(f'[entity] {e}: {"OK" if ok else ("note" if note else "FAIL")} {out.strip()[:140]}{note}')
        if not ok and not note:
            fails.setdefault('entity', []).append(e)
        r.cmd(f'kill @e[type={e}]')
        r.cmd('kill @e[type=item]')
        r.cmd('kill @e[type=experience_orb]')
    # loot tables
    tables = loot_tables()
    for t in tables:
        out = r.cmd(f'loot spawn 8 101 40 loot {t}')
        bad = 'Unknown' in out or 'Invalid' in out or 'error' in out.lower()
        if bad:
            log(f'[loot] {t}: FAIL {out.strip()[:160]}')
            fails.setdefault('loot', []).append(t)
        r.cmd('kill @e[type=item]')
    log(f'[loot] checked {len(tables)} tables')
    # mechanics
    for name, check in MECHANICS:
        try:
            if not check(r, log):
                fails.setdefault('mechanics', []).append(name)
        except Exception as ex:  # a broken check is a failed check, never a crashed run
            log(f'[{name}] FAIL: {ex!r}')
            fails.setdefault('mechanics', []).append(name)
    # leave no test mob behind: a copy of this world is what the singleplayer client test loads
    r.cmd('kill @e[type=!minecraft:player]')
    r.cmd('save-all flush')
    log('== summary: ' + ', '.join(f'{k}: {v}' for k, v in fails.items()) if fails else '== summary: all checks passed')
    latest = os.path.join(run_dir, 'logs', 'latest.log')
    if os.path.exists(latest):
        text = open(latest, encoding='utf-8', errors='replace').read().splitlines()
        pat = re.compile(r'(/ERROR\]|Exception|Mixin apply failed|Critical injection failure|Failed to parse|Couldn\'t parse)')
        hits = [l for l in text if pat.search(l)]
        log(f'== latest.log: {len(hits)} error-like lines')
        for h in hits[:80]:
            log('   ' + h[:300])
    if report:
        with open(report, 'w', encoding='utf-8') as f:
            f.write('\n'.join(lines) + '\n')
    return fails


if __name__ == '__main__':
    if len(sys.argv) < 3:
        raise SystemExit(__doc__)
    if sys.argv[1] == 'prepare':
        prepare(sys.argv[2], int(sys.argv[3]), int(sys.argv[4]))
    elif sys.argv[1] == 'run':
        sys.exit(1 if run(int(sys.argv[2]), sys.argv[3], sys.argv[4] if len(sys.argv) > 4 else None) else 0)
    elif sys.argv[1] == 'mechanic':  # one mechanic against a running server: mechanic <rcon port> <name>
        r = Rcon(int(sys.argv[2]))
        platform(r)
        sys.exit(0 if dict(MECHANICS)[sys.argv[3]](r, print) else 1)
    elif sys.argv[1] == 'cmd':
        print(Rcon(int(sys.argv[2])).cmd(' '.join(sys.argv[3:])))
    elif sys.argv[1] == 'stop':
        print(Rcon(int(sys.argv[2])).cmd('stop'))
    else:
        raise SystemExit(__doc__)
