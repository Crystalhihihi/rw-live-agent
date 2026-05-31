# RWLive — 铁锈战争实时数据 Agent

通过 Java Agent 从 [Rusted Warfare](https://store.steampowered.com/app/647960/Rusted_Warfare__RTS/) 运行时中提取实时战场数据，并生成 LLM 友好的中文战报。

## 项目结构

```
├── src/                    # Java Agent 源码
│   └── RWLiveFinal.java
├── scripts/                # Python 工具
│   ├── llm_interface.py    # 战报生成器（文本 + JSON）
│   ├── battle_analyzer.py  # 战略分析器（拓扑/兵力/经济）
│   ├── translate_units.py  # 单位名称提取工具
│   └── read_state.py       # 状态读取脚本
├── data/                   # 数据文件
│   ├── unit_mapping.txt    # 单位中文映射（含 sandbox 实测 HP）
│   └── locale_names.txt    # 游戏官方中文名称（从 Strings_zh_cn.properties 提取）
├── build/                  # 构建脚本与历史 JAR
│   ├── build-agent.bat
│   ├── MANIFEST.MF
│   └── rw-live-v*.jar
├── docs/                   # 文档
│   ├── BUILD.md
│   ├── PROJECT_LOG.md
│   └── fallback64.bat      # 游戏启动示例（带 -javaagent）
├── rw-source/              # RWX 反编译源码参考（子模块/克隆）
└── tools/                  # 反编译工具
    └── cfr.jar
```

## 快速开始

### 1. 编译 Agent

必须使用游戏内置的 Java 13 编译，以匹配 class version 57：

```batch
:: 游戏路径
set JAVA_HOME=D:\SteamLibrary\steamapps\common\Rusted Warfare\jvm64
set PATH=%JAVA_HOME%\bin;%PATH%

:: 编译
javac -encoding UTF-8 -d . src\RWLiveFinal.java

:: 打包
jar cvmf build\MANIFEST.MF rw-live-final.jar *.class
```

或使用提供的脚本：
```batch
build\build-agent.bat
```

### 2. 启动游戏并注入 Agent

编辑游戏启动脚本，在 `java` 命令中加入：

```batch
jvm64\bin\java -javaagent:d:\tiexiuzhanz\rw-live-final.jar -Xmx1000M -Dfile.encoding=UTF-8 -Djava.library.path=. -cp "game-lib.jar;libs/*" com.corrodinggames.rts.java.Main
```

参考 `docs/fallback64.bat`。

### 3. 生成战报

```bash
python scripts/llm_interface.py
```

同时生成两份输出：
- **`rw_llm.json`** — LLM 友好的结构化战略数据（拓扑、兵力、经济）
- **`report.txt`** — 人类可读的中文文本战报

也可以单独运行战略分析器：
```bash
python scripts/battle_analyzer.py
```

## 数据格式

### `rw_units.json` — 原始战场数据

Agent 每 2 秒写入：

```json
{
  "timestamp": 1717132800000,
  "map": {
    "widthTiles": 200,
    "heightTiles": 150,
    "tileSize": 20,
    "worldWidth": 4000,
    "worldHeight": 3000
  },
  "resources": [
    {"x": 50, "y": 40},
    {"x": 120, "y": 80}
  ],
  "terrain": {
    "width": 50,
    "height": 38,
    "step": 4,
    "mask": [
      "..........................",
      "....~~~~~~................",
      "....~~~~~~................"
    ]
  },
  "teams": [
    {
      "teamId": 1,
      "name": "玩家1",
      "credits": 5000.0,
      "energy": 1000.0,
      "unitCount": 25,
      "colorId": 0,
      "aiLevel": 0,
      "aiBaseLevel": 0,
      "isAI": false,
      "isDefeated": false,
      "baseX": 1200.0,
      "baseY": 800.0
    }
  ],
  "units": [
    {
      "i": 0,
      "team": 1,
      "type": "j",
      "typeId": 24,
      "enum": "tank",
      "name": "坦克",
      "x": 1200.0,
      "y": 800.0,
      "hp": 210.0,
      "maxHp": 210.0
    }
  ]
}
```

### 字段说明

| 字段 | 说明 |
|------|------|
| `map.widthTiles/heightTiles` | 地图格数 |
| `map.tileSize` | 每格像素尺寸（默认 20） |
| `resources[].x/y` | 资源池格坐标 |
| `terrain.mask` | 字符地形简图（下采样至 ~50x50） |
| `terrain.step` | 下采样步长（每 N 格取 1 点） |
| `teams[].credits` | 队伍资金 |
| `teams[].energy` | 队伍能量/电力 |
| `teams[].unitCount` | 队伍单位总数 |
| `teams[].aiLevel` | AI 难度等级 |
| `teams[].baseX/baseY` | 指挥中心坐标（被摧毁时为 -1） |
| `units[].enum` | `UnitTypeEnum.name()`，如 `"tank"`、`"gunShip"` |
| `units[].name` | 游戏内本地化名称（可能为空） |

### 地形图例

Agent v11+ 从 `PathCostMap` 读取实际通行数据，地形掩码简化为：
- `.` 可通行（陆地/桥面）
- `X` 不可通行（水域/岩浆/悬崖）

> 注：`MapTile.isWater`/`isLava` 等属性在 PC 版中可能为空，但 `PathCostMap.d[]` 是游戏寻路系统实际使用的数据，永远准确。

## 技术要点

- **反射读取**：通过 `GameEngine` 的 `fastGameObjectList` 遍历所有 `BaseUnit`，读取坐标、HP、队伍、类型等字段。
- **Obfuscation 映射**：基于 [eam2539/RWX](https://github.com/eam2539/RWX) 反编译源码确认字段名映射。
- **关键字段映射**：
  - `PositionedObject.posX/posY` → obf `w.eo/ep`
  - `BaseUnit.currentHealth/maxHealth` → obf `am.cu/cv`
  - `BaseUnit.team` → obf `am.dn`
  - `BaseUnit.r()` → 返回 `UnitTypeEnum`（`enum` 名如 `"tank"`）
  - `PlayerTeam.credits` → obf `n.o`
  - `PlayerTeam.energy` → obf `n.p`
  - `PlayerTeam.teamCommandCenter` → obf `n.s`
- **启动方式限制**：`jcmd attach` 方式 `agentmain` 不会被执行，必须使用 `-javaagent` 在 JVM 启动时注入。
- **编码**：Windows 终端默认 GBK，源码和 JSON 均使用 UTF-8，用 Python 或 `cat` 查看可避免乱码。

## 依赖

- Rusted Warfare v1.15 (Steam)
- Python 3.x（用于战报生成）
- 游戏内置 JDK 13（编译 Agent）

## 参考源码

- `rw-source/` 目录包含从 `game-lib.jar` 反编译的参考源码（基于 RWX 项目），用于确认字段和方法映射。

## License

本项目仅用于学习和研究目的。Rusted Warfare 版权归 Corroding Games 所有。
