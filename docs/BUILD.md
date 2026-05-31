# RWLiveFinal Agent 构建与部署文档

## 目录结构（当前有效文件）

```
d:\tiexiuzhanz\
├── RWLiveFinal.java      # Agent 源码（唯一源码文件）
├── rw-live-final.jar     # 编译打包后的 Agent JAR
├── MANIFEST.MF           # JAR 清单文件
├── build-agent.bat       # 一键编译脚本
├── read_state.py         # Python 读取脚本（可选）
├── rw_units.json         # Agent 运行时输出（游戏启动后自动生成）
└── BUILD.md              # 本文档
```

## 环境要求

- **编译器必须使用游戏自带的 Java 13**，不能用系统的高版本 Java（如 Java 21）
  - 路径：`D:\SteamLibrary\steamapps\common\Rusted Warfare\jvm64\bin\javac`
  - 原因：游戏内嵌 JVM 是 Java 13，高版本 class 文件无法加载

## 编译步骤

### 手动编译

```batch
set JAVA_HOME=D:\SteamLibrary\steamapps\common\Rusted Warfare\jvm64
set PATH=%JAVA_HOME%\bin;%PATH%
javac -encoding UTF-8 RWLiveFinal.java
```

### 打包 JAR

```batch
echo Premain-Class: RWLiveFinal> MANIFEST.MF
echo Agent-Class: RWLiveFinal>> MANIFEST.MF
echo Can-Redefine-Classes: true>> MANIFEST.MF
echo Can-Retransform-Classes: true>> MANIFEST.MF
jar cvmf MANIFEST.MF rw-live-final.jar *.class
```

### 一键编译（使用 build-agent.bat）

直接运行 `build-agent.bat` 即可。

## 启动游戏（加载 Agent）

**必须使用 `fallback64.bat` 启动**，Steam 直接启动无法加载 Agent。

1. 进入游戏目录：
   ```
   D:\SteamLibrary\steamapps\common\Rusted Warfare\
   ```

2. 运行 `fallback64.bat`（已预先配置好 `-javaagent` 和 PATH）

3. 正常开游戏，Agent 会在后台自动运行

## 数据输出

- 文件：`d:\tiexiuzhanz\rw_units.json`
- 频率：每 2 秒刷新一次
- 格式：JSON，包含所有单位的坐标、HP、队伍、类型ID

## 关键字段说明（通过 CFR 反编译确认）

| 字段 | 所在类 | 含义 |
|------|--------|------|
| `eo` | `gameFramework.w` | X 坐标 |
| `ep` | `gameFramework.w` | Y 坐标 |
| `dn` | `game.units.am` | 队伍 ID |
| `dm` | `game.units.am` | 单位类型 ID |
| `cu` | `game.units.am` | 当前 HP |
| `cv` | `game.units.am` | 最大 HP |
| `bE` | `game.units.am` | 全局单位列表（static） |
