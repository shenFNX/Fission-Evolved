# 裂变进化 / Fission Evolved

[![Build](https://github.com/shenFNX/Fission-Evolved/actions/workflows/build-and-test.yml/badge.svg)](https://github.com/shenFNX/Fission-Evolved/actions/workflows/build-and-test.yml)
[![Minecraft](https://img.shields.io/badge/Minecraft-1.7.10-62B47A)](https://github.com/GTNewHorizons/GT-New-Horizons-Modpack)
[![License](https://img.shields.io/badge/License-MIT-6C63FF)](LICENSE)

**把核燃料一路烧到宇宙尽头。**

裂变进化是面向 **GregTech: New Horizons** 的高功率核能附属。它从九种可实际使用的 GT 核燃料棒起步，提供一座简单直接的 7×7×7 大型发电设备，并在 UMV 阶段解锁真正以区块计量的 48×48×32 终局发电结构。

这里没有复杂的堆芯模拟：放入燃料，决定结构和输出方式，然后拿走 EU。机器、倍率、耗时与回收产物都能直接在 NEI 和控制器提示中查到。

## 主要内容

### 大型裂变反应堆

- 7×7×7 结构，控制器从 IV 阶段开始制作。
- 接受 GTNH 原生 GT 核燃料棒、本模组九种燃料，以及已适配的附属燃料。
- 外壳提供 64～8192 并行；线圈每升一级使发电倍率翻倍。
- 六档可选冷却液提供 ×1.25～×16 发电增幅。
- 本轮燃料在启动时一次性消耗，结束后直接输出枯竭燃料的后处理产物。
- 电力先进入控制器的大整数缓存。动力仓吞吐不足不会爆炸；缓存满后机器自动暂停。
- 支持普通、多安、无线与激光动力仓；无线模式默认关闭。

| 冷却液 | 消耗 | 发电倍率 |
| --- | ---: | ---: |
| 水 | 1000 L/s | ×1.25 |
| 蒸馏水 | 1000 L/s | ×1.50 |
| IC2 冷却液 | 1000 L/s | ×2 |
| 超级冷却液 | 1000 L/s | ×4 |
| 极寒之凛冰 | 1000 L/s | ×8 |
| 富快子时间流体 | 20 L/s | ×16 |

### 终极宇宙毁灭发电机

- 48×48×32 的独立终局结构，控制器通过 UMV 装配线制作。
- 只使用永恒熵烬、万宇星核、时空吞噬者与原初神灾四种终极燃料。
- 四种燃料的单根基础功率依次为 `10^30`、`10^40`、`10^50`、`10^60 EU/t`。
- 三套场发生结构统一使用 3、6 或 9 级，对最终功率分别提供 ×1、×5、×10。
- 最大并行固定为 1,048,576，不受结构等级限制。
- 每轮默认持续 1 小时；安装 TST 后，可在控制器槽放置不消耗的“英雄之证”，把本轮延长至 24 小时。
- 不需要动力仓，电力直接进入所有者队伍的无线电网；不产生任何物品或流体输出。
- 未标记的内部空间不参与结构检查，可自由布置自动化。

## 九种燃料

前五种燃料负责从中后期一路推到终局门口，后四种进入装配线、精密组装机、太空组装机和等离子锻炉的终极路线。安装 TST 或 GTNL 时，部分终极配方会自动采用对应附属的真实物品；没有这些附属时始终保留纯 GTNH 材料路线。

所有自定义枯竭棒的离心配方均为**固定产出**，没有概率：

| 燃料棒 | 固定物品回收 | 固定流体回收 |
| --- | ---: | ---: |
| 超新星余烬 | 32× 钨钢粉 | 32,000 L 氦 |
| 炼狱钚芯 | 32× 奥斯米铱粉 | 64,000 L 氡 |
| 硅岩星髓 | 32× 三钛粉 | 128,000 L 氦-3 |
| 裂界奇点 | 32× 中子素粉 | 256,000 L 氚 |
| 泰伯利亚天灾 | 32× 宇宙中子素粉 | 512,000 L 激发 DTRC |
| 永恒熵烬 | 32× 无尽粉 | 1,024,000 L 激发 DTEC |
| 万宇星核 | 32× 宇宙素粉 | 2,048,000 L 原始星质 |
| 时空吞噬者 | 32× 时空粉 | 4,096,000 L 熔融时空 |
| 原初神灾 | 32× 永恒粉 | 8,192,000 L 原初物质 |

## NEI 与操作

- 在 NEI 搜索“裂变进化”或 `Fission Evolved` 可找到全部物品和控制器。
- 右击燃料棒可查看反应堆配方、基础功率、工作时间与直接回收结果。
- 按住控制器的 `LSHIFT` 查看完整结构说明，或使用多方块全息投影仪自动摆放。
- 控制器 GUI 显示当前燃料、剩余时间、实际/最大并行、实时功率、预计总发电、缓存电量、输出目标和暂停原因。
- 运行中主动停机或拆除结构会损失本轮已经投入的燃料和待产物。

## 安装与兼容

| 项目 | 支持情况 |
| --- | --- |
| Minecraft | 1.7.10 |
| GTNH | 以 2.9.0-beta-2 为基线，面向之后的测试版与 daily |
| Java | Java 25 已验证；JAR 保留 Java 17/21/25 多版本内容 |
| 必需模组 | GregTech 5 Unofficial / GTNH 运行环境 |
| 可选联动 | NH Utilities、Twist Space Technology、GT Not Leisure |

1. 从 [Releases](https://github.com/shenFNX/Fission-Evolved/releases) 下载正常发布 JAR。
2. 将 JAR 放进 GTNH 实例的 `mods` 文件夹，并删除旧版本。
3. 不要安装名称带 `-dev`、`-sources` 或 `-api` 的开发产物。
4. 首次进入世界后在 NEI 中确认结构与配方。

附属兼容说明：

- **NH Utilities**：大型裂变反应堆额外识别其六种辐光铀/原初珍珠钚燃料棒，并读取对应回收路线。
- **TST**：终极燃料配方可采用反物质燃料棒、恒星构造框架、空间扭曲器和奇异湮灭燃料棒；“英雄之证”可作为终极发电机的不消耗催化剂。
- **GTNL**：终极燃料配方可采用其激发混合硅岩燃料棒。
- **无附属环境**：所有本模组内容仍可使用 GTNH 自带材料制作，不会因为缺少 TST、GTNL 或 NHU 而失去主线配方。

反应堆只读取 GTNH 的核电倍率配置，不会修改整合包的全局核电数值。

## 客户端效果

工作中的控制器带有动态核心贴图、电弧粒子与循环声效。可在 `config/fissionevolved.cfg` 的 `client` 分类分别关闭，不影响服务器逻辑和发电数据。

## 社区项目说明

裂变进化是玩家制作的非官方 GTNH 附属，不代表 GTNH、GT5U、TST、GTNL 或 NH Utilities 官方。请把本模组自身的问题提交到本仓库，而不是 GTNH 官方问题区。

源码采用 [MIT License](LICENSE)。上游项目、API 与衍生视觉素材的署名见 [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md)。

---

## English

**Fission Evolved** is a high-output nuclear power addon for GregTech: New Horizons. It adds nine usable GT fuel rods, a straightforward 7×7×7 Large Fission Reactor, and a separate 48×48×32 Ultimate Universe-Destroying Generator for UMV-and-beyond power networks.

### Highlights

- The Large Fission Reactor accepts native GTNH rods, all nine Fission Evolved rods, and supported addon rods. Casing tiers provide 64–8192 parallel, coils double output per tier, and six coolants scale generation from ×1.25 to ×16.
- Controller-side arbitrary-precision storage prevents insufficient dynamo throughput from causing explosions. A full buffer safely pauses the machine.
- The Ultimate Universe-Destroying Generator accepts only the final four custom fuels, produces `10^30` to `10^60 EU/t` per rod before structure and parallel multipliers, and sends power directly to the wireless grid.
- Tier 3/6/9 field structures give ×1/×5/×10 final output. Maximum parallel is fixed at 1,048,576.
- Ultimate cycles last one hour. With TST installed, a non-consumed Proof of Heroes in the controller extends a cycle to 24 hours.
- Every custom depleted-rod centrifuge recipe has deterministic item and fluid outputs. There are no recovery chances.
- NEI documents fuels, output, duration, recovery, structures, and all scaling rules.

### Requirements

- Minecraft 1.7.10 and a GTNH / GT5U runtime.
- GTNH 2.9.0-beta-2 is the compatibility baseline; later test and daily builds are the forward target.
- Java 25 is tested. The JAR retains Java 17/21/25 multi-release content.
- NH Utilities, TST, and GTNL are optional. GTNH-only fallback recipes keep the full progression usable without them.

Download a normal player JAR from [Releases](https://github.com/shenFNX/Fission-Evolved/releases), place it in the instance's `mods` folder, and remove older copies. Do not install `-dev`, `-sources`, or `-api` artifacts.

This is an unofficial community addon. Please report Fission Evolved issues here rather than to the GTNH project.
