"""Scripted Mo'Creatures checks on a minimized test client (the agent's RemoteControl, no input needed).

    py tools/testclient/moctour.py <run dir> <client log> [screenshot prefix]

Waits for the player to join, then on servertest.py's sky platform (x 0-63, z 0-55, floor y 100):
  - horse_hint     right-clicks a wild, unsaddled Mo'Creatures horse with an empty hand and reads the overlay text
                   back: it must say how a MoC horse is tamed (an apple, or a saddle and riding it),
  - vanilla_mount  right-clicks a wild vanilla horse with an empty hand and checks the player is riding it: vanilla
                   horse taming (mount until it stops bucking) still works with the mod installed,
  - horse_no_rear  rides a saddled wild MoC horse while it bucks and while it is hurt, reading its rearing flag
                   twice a second: a ridden horse must never rear (12.0.5's stand() refused a ridden horse; the
                   port's rearing swung the head up through the rider). Hurting it is what made an unridden horse
                   rear one time in three, so a regression shows here,
  - scorpion       a wild and a pet scorpion turning to look at the player, screenshots (the carapace must stay on
                   the body; it used to swing off by the look angle),
  - kitty_bowl     a real right click with a milk bucket, the crosshair on the kitty bed's food bowl (which lies
                   outside the bed's hitbox), must stock the bed and hand back an empty bucket,
  - kitty_bed      a hungry tamed kitty in a bed stocked with pet food, screenshot,
  - lair_sky       the Wyvern Lair and its sky (the mod's own sky renderer), screenshot,
  - ambient_insects  insects spawn around the player on a sunny plains (12.0.5's ambient pool).
Screenshots land in <run dir>/screenshots/. Returns the number of failed checks and prints [moctour] lines.

Every wait and pause watches for a failed or stuck launch (machine rules): a loader or crash error in the log or an
error window (launchwatch.check with the windowguard log), a disconnect or error screen (the agent's "ERROR SCREEN"
line), a client that stopped making progress (launchwatch.stall_check, with a thread dump), a login still on the
connect screen after 20 s (thread dumps of the client AND the server), and the server log's "lost connection" /
"Took too long to log in" / "not white-listed". Any of these ends the tour at once with the reason (LaunchFailure).
"""
import os, re, sys, time

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import testclient  # noqa: E402
import launchwatch  # noqa: E402

BAD = re.compile(r'(Exception|---- Minecraft Crash Report|/FATAL\]|Mixin apply failed|Critical injection failure)')
SERVER_JOIN_FAILURE = re.compile(r'lost connection:|Took too long to log in|You are not white-listed')
CONNECT_STALL_SECONDS = 20
INSECTS = ['ant', 'bee', 'butterfly', 'crab', 'cricket', 'dragonfly', 'firefly', 'fly', 'maggot', 'roach', 'snail']


class LaunchFailure(RuntimeError):
    pass


class Watch:
    """What every wait checks besides its own pattern; see the module doc. guard_log is windowguard.ps1's log, the
    PIDs are for thread dumps, server_log (optional) is read from where it stood when the watch was made."""

    def __init__(self, guard_log=None, client_pid=None, server_pid=None, server_log=None, dump_prefix='moctour'):
        self.guard_log, self.client_pid, self.server_pid = guard_log, client_pid, server_pid
        self.server_log, self.dump_prefix = server_log, dump_prefix
        self.server_mark = size(server_log) if server_log else 0

    def dumps(self, why):
        return [launchwatch.thread_dump(pid, f'{self.dump_prefix}-{why}-{label}-{pid}.txt')
                for label, pid in (('client', self.client_pid), ('server', self.server_pid)) if pid]

    def check(self, text):
        failure = launchwatch.check(text, self.guard_log)
        if not failure and self.server_log:
            m = SERVER_JOIN_FAILURE.search(read_from(self.server_log, self.server_mark))
            if m:
                failure = f'the server refused or dropped the client: {m.group(0)}'
        if not failure:
            beats = launchwatch.HEARTBEAT.findall(text)
            if beats and beats[-1][4].endswith('ConnectScreen'):
                since = int(beats[-1][0])
                for b in reversed(beats):
                    if not b[4].endswith('ConnectScreen'):
                        break
                    since = int(b[0])
                if int(beats[-1][0]) - since >= CONNECT_STALL_SECONDS:
                    failure = (f'login stalled: still on the connect screen after {int(beats[-1][0]) - since} s; '
                               f'thread dumps: {self.dumps("connect-stall")}')
        if not failure:
            stall = launchwatch.stall_check(text)
            if stall:
                failure = f'{stall}; thread dumps: {self.dumps("stall")}'
        if failure:
            raise LaunchFailure(failure)


def size(log):
    return os.path.getsize(log) if log and os.path.exists(log) else 0


def read_from(log, start=0):
    if not log or not os.path.exists(log):
        return ''
    with open(log, encoding='utf-8', errors='replace') as f:
        f.seek(start)
        return f.read()


def wait_for(log, pattern, timeout, start=0, watch=None):
    """True once pattern appears in the log after byte offset start; False on timeout or when the game ends. Raises
    LaunchFailure as soon as the watch sees a failed or stuck launch."""
    t0 = time.time()
    while time.time() - t0 < timeout:
        text = read_from(log, start)
        if re.search(pattern, text):
            return True
        if watch is not None:
            watch.check(read_from(log))
        if re.search(r'---- Minecraft Crash Report|Stopping!', text):
            return False
        time.sleep(1)
    return False


def run(run_dir, log, prefix='client', watch=None, insects=True):
    def send(*lines):
        testclient.send(run_dir, list(lines))

    def pause(seconds):
        """A sleep that keeps watching, so a failure mid-check is reported at once, not at the next wait."""
        end = time.time() + seconds
        while True:
            if watch is not None:
                watch.check(read_from(log))
            left = end - time.time()
            if left <= 0:
                return
            time.sleep(min(1.0, left))

    def chat_after(mark, pattern, timeout=15):
        return wait_for(log, r'\[CHAT\] ' + pattern, timeout, mark, watch)

    fails = 0
    if not wait_for(log, r'\[testclient\] player in world', 900, watch=watch):
        print('[moctour] FAIL: the player never appeared in the world')
        return 1
    pause(8)  # let the terrain and the platform's entities arrive
    send('cmd gamemode creative', 'cmd time set noon', 'cmd weather clear', 'cmd clear @s',
         'cmd effect give @s minecraft:night_vision infinite 0 true', 'cmd kill @e[tag=tour]')
    pause(2)

    # --- horse_hint: an empty hand on a wild MoC horse explains how to tame it
    mark = size(log)
    send('cmd tp @s 12.5 101 30.5 -90 10',
         'cmd summon mocreatures:horse 15.5 101 30.5 {TypeMoC:6,Adult:1b,AgeMoC:100,NoAI:1b,PersistenceRequired:1b,'
         'Tags:["tour"]}')
    pause(4)
    # empty-handed: a killed tour mob's drops (also saved into the singleplayer copy of the world) get picked up
    send('cmd kill @e[type=item]', 'cmd clear @s')
    pause(1)
    send('interact mocreatures:horse')
    pause(2)
    send('overlay', f'screenshot {prefix}_horse_hint')
    if wait_for(log, r"\[testclient\] overlay=.*won't let you on bareback - give it an apple", 20, mark, watch):
        print('[moctour] horse_hint: OK (the overlay explains the apple / saddle-and-ride taming)')
    else:
        fails += 1
        print('[moctour] horse_hint: FAIL (no taming hint in the overlay; see the [testclient] overlay= line)')
    send('cmd kill @e[tag=tour]')
    pause(1)

    # --- vanilla_mount: a wild vanilla horse still takes a rider with an empty hand (vanilla taming)
    mark = size(log)
    send('cmd tp @s 12.5 101 30.5 -90 10',
         'cmd summon minecraft:horse 14.5 101 30.5 {NoAI:1b,PersistenceRequired:1b,Tags:["tour"]}')
    pause(4)
    send('cmd kill @e[type=item]', 'cmd clear @s')
    pause(1)
    send('interact minecraft:horse')
    pause(1)
    send('cmd execute as @s on vehicle if entity @s[type=minecraft:horse]')
    if chat_after(mark, r'Test passed'):
        print('[moctour] vanilla_mount: OK (riding the wild vanilla horse, so vanilla mount-to-tame works)')
    else:
        fails += 1
        print('[moctour] vanilla_mount: FAIL (not riding the vanilla horse after the empty-hand click)')
    send('cmd ride @s dismount', 'cmd kill @e[tag=tour]')
    pause(1)

    # --- horse_no_rear: ride a saddled wild MoC horse (it bucks and throws you; climb back on) and hurt it now and
    #     then, reading its synced rearing flag on the client twice a second
    send('cmd tp @s 12.5 101 30.5 -90 10',
         'cmd summon mocreatures:horse 14.5 101 30.5 {TypeMoC:2,Adult:1b,AgeMoC:100,Saddled:1b,'
         'PersistenceRequired:1b,Tags:["tour"]}')
    pause(3)
    send('cmd kill @e[type=item]', 'cmd clear @s')
    pause(1)
    ridden = rearing = hurts = 0
    shot = 0
    end = time.time() + 30
    next_hurt = time.time() + 2
    while time.time() < end:
        mark = size(log)
        send('vehicle getRearing')
        pause(0.5)
        got = re.findall(r'\[testclient\] vehicle (?:getRearing=(true|false)|(none))', read_from(log, mark))
        if not got:
            continue
        if got[-1][1] == 'none':
            # thrown off (or not on yet): climb back on
            send('cmd tp @s @e[type=mocreatures:horse,tag=tour,limit=1]', 'interact mocreatures:horse')
            pause(1)
            continue
        ridden += 1
        rearing += got[-1][0] == 'true'
        if time.time() >= next_hurt:
            send('cmd damage @e[type=mocreatures:horse,tag=tour,limit=1] 0.5 minecraft:generic')
            hurts += 1
            next_hurt = time.time() + 2
        if shot < 2 and ridden >= (6, 20)[shot]:
            # third person, so the horse's head and the rider are both in the picture
            send('camera THIRD_PERSON_BACK')
            pause(0.5)
            send(f'screenshot {prefix}_horse_ride_{shot + 1}', 'camera FIRST_PERSON')
            shot += 1
    # a horse broken in by riding opens "Name your pet": close it, or it swallows every later click
    send('cmd ride @s dismount', 'cmd kill @e[tag=tour]', 'camera FIRST_PERSON', 'closescreen')
    if ridden >= 15 and hurts >= 5 and rearing == 0:
        print(f'[moctour] horse_no_rear: OK (never reared in {ridden} ridden samples, {hurts} hurts while ridden)')
    else:
        fails += 1
        print(f'[moctour] horse_no_rear: FAIL ({rearing} of {ridden} ridden samples showed the horse rearing; '
              f'{hurts} hurts)')
    pause(1)

    send('closescreen')

    # --- scorpion: wild and pet scorpions facing south turn their heads to the player standing west of them
    send('cmd tp @s 7.5 102 30.5 -90 20',
         'cmd summon mocreatures:scorpion 11.5 101 29.5 {Rotation:[0f,0f],PersistenceRequired:1b,Tags:["tour"]}',
         'cmd summon mocreatures:pet_scorpion 11.5 101 32.0 {Rotation:[0f,0f],Adult:1b,AgeMoC:100,'
         'PersistenceRequired:1b,Tags:["tour"]}')
    for i in range(3):
        pause(1.5)
        send(f'screenshot {prefix}_scorpion_{i + 1}')
    send('cmd kill @e[tag=tour]')
    pause(1)

    # --- kitty_bowl: a real right click on the food bowl, which lies outside the bed's hitbox. A bed facing south
    #     (yaw 0) has its bowl 0.83 west and 0.16 north of its centre (MoCEntityKittyBed.BOWL); the player stands
    #     north of it looking down at the bowl, so the bed's hitbox is not under the crosshair.
    mark = size(log)
    send('cmd summon mocreatures:kitty_bed 13.5 101 41.5 {Rotation:[0f,0f],SheetColour:3,Tags:["tour"]}')
    pause(2)
    send('cmd kill @e[type=item]', 'cmd clear @s', 'cmd give @s minecraft:milk_bucket',
         # yaw 0 (south), pitch 33 down: from the eyes (y 102.62) that ray meets the bowl's rim at z 41.1. Not
         # `tp ... facing <point>`: that aims from the FEET, so the eyes looked level, over the bowl.
         'cmd tp @s 12.67 101 39.0 0 33')
    pause(2)
    # and set the same look on the client itself, so the aim never depends on the tp's rotation arriving first (the
    # "was" in the look line shows what the tp left; it matched on 2026-10-10)
    send('closescreen', 'look 0 33')
    pause(1)
    send('use')
    pause(2)
    send('cmd execute if entity @e[type=mocreatures:kitty_bed,tag=tour,nbt={HasMilk:1b}]')
    milk = chat_after(mark, r'Test passed')
    mark2 = size(log)
    send('cmd clear @s minecraft:bucket 0', f'screenshot {prefix}_kitty_bowl')
    bucket = chat_after(mark2, r'Found 1 matching item')
    if milk and bucket:
        print('[moctour] kitty_bowl: OK (a click on the food bowl with a milk bucket stocked the bed and gave the '
              'bucket back)')
    else:
        fails += 1
        print(f'[moctour] kitty_bowl: FAIL (bed stocked: {milk}, empty bucket back: {bucket}; see the '
              f'"[testclient] used item" line)')
    send('cmd kill @e[tag=tour]', 'cmd clear @s')
    pause(1)

    # --- kitty_bed: a hungry tamed kitty climbs into a stocked bed
    send('cmd tp @s 9.5 102 41.5 -90 25',
         'cmd summon mocreatures:kitty_bed 13.5 101 41.5 {HasFood:1b,SheetColour:14,Tags:["tour"]}',
         'cmd summon mocreatures:kitty 13.5 101 44.5 {TypeMoC:2,Tamed:1b,Adult:1b,AgeMoC:100,OwnerName:"Tester",'
         'KittyHunger:5000,PersistenceRequired:1b,Tags:["tour"]}')
    pause(10)
    send(f'screenshot {prefix}_kitty_bed')
    pause(2)
    send('cmd kill @e[tag=tour]')
    pause(1)

    # --- lair_sky: the Wyvern Lair and its sky (the mod's own sky renderer)
    # onto the top of the Lair's terrain (a fixed height lands inside an island), then look up into its sky
    send('cmd execute in mocreatures:wyvern_lair run spreadplayers 0 0 0 64 false @s')
    pause(8)
    send('closescreen', 'cmd tp @s ~ ~ ~ 30 -25')
    pause(3)
    send(f'screenshot {prefix}_lair_sky')
    pause(2)
    send('cmd execute in minecraft:overworld run tp @s 12.5 101 30.5')
    pause(4)

    # --- ambient insects: 12.0.5 spawned its insects around players all the time (the ambient pool), not once
    #     at chunk generation. Stand on a sunny plains surface and count the ones that turn up.
    if insects:
        mark = size(log)
        send('cmd execute positioned 0 100 0 run locate biome minecraft:plains')
        found = r'nearest minecraft:plains is at \[(-?\d+), (-?\d+), (-?\d+)\]'
        if wait_for(log, found, 30, mark, watch):
            m = re.search(found, read_from(log, mark))
            x, z = int(m.group(1)) + 40, int(m.group(3)) + 40
            # creative, not spectator: spectators do not count for mob spawning; the server test left spawn_mobs off
            send('cmd gamemode creative', 'cmd gamerule spawn_mobs true', 'cmd time set noon', 'cmd weather clear',
                 f'cmd spreadplayers {x} {z} 0 1 false @s', 'cmd moc spawnlist')
            pause(75)
            mark = size(log)
            send(*[f'cmd execute if entity @e[type=mocreatures:{i},distance=..128]' for i in INSECTS])
            pause(4)
            counts = [int(c) for c in re.findall(r'\[CHAT\] Test passed\. Count: (\d+)', read_from(log, mark))]
            send(f'screenshot {prefix}_insects')
            if sum(counts) > 0:
                print(f'[moctour] ambient_insects: OK ({sum(counts)} Mo\'Creatures insects spawned around the player '
                      f'on a plains in 75 s)')
            else:
                fails += 1
                print('[moctour] ambient_insects: FAIL (no Mo\'Creatures insect spawned around the player in 75 s)')
        else:
            fails += 1
            print('[moctour] ambient_insects: FAIL (no plains found)')

    bad = [line for line in read_from(log).splitlines() if BAD.search(line)]
    print(f'[moctour] {len(bad)} exception/crash lines in the client log so far')
    for line in bad[:20]:
        print('   ' + line[:300])
    send('quit')
    return fails


if __name__ == '__main__':
    if len(sys.argv) < 3:
        raise SystemExit(__doc__)
    try:
        sys.exit(1 if run(sys.argv[1], sys.argv[2], sys.argv[3] if len(sys.argv) > 3 else 'client') else 0)
    except LaunchFailure as e:
        print(f'[moctour] LAUNCH FAILURE: {e}')
        sys.exit(2)
