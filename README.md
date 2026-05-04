---

# 通用机械高级配置卡 / Mekanism Advanced Configuration Card

[中文](#中文) | [English](#english)

---

<a name="中文"></a>
## 中文

### 简介

这是一个 Minecraft 1.12.2 的 Forge 模组，添加了高级配置卡。

**灵感来源于 [Mekanism Config Card Upgrades](https://www.curseforge.com/minecraft/mc-mods/mekanism-config-card-upgrades)。**

### 功能特性

#### 高级配置卡
- 跨机器复制配置；
- 复制升级；
- 复制等级；
- 复制为空时，返还升级。

#### 卡槽包
- 用于存储升级和工厂安装器；
- 配置卡会自动从卡槽包中消耗/返还物品（返还可配置）；
- 支持 BaublesEX 饰品栏（任意槽位）；
- 在升级安装界面显示卡槽包中的数量，并实现一键升级/卸载操作。

#### 兼容性
- 通用机械：更多机械：几乎完美支持更多机械的机器；
- Baubles：卡槽包可放入饰品栏，并调用；
- 无限升级卡：支持使用无限升级卡来升级机器；
- R键整理：在卡槽包中使用整理功能；
- JEI物品管理器：JEI避让，升级安装界面卡槽包GUI；
- 应用能源2：高级配置卡绑定网络后，允许从网络中提取/返还升级。

### 使用方法

| 操作 | 效果 |
|------|------|
| 潜行 + 右键机器 | 复制机器配置和升级 |
| 右键机器 | 粘贴配置和升级 |
| 潜行 + 右键空气/非机械方块 | 清除配置卡数据 |

### 依赖
Mekanism CE Unofficial

### 配置选项

配置文件位于 `config/mekanism_advanced_configuration_card.cfg`

### 安装

1. 安装 Minecraft Forge 1.12.2
2. 安装 Mekanism CE Unofficial
3. 将模组 jar 文件放入 mods 文件夹

---

<a name="english"></a>
## English

### Introduction

A Minecraft 1.12.2 Forge mod that adds an advanced configuration card.

**Inspired by [Mekanism Config Card Upgrades](https://www.curseforge.com/minecraft/mc-mods/mekanism-config-card-upgrades).**

### Features

#### Advanced Configuration Card
- Cross-machine copy configuration;
- Copy upgrades;
- Copy tier/level;
- When the copied data is empty, return the upgrade.

#### Card Slot Bag
- Stores upgrade cards and tier installers;
- Configuration card automatically consumes/returns items from the bag (return behavior configurable);
- Supports BaublesEX accessory slot (any slot);
- Displays the count of items in the card slot bag within the upgrade installation GUI, allowing one-click upgrade/removal operations.

#### Compatibility
- **MekanismCEU-MoreMachine**: Nearly perfect support for MoreMachine machines;
- **BaublesEX**: Card slot bag can be placed in the accessory slot and interacted with;
- **Infinite Upgrade Card**: Supports using infinite upgrade cards to upgrade machines;
- **Inventory Tweaks**: Supports sorting items inside the card slot bag;
- **JEI**: JEI avoidance, upgrade installation GUI and card slot bag GUI;
- **Applied Energistics 2**: After binding the advanced configuration card to a network, allows extraction/return of upgrades from the AE2 network.

### Usage

| Action | Effect |
|--------|--------|
| Sneak + Right-click machine | Copy machine configuration and upgrades |
| Right-click machine | Paste configuration and upgrades |
| Sneak + Right-click air / non-Mekanism block | Clear card data |

### Dependencies
Mekanism CE Unofficial

### Configuration

Configuration file is located at `config/mekanism_advanced_configuration_card.cfg`

### Installation

1. Install Minecraft Forge 1.12.2
2. Install Mekanism CE Unofficial
3. Place the mod jar file into the `mods` folder

---

## Credits

- Inspired by: Mekanism Config Card Upgrades
- Mekanism CE Unofficial
- Author: Nyonio
