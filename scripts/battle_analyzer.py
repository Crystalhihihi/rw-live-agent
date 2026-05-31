#!/usr/bin/env python3
"""
Battle Analyzer — 将原始游戏数据翻译为 LLM 友好的「参谋语言」
输入: rw_units.json（原始数据）
输出: rw_llm.json（拓扑战略图 + 兵力态势 + 经济健康度）
"""

import json
import os
from collections import defaultdict, deque
from typing import List, Dict, Tuple, Any

# ============================================================
# 1. 地形拓扑分析
# ============================================================

def analyze_topology(mask: List[str], step: int = 3) -> List[Dict]:
    """
    从 50x50 字符掩码中提取战略区域拓扑。
    '.' = 可通行, 'X' = 不可通行
    返回: zones 列表，每个 zone 有 id, type, center, connects_to, width
    """
    h = len(mask)
    w = len(mask[0]) if h > 0 else 0
    if h == 0 or w == 0:
        return []

    # --- 1.1 距离变换：每个可通行格到最近障碍的距离 ---
    dist = [[-1] * w for _ in range(h)]
    q = deque()
    for y in range(h):
        for x in range(w):
            if mask[y][x] == 'X':
                dist[y][x] = 0
                q.append((x, y))

    while q:
        cx, cy = q.popleft()
        for dx, dy in [(-1, 0), (1, 0), (0, -1), (0, 1)]:
            nx, ny = cx + dx, cy + dy
            if 0 <= nx < w and 0 <= ny < h and dist[ny][nx] == -1:
                dist[ny][nx] = dist[cy][cx] + 1
                q.append((nx, ny))

    # --- 1.2 识别所有可通行区域（不做 choke point 细分）---
    passable = set()
    for y in range(h):
        for x in range(w):
            if mask[y][x] == '.':
                passable.add((x, y))

    # --- 1.3 连通域分析 ---
    def find_components(cells: set, min_size: int = 10) -> List[List[Tuple[int, int]]]:
        visited = set()
        comps = []
        for x, y in cells:
            if (x, y) not in visited:
                comp = []
                q = deque([(x, y)])
                visited.add((x, y))
                while q:
                    cx, cy = q.popleft()
                    comp.append((cx, cy))
                    for dx, dy in [(-1, 0), (1, 0), (0, -1), (0, 1)]:
                        nx, ny = cx + dx, cy + dy
                        if (nx, ny) in cells and (nx, ny) not in visited:
                            visited.add((nx, ny))
                            q.append((nx, ny))
                if len(comp) >= min_size:
                    comps.append(comp)
        return comps

    zones_raw = find_components(passable, min_size=15)

    # --- 1.4 区域命名（基于中心位置）---
    def name_zone(cells: List[Tuple[int, int]], existing: set) -> str:
        if not cells:
            return "unknown"
        avg_x = sum(x for x, y in cells) / len(cells)
        avg_y = sum(y for x, y in cells) / len(cells)

        ns = ""
        ew = ""
        if avg_y < h * 0.25:
            ns = "north"
        elif avg_y > h * 0.75:
            ns = "south"

        if avg_x < w * 0.25:
            ew = "west"
        elif avg_x > w * 0.75:
            ew = "east"

        if ns and ew:
            name = f"{ns}_{ew}"
        elif ns:
            name = ns
        elif ew:
            name = ew
        else:
            name = "center"

        base = name
        suffix = 1
        while name in existing:
            name = f"{base}_{suffix}"
            suffix += 1
        return name

    zones = []
    existing_names = set()

    # 按面积排序，只保留前 6 个最大的区域
    zones_raw = sorted(zones_raw, key=len, reverse=True)[:6]
    for comp in zones_raw:
        name = name_zone(comp, existing_names)
        existing_names.add(name)
        zones.append({
            "id": name,
            "type": "main_zone",
            "cell_count": len(comp),
            "center": [round(sum(x for x, y in comp) / len(comp), 1),
                       round(sum(y for x, y in comp) / len(comp), 1)],
            "connects_to": [],
            "risk_level": "low"
        })

    return zones, {}


# ============================================================
# 2. 资源点战略归属
# ============================================================

def assign_resources(resources: List[Dict], zones: List[Dict],
                     mask: List[str], step: int = 3, tile_size: int = 20) -> List[Dict]:
    """把裸坐标资源点翻译成战略归属。"""
    h = len(mask)
    w = len(mask[0]) if h > 0 else 0
    result = []

    for i, r in enumerate(resources):
        rx, ry = r.get("x", 0), r.get("y", 0)
        gx = min(rx // step, w - 1)
        gy = min(ry // step, h - 1)

        # 找所属 zone（只考虑主区域）
        zone_id = "unknown"
        min_dist = 9999
        for z in zones:
            if z["type"] != "main_zone":
                continue
            zcx, zcy = z["center"]
            dist = abs(gx - zcx) + abs(gy - zcy)
            if dist < min_dist:
                min_dist = dist
                zone_id = z["id"]

        # 判断可达性：默认 low，只有完全孤立（周围无通行格）才 high
        access_route = f"{zone_id} -> resource"
        risk = "low"
        nearby_passable = False
        for dy in range(-1, 2):
            for dx in range(-1, 2):
                nx, ny = gx + dx, gy + dy
                if 0 <= nx < w and 0 <= ny < h and mask[ny][nx] == '.':
                    nearby_passable = True
                    break
            if nearby_passable:
                break
        if not nearby_passable:
            risk = "high"
            access_route = f"{zone_id} -> resource (isolated, on blocked terrain)"

        result.append({
            "id": i + 1,
            "zone": zone_id,
            "grid_pos": [gx, gy],
            "world_pos": [rx * tile_size, ry * tile_size],
            "access_route": access_route,
            "risk_level": risk,
            "status": "unclaimed"
        })

    return result


# ============================================================
# 3. 兵力态势摘要
# ============================================================

def analyze_forces(units: List[Dict], zones: List[Dict],
                   mask: List[str], step: int = 3, tile_size: int = 20) -> Dict:
    """把单位列表翻译成分区兵力态势。"""
    h = len(mask)
    w = len(mask[0]) if h > 0 else 0

    # 按队伍和 zone 统计
    team_zones = defaultdict(lambda: defaultdict(list))

    for u in units:
        team = u.get("team", -1)
        if team < 0:
            continue

        x, y = u.get("x", 0), u.get("y", 0)
        gx = min(int(x / tile_size) // step, w - 1)
        gy = min(int(y / tile_size) // step, h - 1)

        # 找最近 zone
        zone_id = "unknown"
        min_dist = 9999
        for z in zones:
            zcx, zcy = z["center"]
            dist = abs(gx - zcx) + abs(gy - zcy)
            if dist < min_dist:
                min_dist = dist
                zone_id = z["id"]

        team_zones[team][zone_id].append(u)

    # 汇总
    force_disposition = {}
    all_teams = sorted(team_zones.keys())

    for team in all_teams:
        zones_data = {}
        total = 0
        for zone_id, unit_list in team_zones[team].items():
            # 按类型统计
            type_counts = defaultdict(int)
            for u in unit_list:
                enum_name = u.get("enum", "")
                if enum_name:
                    type_counts[enum_name] += 1
                else:
                    type_counts["unknown"] += 1

            zones_data[zone_id] = {
                "count": len(unit_list),
                "types": dict(type_counts),
                "task": "defending" if "choke" in zone_id else "patrolling"
            }
            total += len(unit_list)

        force_disposition[team] = {
            "total": total,
            "by_zone": zones_data
        }

    # 计算敌我比例（按 zone）
    force_ratios = {}
    for zone in zones:
        zid = zone["id"]
        counts = {}
        for team in all_teams:
            cnt = force_disposition[team]["by_zone"].get(zid, {}).get("count", 0)
            if cnt > 0:
                counts[team] = cnt

        if len(counts) >= 2:
            max_team = max(counts, key=counts.get)
            min_team = min(counts, key=counts.get)
            ratio = counts[max_team] / max(1, counts[min_team])
            force_ratios[zid] = f"{ratio:.1f}:1_favorable_team_{max_team}"
        elif len(counts) == 1:
            force_ratios[zid] = f"sole_control_team_{list(counts.keys())[0]}"

    return {
        "teams": {str(k): v for k, v in force_disposition.items()},
        "force_ratio_by_zone": force_ratios
    }


# ============================================================
# 4. 经济健康度
# ============================================================

def analyze_economy(teams: List[Dict], history: Dict = None) -> Dict:
    """把原始经济数据翻译成产能健康度。"""
    if history is None:
        history = {}

    result = {}
    for t in teams:
        tid = t.get("teamId", -1)
        if tid < 0:
            continue

        credits = t.get("credits", 0)
        energy = t.get("energy", 0)
        unit_count = t.get("unitCount", 0)

        # 估算收入（需要历史数据，否则默认为 0）
        income = 0
        if str(tid) in history:
            prev = history[str(tid)]
            dt = 2.0
            income = max(0, (credits - prev.get("credits", credits)) / dt)

        # 存储状态评级
        storage_state = "critically_low"
        if credits > 8000:
            storage_state = "high"
        elif credits > 4000:
            storage_state = "medium"
        elif credits > 1500:
            storage_state = "low"

        # 建议
        recommendation = "stable"
        if income < 10 and credits < 2000:
            recommendation = "urgently_expand_miners"
        elif income < 10:
            recommendation = "expand_miners_or_reduce_production"
        elif credits > 10000:
            recommendation = "maximize_production"

        result[str(tid)] = {
            "team_name": t.get("name", f"Team_{tid}"),
            "income_per_second": round(income, 1),
            "storage": round(credits, 1),
            "storage_state": storage_state,
            "unit_count": unit_count,
            "recommendation": recommendation
        }

    return result


# ============================================================
# 5. 事件检测（对比上一帧）
# ============================================================

def detect_events(current: Dict, previous: Dict) -> List[Dict]:
    """检测两帧之间的关键变化。"""
    events = []
    if not previous:
        return events

    # 队伍经济突变
    prev_teams = {t["teamId"]: t for t in previous.get("teams", [])}
    for t in current.get("teams", []):
        tid = t["teamId"]
        if tid in prev_teams:
            prev_credits = prev_teams[tid].get("credits", 0)
            cur_credits = t.get("credits", 0)
            if abs(cur_credits - prev_credits) > 500:
                direction = "increased" if cur_credits > prev_credits else "decreased"
                events.append({
                    "type": "economy_shift",
                    "team": tid,
                    "details": f"Credits {direction} by {abs(cur_credits - prev_credits):.0f}"
                })

    # 单位数量突变
    prev_units = previous.get("units", [])
    cur_units = current.get("units", [])
    if abs(len(cur_units) - len(prev_units)) > 5:
        events.append({
            "type": "unit_count_change",
            "details": f"Total units changed from {len(prev_units)} to {len(cur_units)}"
        })

    return events


# ============================================================
# 6. 主入口：生成 LLM 友好 JSON
# ============================================================

def load_history(path: str = "d:/tiexiuzhanz/rw_history.json") -> Dict:
    if os.path.exists(path):
        with open(path, "r", encoding="utf-8") as f:
            return json.load(f)
    return {}


def save_history(history: Dict, path: str = "d:/tiexiuzhanz/rw_history.json"):
    with open(path, "w", encoding="utf-8") as f:
        json.dump(history, f, ensure_ascii=False, indent=2)


def generate_llm_report(data: Dict, history: Dict = None) -> Dict:
    """将原始 rw_units.json 翻译成参谋部战报。"""
    if history is None:
        history = load_history()

    terrain_data = data.get("terrain", {})
    mask = terrain_data.get("mask", [])
    step = terrain_data.get("step", 3)
    tile_size = data.get("map", {}).get("tileSize", 20)

    # 地形拓扑
    zones, travel_risks = analyze_topology(mask, step)

    # 资源点归属
    resources = assign_resources(
        data.get("resources", []), zones, mask, step, tile_size
    )

    # 兵力态势
    forces = analyze_forces(
        data.get("units", []), zones, mask, step, tile_size
    )

    # 经济健康度（传入上一帧的团队经济数据）
    economy = analyze_economy(data.get("teams", []), history.get("teams"))

    # 事件检测
    events = detect_events(data, history.get("_last_data"))

    # 更新历史
    history["teams"] = {
        str(t["teamId"]): {"credits": t.get("credits", 0)}
        for t in data.get("teams", [])
    }
    history["_last_data"] = data
    save_history(history)

    return {
        "timestamp": data.get("timestamp", 0),
        "update_type": "event_driven" if events else "periodic",
        "events": events,
        "map_topology": {
            "map_size": data.get("map"),
            "zones": zones,
            "travel_risks": travel_risks
        },
        "resources": resources,
        "force_disposition": forces,
        "economy_status": economy
    }


def main(in_path=None, out_path=None):
    if in_path is None:
        in_path = "d:/tiexiuzhanz/rw_units.json"
    if out_path is None:
        out_path = "d:/tiexiuzhanz/rw_llm.json"

    with open(in_path, "r", encoding="utf-8") as f:
        data = json.load(f)

    report = generate_llm_report(data)

    with open(out_path, "w", encoding="utf-8") as f:
        json.dump(report, f, ensure_ascii=False, indent=2)

    print(f"LLM report written to {out_path}")


if __name__ == "__main__":
    import sys
    main(sys.argv[1] if len(sys.argv) > 1 else None,
         sys.argv[2] if len(sys.argv) > 2 else None)
