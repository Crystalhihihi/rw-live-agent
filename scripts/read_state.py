#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
读取铁锈战争游戏状态JSON，并格式化为LLM可读的文本摘要
"""

import json
import time
import os

STATE_FILE = r"d:\tiexiuzhanz\rw_gamestate.json"


def read_state():
    if not os.path.exists(STATE_FILE):
        print("[Reader] 状态文件不存在，请确保游戏已启动且Agent已加载")
        return None
    
    try:
        with open(STATE_FILE, 'r', encoding='utf-8') as f:
            return json.load(f)
    except Exception as e:
        print(f"[Reader] 读取失败: {e}")
        return None


def format_for_llm(data):
    if not data or 'teams' not in data:
        return "暂无游戏数据"
    
    lines = ["=== 铁锈战争当前战况 ===", ""]
    
    for team in data['teams']:
        name = team.get('name', f"队伍{team['index']}")
        lines.append(f"【{name}】")
        
        # 打印所有字段
        for key, val in team.items():
            if key in ('index', 'name', '_type'):
                continue
            if isinstance(val, float):
                lines.append(f"  {key}: {val:.1f}")
            else:
                lines.append(f"  {key}: {val}")
        lines.append("")
    
    return "\n".join(lines)


def main():
    print("[Reader] 铁锈战争状态读取器")
    print(f"[Reader] 监控文件: {STATE_FILE}")
    print("[Reader] 按 Ctrl+C 退出\n")
    
    last_mtime = 0
    
    while True:
        try:
            if os.path.exists(STATE_FILE):
                mtime = os.path.getmtime(STATE_FILE)
                if mtime != last_mtime:
                    last_mtime = mtime
                    data = read_state()
                    if data:
                        print(format_for_llm(data))
                        print("-" * 40)
            
            time.sleep(2)
        except KeyboardInterrupt:
            print("\n[Reader] 已退出")
            break


if __name__ == "__main__":
    main()
