# ⚽ 足球游戏 Kotlin 版 (Football Game Kotlin)

[![Build](https://github.com/xf8410/football-game-kotlin/actions/workflows/build.yml/badge.svg)](https://github.com/xf8410/football-game-kotlin/actions/workflows/build.yml)
[![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)
[![API](https://img.shields.io/badge/API-24%2B-brightgreen.svg)](https://developer.android.com/about/versions/nougat/android-7.0)

基于 Godot GDScript 版本重写的 Android 原生足球游戏。

## 📱 功能特性

### 已实现
- ✅ **五大联赛** — 英超/西甲/德甲/意甲/法甲 完整球队数据
- ✅ **知名球星** — 哈兰德/姆巴佩/贝林厄姆/萨拉赫等 50+ 球星
- ✅ **3D球场** — 标准 FIFA 尺寸球场 (105m × 68m)
- ✅ **11v11** — 两支队伍完整对战
- ✅ **触屏操作** — 虚拟摇杆 + 情境化按钮
- ✅ **AI系统** — 4档难度，三层AI架构
- ✅ **比赛系统** — 计时/比分/进球/出界/犯规/黄红牌
- ✅ **多种射门** — 普通射门/远射/搓射/电梯球/挑射
- ✅ **传球系统** — 短传/直塞/传中/挑球/2过1
- ✅ **定位球** — 任意球/角球/界外球/球门球/点球
- ✅ **体力系统** — 跑步消耗/冲刺额外消耗
- ✅ **存档系统** — 本地 JSON 存储
- ✅ **联赛模式** — 选择联赛和球队进行比赛

### 计划中
- 🔜 **完整联赛** — 积分榜/赛程/升降级
- 🔜 **杯赛模式** — 淘汰赛/小组赛
- 🔜 **球员成长** — 等级/经验/属性升级
- 🔜 **局域网联机** — 1v1 对战
- 🔜 **音效解说** — 程序化音效 + TTS 解说

## 🏟️ 联赛数据

### 英超 (Premier League)
阿森纳 | 曼城 | 利物浦 | 曼联 | 切尔西 | 热刺 | 纽卡斯尔 | 阿斯顿维拉 | 布莱顿 | 西汉姆联

### 西甲 (La Liga)
皇家马德里 | 巴塞罗那 | 马德里竞技 | 皇家社会 | 比利亚雷亚尔 | 皇家贝蒂斯 | 毕尔巴鄂竞技 | 赫罗纳

### 德甲 (Bundesliga)
拜仁慕尼黑 | 多特蒙德 | 勒沃库森 | 莱比锡 | 法兰克福 | 斯图加特

### 意甲 (Serie A)
国际米兰 | AC米兰 | 尤文图斯 | 那不勒斯 | 罗马 | 拉齐奥 | 亚特兰大

### 法甲 (Ligue 1)
巴黎圣日耳曼 | 马赛 | 里昂 | 摩纳哥 | 尼斯 | 里尔

## 🌟 知名球星

| 联赛 | 球星 |
|------|------|
| 英超 | 哈兰德、萨拉赫、福登、德布劳内、萨卡、厄德高、帕尔默、孙兴慜 |
| 西甲 | 姆巴佩、维尼修斯、贝林厄姆、莱万多夫斯基、亚马尔、格里兹曼 |
| 德甲 | 凯恩、穆西亚拉、维尔茨、萨内、阿德耶米 |
| 意甲 | 劳塔罗、莱奥、弗拉霍维奇、克瓦拉茨赫利亚、奥西梅恩 |
| 法甲 | 登贝莱、巴尔科拉、阿森西奥 |

## 🎮 操作方式

### 触屏操作
- **左侧虚拟摇杆**: 360° 移动控制
- **右侧按钮（有球时）**: 传球 / 直塞 / 射门 / 冲刺
- **右侧按钮（无球时）**: 切换 / 冲刺

## 🏗️ 技术架构

- **语言**: Kotlin
- **UI**: Jetpack Compose
- **渲染**: OpenGL ES 2.0
- **架构**: MVVM + Clean Architecture
- **最低 API**: 24 (Android 7.0)

## 📁 项目结构

```
app/src/main/java/com/football/game/
├── core/               # 核心游戏逻辑
│   ├── GameEngine.kt   # 游戏引擎
│   ├── GameState.kt    # 游戏状态
│   └── Vector3.kt      # 3D向量
├── ai/                 # AI 系统
│   ├── PlayerAI.kt     # 球员AI
│   └── TeamAI.kt       # 球队战术
├── model/              # 数据模型
│   ├── Player.kt       # 球员模型
│   ├── Team.kt         # 球队模型
│   ├── Match.kt        # 比赛模型
│   └── League.kt       # 联赛模型
├── data/               # 数据管理
│   ├── SaveManager.kt  # 存档管理
│   ├── PlayerDatabase.kt    # 球员数据库
│   ├── LeagueDatabase.kt    # 联赛数据库
│   └── FamousPlayers.kt     # 著名球员
├── ui/                 # 界面
│   ├── screen/         # 页面
│   └── component/      # 组件
└── render/             # 渲染
    └── GameRenderer.kt # OpenGL渲染器
```

## 🚀 快速开始

### 下载安装
前往 [Releases](https://github.com/xf8410/football-game-kotlin/releases) 下载 APK。

### 从源码构建
```bash
# 克隆仓库
git clone https://github.com/xf8410/football-game-kotlin.git

# 用 Android Studio 打开项目
# 点击 Run 按钮运行
```

## 📜 许可证

MIT License — 可自由使用、修改、分发

## 🤝 贡献

欢迎提交 Issue 和 Pull Request！

## 🙏 致谢

- 数据来源: [Transfermarkt](https://www.transfermarkt.com/)
- 球队数据: [TheSportsDB](https://www.thesportsdb.com/)
