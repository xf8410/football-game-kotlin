# ⚽ 足球游戏 Kotlin 版 (Football Game Kotlin)

[![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)
[![API](https://img.shields.io/badge/API-24%2B-brightgreen.svg)](https://developer.android.com/about/versions/nougat/android-7.0)

基于 Godot GDScript 版本重写的 Android 原生足球游戏。

## 📱 功能特性

### 已实现
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

### 计划中
- 🔜 **联赛模式** — 双循环赛程，积分榜
- 🔜 **杯赛模式** — 淘汰赛/小组赛
- 🔜 **球员系统** — 球员卡牌/转会/成长
- 🔜 **局域网联机** — 1v1 对战
- 🔜 **音效解说** — 程序化音效 + TTS 解说

## 🎮 操作方式

### 触屏操作
- **左侧虚拟摇杆**: 360° 移动控制
- **右侧按钮（有球时）**: 传球 / 直塞 / 射门 / 冲刺
- **右侧按钮（无球时）**: 压迫 / 切换 / 冲刺

## 🏗️ 技术架构

- **语言**: Kotlin
- **渲染**: OpenGL ES 3.0 / Vulkan
- **UI**: Jetpack Compose
- **架构**: MVVM + Clean Architecture
- **最低 API**: 24 (Android 7.0)

## 📁 项目结构

```
app/src/main/java/com/football/game/
├── core/               # 核心游戏逻辑
│   ├── GameEngine.kt   # 游戏引擎
│   ├── GameState.kt    # 游戏状态
│   └── Physics.kt      # 物理系统
├── ai/                 # AI 系统
│   ├── PlayerAI.kt     # 球员AI
│   └── TeamAI.kt       # 球队战术
├── model/              # 数据模型
│   ├── Player.kt       # 球员模型
│   ├── Team.kt         # 球队模型
│   └── Match.kt        # 比赛模型
├── ui/                 # 界面
│   ├── screen/         # 页面
│   └── component/      # 组件
└── data/               # 数据
    ├── database/       # 数据库
    └── repository/     # 仓库
```

## 🚀 快速开始

### 环境要求
- Android Studio Arctic Fox+
- Kotlin 1.8+
- Gradle 8.0+

### 构建运行
```bash
# 克隆仓库
git clone https://github.com/xf8410/football-game-kotlin.git

# 用 Android Studio 打开项目
# 点击 Run 按钮运行
```

### 下载安装
前往 [Releases](https://github.com/xf8410/football-game-kotlin/releases) 下载 APK。

## 📜 许可证

MIT License — 可自由使用、修改、分发

## 🤝 贡献

欢迎提交 Issue 和 Pull Request！