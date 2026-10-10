"""Scripted Mo'Creatures checks on a minimized test client (the agent's RemoteControl, no input needed).

    py tools/testclient/moctour.py <run dir> <client log> [screenshot prefix]

Waits for the player to join, then on servertest.py's sky platform (x 0-63, z 0-55, floor y 100):
  - horse_hint     right-clicks a wild, unsaddled Mo'Creatures horse with an empty hand and reads the overlay text
                   back: it must say how a MoC horse is tamed (an apple, or a saddle and riding it),
  - vanilla_mount  right-clicks a wild vanilla horse with an empty hand and checks the player is riding it: vanilla
                   horse taming (mount until it stops bucking) still works with the mod installed,
  - scorpion       a wild and a pet scorpion turning to look at the player, screenshots (the carapace must stay on
                   the body; it used to swing off by the look angle),
  - kitty_bed      a hungry tamed kitty in a bed stocked with pet food, screenshot.
Screenshots land in <run dir>/screenshots/. Returns the number of failed checks and prints [moctour] lines.
"""
import os, re, sys, time

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import testclient  # noqa: E402

BAD = re.compile(r'(Exception|---- Minecraft Crash Report|/FATAL\]|Mixin apply failed|Critical injection failure)')


def size(log):
    return os.path.getsize(log) if os.path.exists(log) else 0


def wait_for(log, pattern, timeout, start=0):
    """True once pattern appears in the log after byte offset start; False on timeout or when the game ends."""
    t0 = time.time()
    while time.time() - t0 < timeout:
        if os.path.exists(log):
            with open(log, encoding='utf-8', errors='replace') as f:
                f.seek(start)
                text = f.read()
            if re.search(pattern, text):
                return True
            if re.search(r'---- Minecraft Crash Report|Stopping!', text):
                return False
        time.sleep(1)
    return False


def run(run_dir, log, prefix='client'):
    def send(*lines):
        testclient.send(run_dir, list(lines))

    fails = 0
    if not wait_for(log, r'\[testclient\] player in world', 900):
        print('[moctour] FAIL: the player never appeared in the world')
        return 1
    time.sleep(8)  # let the terrain and the platform's entities arrive
    send('cmd gamemode creative', 'cmd time set noon', 'cmd weather clear', 'cmd clear @s',
         'cmd effect give @s minecraft:night_vision infinite 0 true', 'cmd kill @e[tag=tour]')
    time.sleep(2)

    # --- horse_hint: an empty hand on a wild MoC horse explains how to tame it
    mark = size(log)
    send('cmd tp @s 12.5 101 30.5 -90 10',
         'cmd summon mocreatures:horse 15.5 101 30.5 {TypeMoC:6,Adult:1b,AgeMoC:100,NoAI:1b,PersistenceRequired:1b,'
         'Tags:["tour"]}')
    time.sleep(4)
    # empty-handed: a killed tour mob's drops (also saved into the singleplayer copy of the world) get picked up
    send('cmd kill @e[type=item]', 'cmd clear @s')
    time.sleep(1)
    send('interact mocreatures:horse')
    time.sleep(2)
    send('overlay', f'screenshot {prefix}_horse_hint')
    if wait_for(log, r"\[testclient\] overlay=.*won't let you on bareback - give it an apple", 20, mark):
        print('[moctour] horse_hint: OK (the overlay explains the apple / saddle-and-ride taming)')
    else:
        fails += 1
        print('[moctour] horse_hint: FAIL (no taming hint in the overlay; see the [testclient] overlay= line)')
    send('cmd kill @e[tag=tour]')
    time.sleep(1)

    # --- vanilla_mount: a wild vanilla horse still takes a rider with an empty hand (vanilla taming)
    mark = size(log)
    send('cmd tp @s 12.5 101 30.5 -90 10',
         'cmd summon minecraft:horse 14.5 101 30.5 {NoAI:1b,PersistenceRequired:1b,Tags:["tour"]}')
    time.sleep(4)
    send('cmd kill @e[type=item]', 'cmd clear @s')
    time.sleep(1)
    send('interact minecraft:horse')
    time.sleep(1)
    send('cmd execute as @s on vehicle if entity @s[type=minecraft:horse]')
    if wait_for(log, r'\[CHAT\] Test passed', 15, mark):
        print('[moctour] vanilla_mount: OK (riding the wild vanilla horse, so vanilla mount-to-tame works)')
    else:
        fails += 1
        print('[moctour] vanilla_mount: FAIL (not riding the vanilla horse after the empty-hand click)')
    send('cmd ride @s dismount', 'cmd kill @e[tag=tour]')
    time.sleep(1)

    # --- scorpion: wild and pet scorpions facing south turn their heads to the player standing west of them
    send('cmd tp @s 7.5 102 30.5 -90 20',
         'cmd summon mocreatures:scorpion 11.5 101 29.5 {Rotation:[0f,0f],PersistenceRequired:1b,Tags:["tour"]}',
         'cmd summon mocreatures:pet_scorpion 11.5 101 32.0 {Rotation:[0f,0f],Adult:1b,AgeMoC:100,'
         'PersistenceRequired:1b,Tags:["tour"]}')
    for i in range(3):
        time.sleep(1.5)
        send(f'screenshot {prefix}_scorpion_{i + 1}')
    send('cmd kill @e[tag=tour]')
    time.sleep(1)

    # --- kitty_bed: a hungry tamed kitty climbs into a stocked bed
    send('cmd tp @s 9.5 102 41.5 -90 25',
         'cmd summon mocreatures:kitty_bed 13.5 101 41.5 {HasFood:1b,SheetColour:14,Tags:["tour"]}',
         'cmd summon mocreatures:kitty 13.5 101 44.5 {TypeMoC:2,Tamed:1b,Adult:1b,AgeMoC:100,OwnerName:"Tester",'
         'KittyHunger:5000,PersistenceRequired:1b,Tags:["tour"]}')
    time.sleep(10)
    send(f'screenshot {prefix}_kitty_bed')
    time.sleep(2)
    send('cmd kill @e[tag=tour]')
    time.sleep(1)

    # --- ambient insects: 12.0.5 spawned its insects around players all the time (the ambient pool), not once
    #     at chunk generation. Stand on a sunny plains surface and count the ones that turn up.
    mark = size(log)
    send('cmd execute positioned 0 100 0 run locate biome minecraft:plains')
    if wait_for(log, r'nearest minecraft:plains is at \[(-?\d+), (-?\d+), (-?\d+)\]', 30, mark):
        with open(log, encoding='utf-8', errors='replace') as f:
            f.seek(mark)
            m = re.search(r'nearest minecraft:plains is at \[(-?\d+), (-?\d+), (-?\d+)\]', f.read())
        x, z = int(m.group(1)) + 40, int(m.group(3)) + 40
        # creative, not spectator: spectators do not count for mob spawning; the server test left spawn_mobs off
        send('cmd gamemode creative', 'cmd gamerule spawn_mobs true', 'cmd time set noon',
              'cmd weather clear',
             f'cmd spreadplayers {x} {z} 0 1 false @s', 'cmd moc spawnlist')
        time.sleep(75)
        mark = size(log)
        insects = ['ant', 'bee', 'butterfly', 'crab', 'cricket', 'dragonfly', 'firefly', 'fly', 'maggot', 'roach',
                   'snail']
        send(*[f'cmd execute if entity @e[type=mocreatures:{i},distance=..128]' for i in insects])
        time.sleep(4)
        with open(log, encoding='utf-8', errors='replace') as f:
            f.seek(mark)
            counts = [int(c) for c in re.findall(r'\[CHAT\] Test passed\. Count: (\d+)', f.read())]
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

    with open(log, encoding='utf-8', errors='replace') as f:
        bad = [l for l in f.read().splitlines() if BAD.search(l)]
    print(f'[moctour] {len(bad)} exception/crash lines in the client log so far')
    for line in bad[:20]:
        print('   ' + line[:300])
    send('quit')
    return fails


if __name__ == '__main__':
    if len(sys.argv) < 3:
        raise SystemExit(__doc__)
    sys.exit(1 if run(sys.argv[1], sys.argv[2], sys.argv[3] if len(sys.argv) > 3 else 'client') else 0)
