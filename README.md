# ⚽ 足球游戏 Kotlin 版 (Football Game Kotlin)

[![Build](https://github.com/xf8410/football-game-kotlin/actions/workflows/build.yml/badge.svg)](https://github.com/xf8410/football-game-kotlin/actions/workflows/build.yml)
[![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)
[![API](https://img.shields.io/badge/API-24%2B-brightgreen.svg)](https://developer.android.com/about/versions/nougat/android-7.0)

基于 Godot GDScript 版本重写的 Android 原生足球游戏。

## 📱 功能特性

### ✅ 物理和碰撞系统
- **真实碰撞体积** - 圆柱体碰撞，防止穿模
- **身体对抗** - 抢断时的物理对抗，有力量计算
- **护球机制** - 按住护球按钮可以保护球
- **爆趟突破** - 快速带球突破，消耗体力
- **盘带系统** - 基础盘带、变向、技术动作

### ✅ 盘带系统
- **基础盘带** - 稳定控球，速度适中
- **爆趟** - 快速突破，消耗体力，控球质量下降
- **技术动作** - 踩单车、身体假动作、剪刀脚、克鲁伊夫转身
- **穿裆** - 当对手两腿分开时可以穿裆过人
- **空间分析** - AI会分析最佳突破方向

### ✅ 五大联赛
- **英超** - 10支球队（阿森纳、曼城、利物浦等）
- **西甲** - 8支球队（皇马、巴萨、马竞等）
- **德甲** - 6支球队（拜仁、多特、勒沃库森等）
- **意甲** - 7支球队（国米、米兰、尤文等）
- **法甲** - 6支球队（巴黎、马赛、摩纳哥等）

### ✅ 传奇球星
- **现代 (2020s)** - 姆巴佩、哈兰德、维尼修斯、贝林厄姆
- **近年 (2010s)** - 梅西、C罗、内马尔、莫德里奇、伊涅斯塔
- **经典 (2000s)** - 罗纳尔迪尼奥、齐达内、大罗、亨利、卡卡
- **黄金年代 (80-90s)** - 马拉多纳、范巴斯滕、古利特、巴乔
- **复古 (60-70s)** - 贝利、贝肯鲍尔、加林查、尤西比奥

### ✅ 游戏系统
- **AI系统** - 4档难度，三层AI架构
- **比赛系统** - 计时/比分/进球/出界/犯规/黄红牌
- **定位球** - 任意球/角球/界外球/球门球/点球
- **存档系统** - 本地 JSON 存储

## 🎮 操作方式

### 触屏操作
- **左侧虚拟摇杆**: 360° 移动控制
- **右侧按钮（有球时）**:
  - 传球：短传给最近队友
  - 直塞：传到队友前方
  - 射门：向球门射门
  - 冲刺：快速带球突破（消耗体力）
  - 护球：保护球不被抢断

### 盘带技巧
- **基础盘带**: 轻推摇杆
- **爆趟**: 推摇杆 + 冲刺按钮
- **变向**: 快速改变摇杆方向
- **护球**: 背对对手时按护球按钮

## 🏗️ 技术架构

### 物理系统 (PhysicsSystem.kt)
- 球员碰撞检测（圆柱体）
- 身体对抗计算
- 球的物理（重力、摩擦、Magnus效应）
- 球场边界检测

### 碰撞体积 (PlayerModel.kt)
- 身体碰撞体积
- 腿部碰撞（用于抢断）
- 护球区域
- 抢断区域

### 盘带系统 (DribbleSystem.kt)
- 控球质量计算
- 技术动作系统
- 突破方向分析
- 空间检测

## 📁 项目结构

```
app/src/main/java/com/football/game/
├── core/               # 核心游戏逻辑
│   ├── GameEngine.kt   # 游戏引擎
│   ├── GameState.kt    # 游戏状态
│   ├── Vector3.kt      # 3D向量
│   ├── PlayerModel.kt  # 球员模型和碰撞
│   ├── PhysicsSystem.kt# 物理系统
│   └── DribbleSystem.kt# 盘带系统
├── ai/                 # AI 系统
├── model/              # 数据模型
├── data/               # 数据管理
├── ui/                 # 界面
└── render/             # 渲染
```

## 🚀 快速开始

### 下载安装
前往 [Releases](https://github.com/xf8410/football-game-kotlin/releases) 下载 APK。

### 从源码构建
```bash
git clone https://github.com/xf8410/football-game-kotlin.git
# 用 Android Studio 打开项目
# 点击 Run 按钮运行
```

## 📜 许可证

MIT License — 可自由使用、修改、分发