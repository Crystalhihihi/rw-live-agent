#!/usr/bin/env python3
"""
Rusted Warfare → LLM 战报生成器
读取 rw_units.json，输出 LLM 友好的自然语言战报
"""

import json
import sys
from collections import defaultdict
import os
import math

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
PROJECT_ROOT = os.path.dirname(SCRIPT_DIR)
DATA_DIR = os.path.join(PROJECT_ROOT, "data")

UNIT_NAMES = {}
TYPE_HP_MAP = {}

# 1. Load manual mapping (sandbox-tested entries with HP info)
try:
    mapping_path = os.path.join(DATA_DIR, "unit_mapping.txt")
    with open(mapping_path, 'r', encoding='utf-8') as f:
        for line in f:
            line = line.strip()
            if not line or line.startswith('#'):
                continue
            parts = [p.strip() for p in line.split('|')]
            if len(parts) >= 6:
                cn_name = parts[0]
                type_letter = parts[1]
                hp = parts[3]
                enum_name = parts[5]
                if enum_name:
                    UNIT_NAMES[enum_name] = cn_name
                TYPE_HP_MAP[f"{type_letter}_{hp}"] = cn_name
except Exception as e:
    print(f"Warning: could not load unit_mapping.txt: {e}", file=sys.stderr)

# 2. Load locale names as fallback
try:
    locale_path = os.path.join(DATA_DIR, "locale_names.txt")
    with open(locale_path, 'r', encoding='utf-8') as f:
        for line in f:
            line = line.strip()
            if not line or not line.startswith('units.'):
                continue
            if '=' not in line:
                continue
            key, val = line.split('=', 1)
            key = key.strip().replace('units.', '').replace('.name', '')
            val = val.strip()
            if key and val and key not in UNIT_NAMES:
                UNIT_NAMES[key] = val
except Exception as e:
    print(f"Warning: could not load locale_names.txt: {e}", file=sys.stderr)


def get_unit_name(u):
    """Determine unit Chinese name from JSON data."""
    enum_name = u.get('enum', '')
    if enum_name and enum_name in UNIT_NAMES:
        return UNIT_NAMES[enum_name]

    display_name = u.get('name', '')
    if display_name:
        return display_name

    type_letter = u.get('type', '')
    hp = u.get('hp', 0)
    key = f"{type_letter}_{int(hp)}"
    if key in TYPE_HP_MAP:
        return TYPE_HP_MAP[key]

    return f"未知({type_letter})"


def quadrant_name(x, y, map_w, map_h):
    """Return rough directional quadrant for a point on the map."""
    if map_w <= 0 or map_h <= 0:
        return "未知区域"
    hx, hy = map_w / 2, map_h / 2
    ns = "北" if y < hy else "南"
    ew = "东" if x > hx else "西"
    if abs(x - hx) < hx * 0.3 and abs(y - hy) < hy * 0.3:
        return "中央区域"
    if abs(x - hx) < hx * 0.3:
        return ns + "部"
    if abs(y - hy) < hy * 0.3:
        return ew + "部"
    return ns + ew


def generate_report(json_path=None):
    if json_path is None:
        json_path = os.path.join(PROJECT_ROOT, "rw_units.json")

    with open(json_path, 'r', encoding='utf-8') as f:
        data = json.load(f)

    timestamp = data.get('timestamp', 0)
    map_info = data.get('map', {})
    resources = data.get('resources', [])
    terrain = data.get('terrain', {})
    teams_info = {t['teamId']: t for t in data.get('teams', [])}
    units = data.get('units', [])

    map_w = map_info.get('worldWidth', 0)
    map_h = map_info.get('worldHeight', 0)
    tile_size = map_info.get('tileSize', 20)

    valid_units = [u for u in units if u.get('team', -1) >= 0 and u.get('type') != 'h']
    team_units = defaultdict(list)
    for u in valid_units:
        team_units[u['team']].append(u)

    report = []
    report.append(f"## 铁锈战争实时战报 (Tick: {timestamp})")

    # Map overview
    if map_info:
        w_t = map_info.get('widthTiles', 0)
        h_t = map_info.get('heightTiles', 0)
        report.append(f"地图尺寸: {w_t}x{h_t} 格 ({map_w}x{map_h} 像素)")
    if resources:
        report.append(f"资源点总数: {len(resources)}")
        # Resource distribution by quadrant
        res_quads = defaultdict(int)
        for r in resources:
            q = quadrant_name(r['x'] * tile_size, r['y'] * tile_size, map_w, map_h)
            res_quads[q] += 1
        quad_str = ", ".join([f"{q}:{c}" for q, c in sorted(res_quads.items())])
        report.append(f"资源分布: {quad_str}")
    if terrain and 'mask' in terrain:
        mask = terrain['mask']
        water_cells = sum(row.count('~') for row in mask)
        cliff_cells = sum(row.count('^') for row in mask)
        resource_cells = sum(row.count('*') for row in mask)
        total_cells = len(mask) * len(mask[0]) if mask else 1
        report.append(f"地形概览: 水域{water_cells}/{total_cells} 悬崖{cliff_cells}/{total_cells} 资源{resource_cells}/{total_cells}")
    report.append("")

    # Team overview with resource control
    report.append("### 队伍概况")
    extractor_enum = 'extractor'
    for tid in sorted(teams_info.keys()):
        t = teams_info[tid]
        name = t.get('name', f'队伍{tid}')
        ai_tag = "[AI]" if t.get('isAI') else "[玩家]"
        defeated_tag = "[已战败]" if t.get('isDefeated') else ""
        credits = t.get('credits', 0)
        energy = t.get('energy', 0)
        ucount = t.get('unitCount', 0)
        ai_lvl = t.get('aiLevel', 0)
        ai_base = t.get('aiBaseLevel', 0)
        base_x = t.get('baseX', -1)
        base_y = t.get('baseY', -1)
        base_str = f"基地({base_x:.0f}, {base_y:.0f})" if base_x >= 0 else "基地(已摧毁)"

        # Count extractors for this team
        extractors = [u for u in team_units.get(tid, []) if u.get('enum') == extractor_enum]
        res_controlled = 0
        for ex in extractors:
            ex_x = ex.get('x', 0)
            ex_y = ex.get('y', 0)
            # Check if near any resource point (within 2 tiles = 40px)
            for r in resources:
                rx = r['x'] * tile_size
                ry = r['y'] * tile_size
                if math.hypot(ex_x - rx, ex_y - ry) < 60:
                    res_controlled += 1
                    break

        report.append(f"- 队伍{tid} {name} {ai_tag}{defeated_tag}: 资金={credits:.0f} 能量={energy:.0f} 单位数={ucount} AI等级={ai_lvl} 基地等级={ai_base} {base_str}")
        report.append(f"  采集器{len(extractors)}个, 控制资源点{res_controlled}处")
    report.append("")

    # Unit details per team
    for team_id in sorted(team_units.keys()):
        tinfo = teams_info.get(team_id, {})
        tname = tinfo.get('name', f'队伍{team_id}')
        team_list = team_units[team_id]
        report.append(f"### 队伍 {team_id} ({tname}) — {len(team_list)} 个单位")

        type_counts = defaultdict(int)
        type_hp = defaultdict(lambda: [0, 0])
        for u in team_list:
            name = get_unit_name(u)
            type_counts[name] += 1
            type_hp[name][0] += u.get('hp', 0)
            type_hp[name][1] += u.get('maxHp', 0)

        for name in sorted(type_counts.keys()):
            count = type_counts[name]
            hp_cur, hp_max = type_hp[name]
            hp_pct = (hp_cur / hp_max * 100) if hp_max > 0 else 0
            report.append(f"  - {name}: {count}个, 总HP {hp_cur:.0f}/{hp_max:.0f} ({hp_pct:.0f}%)")

        xs = [u['x'] for u in team_list]
        ys = [u['y'] for u in team_list]
        report.append(f"  活动区域: X({min(xs):.0f}~{max(xs):.0f}), Y({min(ys):.0f}~{max(ys):.0f})")
        report.append("")

    # Terrain ASCII preview
    if terrain and 'mask' in terrain:
        report.append("### 地形简图")
        report.append("```")
        report.append("图例: .陆地 ~水域 ^悬崖 *资源 @障碍")
        report.append("```")
        for row in terrain['mask']:
            report.append(f"    {row}")
        report.append("")

    return '\n'.join(report)


if __name__ == '__main__':
    print(generate_report())
