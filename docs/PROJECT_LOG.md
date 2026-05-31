# 铁锈战争 LLM 操控项目 - 进度与总计划

## 一、项目目标
让LLM（大语言模型）能够读取铁锈战争（Rusted Warfare）实时游戏数据，并辅助或替代真人进行战略决策和操作。

---

## 二、技术架构（已验证）

```
铁锈战争客户端 (JVM)
  │
  ▼
Java Agent 注入 ←── jcmd JVMTI.agent_load
  │
  ▼
反射读取游戏内存对象 (com.corrodinggames.rts.game.n 等)
  │
  ▼
实时导出 JSON/文本 → d:\tiexiuzhanz\rw_live.json
  │
  ▼
Python / LLM 读取 → 决策 → 模拟键鼠操作 / 其他方式
```

**核心验证结果**：
- ✅ Java Agent 可以成功 Attach 到运行中的铁锈战争 JVM
- ✅ 可以反射读取游戏内部对象（队伍、资源、单位统计）
- ✅ 数据是实时的，每秒更新
- ✅ 不需要视觉识别、不需要内存扫描、不需要逆向工程

---

## 三、已完成的成果

### 3.1 环境分析
- 游戏核心逻辑在 `game-lib.jar` 中，包名为 `com.corrodinggames.rts`
- 类名经过 ProGuard 混淆（单字母类名），但字段名保留
- 游戏自带完整 JDK（`jvm64/` 目录），包含 `javac`、`jcmd`、`jps` 等工具
- 真实启动命令：`jvm64\bin\java -cp "game-lib.jar;libs/*" com.corrodinggames.rts.java.Main`

### 3.2 核心类定位

| 类路径 | 功能 | 关键字段 |
|--------|------|---------|
| `com.corrodinggames.rts.game.n` | 队伍/玩家基类 | `static n[] as`（10个队伍槽位） |
| `com.corrodinggames.rts.game.a.a` | AI队伍子类 | `aI`(单位数), `aS`(子类内部资源?) |
| `com.corrodinggames.rts.game.e` | 玩家队伍子类 | 额外字段：`an`, `ao`, `ap`, `aq`, `ar` |
| `com.corrodinggames.rts.game.i` | 游戏引擎 | 待深入 |
| `com.corrodinggames.rts.game.units.ar` | 单位类型枚举 | 50+种单位类型 |

### 3.3 已破译的字段映射

#### 基类 n（所有队伍共有）
| 字段 | 类型 | 含义 | 验证状态 |
|------|------|------|---------|
| `o` | double | **现金（Credits）** | ✅ 已验证（实时增长，玩家14939 vs AI 445） |
| `p` | double | 电力/第二资源 | 🟡 始终为0，原版无电力系统 |
| `q` | int | 游戏帧数/全局计时器 | ✅ 所有队伍几乎相同 |
| `v` | String | 队伍名称（"Player", "AI - Medium"） | ✅ 已验证 |
| `l` | String | 提示文本（Mod用） | ✅ "Note to modifiers..." |
| `m` | boolean | 是否启用 | ✅ Player=true |
| `D` | int | 可能是队伍ID/索引 | 🟡 值为-1（待确认） |

#### AI子类 a.a 特有字段
| 字段 | 类型 | 含义 | 验证状态 |
|------|------|------|---------|
| `aI` | int | **总单位数** | ✅ AI队伍有值（8, 7, 7） |
| `aS` | float | 子类内部资源/统计 | 🟡 变化中，含义待确认 |
| `aT` | float | 同上 | 🟡 变化中 |
| `aU` | float | 可能是百分比或上限 | 🟡 AI队伍常为100 |
| `ax`~`aH` | int | 各类单位/建筑数量统计 | 🟡 需要对照游戏确认 |
| `bf` | boolean | 是否存活/活跃 | ✅ true=活跃 |

#### 玩家子类 e 特有字段（待破译）
| 字段 | 类型 | 值示例 | 猜测 |
|------|------|--------|------|
| `an` | float | 8.04 | 可能是收入倍率或相机缩放 |
| `ao` | float | 40.0 | 未知 |
| `ap` | float | 10.0 | 未知 |
| `aq` | long | 时间戳 | 可能是系统时间 |
| `ar` | double | 和`o`相同 | `o`的副本或缓存 |

### 3.4 已开发的工具

| 文件 | 功能 | 状态 |
|------|------|------|
| `rw-agent.jar` | 基础验证，打印类结构 | ✅ 可用 |
| `rw-data-extractor.jar` | 早期数据导出尝试 | ✅ 可用 |
| `rw-deep-probe.jar` | 深度探测，遍历类层次 | ✅ 可用 |
| `rw-fullscan.jar` | 完整字段扫描（含继承字段） | ✅ 可用 |
| `rw-live.jar` | **实时数据导出（每1秒）** | ✅ **正在运行** |
| `read_state.py` | Python读取器 | ✅ 可用 |

---

## 四、待解决的关键问题

### 4.1 高优先级（阻塞后续进展）

1. **玩家单位数读不到**
   - `game.e`（玩家类）没有 `aI` 字段
   - 需要找到玩家类中对应的单位数字段名
   - **影响**：LLM不知道玩家有多少单位

2. **单位列表未找到**
   - 需要找到每个**具体单位**的数据：位置(x,y)、类型、血量、状态
   - 候选：`bn`, `bq`, `bH`, `bK` 等 ArrayList 字段
   - **影响**：没有单位级数据，LLM无法做战术决策

3. **收入率字段未确认**
   - 用户说"每秒+18"，但哪个字段是收入率？
   - 候选：`an`(8.04) 不匹配，`ao`(40) 也不匹配
   - 可能收入是动态计算的，没有固定字段

### 4.2 中优先级

4. **操作执行层（输出）**
   - 目前只有**数据输入**，没有**操作输出**
   - 方案A：模拟鼠标/键盘（Java Robot或Python pyautogui）
   - 方案B：Hook游戏输入层（更难但更快）
   - 方案C：利用Mod系统 autoTrigger（有限制）

5. **人机协作仲裁**
   - 真人和LLM同时操作，如何避免冲突？
   - 编队隔离（真人1-5，LLM 6-0）是否可行？

6. **LLM上下文压缩**
   - 后期单位数200+时，怎么压缩成LLM能处理的大小？
   - 需要设计摘要算法（按区域聚合、按类型统计）

### 4.3 低优先级

7. **多游戏版本兼容**
   - 当前基于 v1.15，更新后类名/字段可能改变
   - 需要自动化映射更新流程

8. **联机公平性**
   - 单机/自定义房间可用，正式联机可能被封
   - 明确使用场景限制

---

## 五、总计划路线图

### Phase 1: 数据输入层（当前，80%完成）
- [x] 验证Java Agent Attach可行性
- [x] 定位核心类（game.n, game.i, units.ar）
- [x] 破译队伍级字段（现金、单位数、名称）
- [x] 实时数据导出（每秒JSON）
- [ ] **找到单位列表**（每个单位的位置、类型、血量）
- [ ] **找到玩家单位数字段**
- [ ] **确认收入率字段**

### Phase 2: 状态摘要层
- [ ] 单位数据聚合（按区域/类型统计）
- [ ] 设计LLM友好的文本摘要格式
- [ ] 历史趋势追踪（经济曲线、单位增长）
- [ ] 异常检测（被攻击、经济停滞）

### Phase 3: LLM决策层
- [ ] Prompt工程（战略决策：造什么、去哪、打谁）
- [ ] 本地模型部署（7B/14B，降低延迟和成本）
- [ ] 决策置信度评估（低置信度时请求确认）
- [ ] 长期记忆（对局历史、对手习惯）

### Phase 4: 操作执行层
- [ ] 键鼠模拟原型（建造、编队、移动）
- [ ] 指令解析器（LLM自然语言 → 具体操作序列）
- [ ] 人机仲裁（编队隔离、优先级冲突解决）
- [ ] 闭环验证（操作后检查游戏状态是否变化）

### Phase 5: 优化与实战
- [ ] 延迟优化（目标：决策延迟 < 2秒）
- [ ] 对抗原版AI测试
- [ ] 人机协作测试（真人+LLM vs AI）
- [ ] 录像分析（战后复盘）

---

## 六、关键文件位置

```
d:\tiexiuzhanz\                    ← 项目根目录
  PROJECT_LOG.md                   ← 本文档
  rw_live.json                     ← 实时数据（JSON，每秒更新）
  rw_live.txt                      ← 实时数据（文本，每秒更新）
  
  RWAgent.java                     ← 基础验证Agent源码
  RWDataExtractor.java             ← 数据导出Agent源码
  RWDeepProbe.java                 ← 深度探测Agent源码
  RWFullScan.java                  ← 完整扫描Agent源码
  RWLiveExport.java                ← 实时导出Agent源码
  
  rw-agent.jar                     ← 基础验证Agent
  rw-data-extractor.jar            ← 数据导出Agent
  rw-deep-probe.jar                ← 深度探测Agent
  rw-fullscan.jar                  ← 完整扫描Agent
  rw-live.jar                      ← 实时导出Agent（当前运行中）
  
  read_state.py                    ← Python读取器
  
  *.bat                            ← 各种启动/编译脚本
```

---

## 七、已知坑与注意事项

1. **Manifest必须写对Agent-Class名**
   - 之前多次因为Manifest里的类名和实际class文件不匹配导致Agent加载但不执行
   - 教训：打包前必须检查Manifest

2. **字段读取要遍历继承链**
   - `getDeclaredFields()` 只读当前类声明的字段
   - 继承字段（如基类`n`的`o`）需要用`getSuperclass()`遍历
   - 教训：RWDeepProbe早期漏掉了基类字段

3. **混淆后的字段名可能变**
   - ProGuard每次重新混淆可能改变单字母字段映射
   - 但作者似乎用了固定的映射（因为字段名在版本中保持一致）
   - 风险：大版本更新后可能需要重新分析

4. **Agent输出会被JVM吞掉**
   - exe启动的JVM把stdout重定向了，System.out看不到
   - 必须写文件才能看到输出
   - 教训：所有Agent必须写日志文件

5. **主菜单也有数据**
   - 主菜单背景是一个实时运行的演示场景
   - 读取到4个队伍不一定是真正的对局，要确认是不是进了游戏

---

## 八、下一步建议

**当前最该做的事**：找到单位列表。

从FullScan知道队伍对象里有4个ArrayList：`bn`, `bq`, `bH`, `bK`。其中一个大概率是单位列表。写一个Agent遍历这些List，打印元素类型和数量，就能定位。

这是从"队伍Summary"到"单位详情"的关键一跳，完成后LLM就有完整的战场感知了。
