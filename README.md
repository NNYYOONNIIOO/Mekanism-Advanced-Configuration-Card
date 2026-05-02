# 通用机械高级配置卡 / Mekanism Advanced Configuration Card

![](src/main/resources/assets/mekanism_advanced_configuration_card/textures/item/card_slot_bag.png)

[中文](#中文) | [English](#english)

---

<a name="中文"></a>
## 中文

### 简介

这是一个 Minecraft 1.12.2 的 Forge 模组，为 Mekanism CE Unofficial 添加了增强版配置卡功能。

**灵感来源于 [Mekanism Config Card Upgrades](https://www.curseforge.com/minecraft/mc-mods/mekanism-config-card-upgrades)。**

### 功能特性

#### 高级配置卡
- **复制升级数据**：复制机器时同时保存升级卡数据（速度升级、能量升级等）
- **复制工厂等级**：支持复制工厂等级到其他机器，自动消耗工厂安装器进行升级
- **跨机器粘贴**：支持从工厂复制配置到普通机器，或从普通机器复制到工厂
- **超量升级支持**：支持保存和粘贴超出常规上限的升级数量（可通过配置关闭）
- **清除数据**：潜行+右键空气或非机械方块可清除配置卡数据

#### 卡槽包
- 用于存储升级卡和工厂安装器
- 配置卡会自动从卡槽包中消耗/返还物品
- 支持 BaublesEX 饰品栏（身体部位）

#### 兼容性
- **MekanismCeU-MoreMachine**：完整支持更多机械的机器
- **BaublesEX**：卡槽包可放入饰品栏

### 使用方法

| 操作 | 效果 |
|------|------|
| 潜行 + 右键机器 | 复制机器配置和升级 |
| 右键机器 | 粘贴配置和升级 |
| 潜行 + 右键空气/非机械方块 | 清除配置卡数据 |

### 依赖

| 模组 | 必需 |
|------|------|
| Mekanism CE Unofficial | 是 |
| MixinBooter | 是 |
| BaublesEX | 否（可选，用于饰品栏支持） |
| MekanismCeU-MoreMachine | 否（可选，用于更多机械支持） |

### 配置选项

配置文件位于 config/mekanism_advanced_configuration_card.cfg

### 安装

1. 安装 Minecraft Forge 1.12.2
2. 安装 Mekanism CE Unofficial
3. 安装 MixinBooter
4. 将模组 jar 文件放入 mods 文件夹

---

<a name="english"></a>
## English

### Introduction

A Minecraft 1.12.2 Forge mod that adds an advanced configuration card for Mekanism CE Unofficial.

**Inspired by [Mekanism Config Card Upgrades](https://www.curseforge.com/minecraft/mc-mods/mekanism-config-card-upgrades).**

### Features

#### Advanced Configuration Card
- **Copy Upgrade Data**: Saves upgrade cards when copying machine configuration
- **Copy Factory Tier**: Supports copying factory tier to other machines
- **Cross-Machine Paste**: Supports pasting from factory to regular machines and vice versa
- **Excess Upgrades Support**: Supports saving and pasting upgrades beyond the normal limit
- **Clear Data**: Sneak + right-click on air or non-machine blocks to clear card data

#### Card Slot Bag
- Stores upgrade cards and tier installers
- Configuration card automatically consumes/returns items from the bag
- Supports BaublesEX accessory slot (body slot)

#### Compatibility
- **MekanismCeU-MoreMachine**: Full support for MoreMachine machines
- **BaublesEX**: Card slot bag can be placed in accessory slot

### Usage

| Action | Effect |
|--------|--------|
| Sneak + Right-click machine | Copy configuration and upgrades |
| Right-click machine | Paste configuration and upgrades |
| Sneak + Right-click air/non-machine block | Clear card data |

### Dependencies

| Mod | Required |
|-----|----------|
| Mekanism CE Unofficial | Yes |
| MixinBooter | Yes |
| BaublesEX | No (optional) |
| MekanismCeU-MoreMachine | No (optional) |

### Installation

1. Install Minecraft Forge 1.12.2
2. Install Mekanism CE Unofficial
3. Install MixinBooter
4. Place the mod jar file into the mods folder

---

## Credits

- Inspired by: Mekanism Config Card Upgrades
- Mekanism CE Unofficial
- Author: Nyonio
