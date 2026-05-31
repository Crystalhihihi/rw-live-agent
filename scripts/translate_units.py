#!/usr/bin/env python3
"""Read rw_units.json and add Chinese names using locale file."""

import json
import re

# Load English -> Chinese mapping from game locale
locale_path = r"D:\SteamLibrary\steamapps\common\Rusted Warfare\assets\translations\Strings_zh_cn.properties"
name_map = {}
with open(locale_path, 'r', encoding='utf-8') as f:
    for line in f:
        m = re.match(r'units\.([a-zA-Z0-9_]+)\.name\s*=\s*(.+)', line.strip())
        if m:
            en_name = m.group(1)
            cn_name = m.group(2).strip()
            name_map[en_name] = cn_name

# Read current units data
with open('rw_units.json', 'r', encoding='utf-8') as f:
    data = json.load(f)

units = data.get('units', [])
print(f"Total units: {len(units)}\n")

for u in units:
    team = u.get('team', -1)
    if team < 0:
        continue
    t = u.get('type', '?')
    tid = u.get('typeId', -1)
    x = u.get('x', 0)
    y = u.get('y', 0)
    hp = u.get('hp', 0)
    maxhp = u.get('maxHp', 0)
    # For now we don't have English name in JSON yet
    # This script will be fully functional after Agent update
    print(f"[{t:>2s} id={tid:2d}] team={team} ({x:7.1f},{y:7.1f}) hp={hp:6.0f}/{maxhp:6.0f}")

print("\n--- Known mappings from manual recording ---")
print("j id=7  -> 轻型武装直升机")
print("c id=19 -> 机枪艇")
print("j id=24 -> 坦克")
print("g id=25 -> 悬浮坦克")
print("j id=20 -> 水下探测器")
print("b id=26 -> 建造者")
