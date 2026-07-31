# CNTierTag

把 [CN Tierlist](https://cntier.win) 的段位直接挂到玩家头顶和 Tab 名字上，模式图标、颜色和排版照 PvPCore 的现有效果走，顺便保留 PlaceholderAPI 占位符与查询命令。

默认装好就显示，不需要再往 TAB 配 `%cntier_tag%`。头顶称号改用跟 PvPCore 一样的 Text Display 层，直接显示成 `彩色模式图标 段位 | 玩家名` 并跟着玩家走；Tab 也是同一套排法。模式色、段位色和排版照 PvPCore 的现有效果走。

## 能干啥

- 无 Key 拉取 CNTier 匿名总榜，优先按 UUID、对不上再按玩家名匹配
- 自动显示头顶名牌和 Tab 标签，不依赖变量
- 直接使用 8 个原生 Unicode 模式图标
- 支持盾斧、剑、BUHC、水晶、合金药水、钻石药水、SMP、重锤八种模式
- PlaceholderAPI 首次请求只触发后台查询，不会卡 Paper 主线程
- 整服共用一份总榜缓存，同一时间最多只拉一次，TAB 刷得再勤也不会一窝蜂冲接口
- 正常、未收录、接口失败分别缓存；旧数据还能在刷新失败时顶一会儿
- 退役段位保留成 `(R) LT2`，不会只剩一个莫名其妙的 `RETIRED`
- 标签能选“只看指定模式 / 只看最高段位 / 指定模式没有就回退最高段位”三种规则
- 每个模式都能单独配颜色和图标，默认值和 PvPCore 一致
- 自带 `/cntier` 查询、强刷、重载和清缓存
- 其他插件可以通过 Bukkit ServicesManager 或静态入口调用开发者 API

## 环境

- Paper 1.21+
- Java 21
- PlaceholderAPI 2.11.6+（可选，只给其他插件继续取变量用）
- 不需要 CNTier API Key

默认数据源是 `GET /api/ranking/overall`。这条匿名总榜目前不要求 `X-Api-Key`，做法和 CnTierTagger 一样；插件会把整榜放进一份全服共享缓存，再按玩家 UUID 或名字取段位。

## 安装

1. 把 `CNTierTag-1.0.0.jar` 放进服务器的 `plugins`。
2. 启动服务器，完事。玩家进服后，头顶和 Tab 会直接显示图标称号，不用填 Key，也不用收材质包。

`api.key` 现在只是可选备用：匿名总榜失败或没找到玩家时，插件才会尝试旧版单玩家接口。真要配，建议在启动服务端前设置环境变量 `CNTIER_API_KEY`，别让 Key 跟着整服备份到处跑。比如 PowerShell：

```powershell
$env:CNTIER_API_KEY = "你自己的Key"
java -jar paper.jar --nogui
```

别把 Key 写进启动脚本后再把脚本扔 GitHub，换了个地方裸奔也还是裸奔。

如果服务器是离线模式，服内 UUID 和 CNTier 收录的正版 UUID 对不上也没事：插件会继续拿玩家名做精确匹配。UUID 仍然排第一，免得重名或改名时串号。

## 直接显示

默认配置已经开了：

```yaml
direct-display:
  enabled: true
  nametag: true
  tab-list: true
  refresh-ticks: 40
```

`nametag` 管头顶名字，`tab-list` 管 Tab。关掉 `enabled` 才会回到纯占位符模式。

`nametag` 会挂一层 Text Display，把模式图标、段位和玩家名放在同一行，不再赌客户端还会不会把原版名字画出来，也不抢主计分板队伍。这样 TAB、侧边栏或其他计分板插件换 Scoreboard 时，头顶称号也不会跟着蒸发。玩家离线、切世界、关闭显示或停服时，这层实体会自动清掉。

## PvPCore 图标

这版不再打包或发送任何自定义材质。模式图标直接使用 PvPCore 的原生 Unicode 字符，客户端进服就能显示，不占服务器端口，也不会弹资源包确认：

| 模式 | 字形 | 默认颜色 |
|---|---|---|
| 盾斧 | `🪓` | `#55FF55` |
| 重锤 | `🔨` | `#AAAAAA` |
| 合金药水 | `☠` | `#7D4A40` |
| 钻石药水 | `⚗` | `#FF0000` |
| SMP | `🛡` | `#ECCB45` |
| 剑 | `🗡` | `#A4FDF0` |
| BUHC | `❤` | `#FF5555` |
| 水晶 / 原版 | `✦` | `#FF55FF` |

## 旧版占位符接法

如果你就是想让另一个插件接管显示，先关掉 `direct-display.enabled`，再按以前的方式放变量：

```yaml
_DEFAULT_:
  tabprefix: "%cntier_tag%"
  tagprefix: "%cntier_tag%"
```

不同 TAB 版本的配置层级可能不一样，认准 `tabprefix`、`tagprefix` 或对应的前缀项就行。

## 选哪个模式显示

`default-mode` 可以填 `axe`、`sword`、`buhc`、`vanilla`、`npot`、`potion`、`smp`、`mace` 或 `best`。`display-rule` 有三种：

| 值 | 实际效果 |
|---|---|
| `selected_only` | 只显示 `default-mode`，该模式没段位就留空 |
| `highest_only` | 忽略指定模式，只显示玩家最高的未退役段位 |
| `mixed` | 优先指定模式，没有才退回最高段位 |

`default-mode: best` 会直接走最高段位，适合懒得纠结模式的服。标签格式额外支持 `<mode>`、`<mode_name>`、`<mode_icon>` 和 `<mode_color>`；默认格式是 `<mode_color><mode_icon> <tier_color><retired><tier> &7| &r`，也就是 PvPCore 的“模式图标 段位 | 玩家名”排法。不喜欢的话直接在 `formats.tag` 改。

## 占位符

模式名可以用：`axe`、`sword`、`buhc`、`vanilla`、`npot`、`potion`、`smp`、`mace`。

另外认这些常见叫法：`uhc` 等于 `buhc`，`pot` 等于 `potion`，`nethop` 等于 `npot`，`crystal` 和 `cpvp` 等于 `vanilla`。

| 占位符 | 返回内容 |
|---|---|
| `%cntier_tag%` | 配置的默认标签，默认挑最佳未退役段位 |
| `%cntier_mode%` | 当前默认标签实际使用的模式键名 |
| `%cntier_mode_name%` | 当前默认标签实际使用的模式中文名 |
| `%cntier_mode_icon%` | 当前默认标签的模式字形 |
| `%cntier_mode_color%` | 当前默认标签的模式颜色 |
| `%cntier_tag_sword%` | 指定模式的完整标签 |
| `%cntier_mode_icon_sword%` | 指定模式的字形 |
| `%cntier_mode_color_sword%` | 指定模式的颜色 |
| `%cntier_tier_sword%` | 带颜色的当前段位 |
| `%cntier_tier_sword_raw%` | 纯文本当前段位，例如 `(R) LT2` |
| `%cntier_tier_sword_color%` | 当前段位颜色，例如 `#D5B355` |
| `%cntier_tier_sword_icon%` | 配置里的段位图标 |
| `%cntier_tier_sword_formatted%` | 带方括号、但不带末尾空格的标签 |
| `%cntier_peak_sword%` | 带颜色的巅峰段位 |
| `%cntier_peak_sword_raw%` | 纯文本巅峰段位 |
| `%cntier_peak_sword_color%` | 巅峰段位颜色 |
| `%cntier_retired_sword%` | `true` 或 `false` |
| `%cntier_last_updated_sword%` | 这条记录最后更新时间，UTC；匿名总榜没给时留空 |
| `%cntier_best_tag%` | 玩家最佳模式标签 |
| `%cntier_best_tier%` | 玩家最佳段位，带颜色 |
| `%cntier_best_tier_raw%` | 玩家最佳段位，纯文本 |
| `%cntier_best_mode%` | 最佳模式键名 |
| `%cntier_best_mode_name%` | 最佳模式中文名 |
| `%cntier_best_mode_icon%` | 最佳模式字形 |
| `%cntier_best_mode_color%` | 最佳模式颜色 |
| `%cntier_region%` | 玩家地区 |
| `%cntier_blacklisted%` | 是否在黑名单中；匿名总榜没给这项时返回 `否` |
| `%cntier_blacklist_reason%` | 黑名单原因；匿名总榜没给时留空 |
| `%cntier_blacklist_date%` | 黑名单日期，UTC；匿名总榜没给时留空 |
| `%cntier_uuid%` | CNTier 返回的玩家 UUID |
| `%cntier_status%` | 有缓存数据时返回 `有数据` |

第一次解析占位符时默认返回空字符串，同时后台开始查。TAB 下一轮刷新就会拿到值，不会为了立刻显示一次把主线程摁住。

## 命令

| 命令 | 权限 | 干嘛的 |
|---|---|---|
| `/cntier` | `cntier.use` | 查自己 |
| `/cntier <在线玩家或UUID>` | `cntier.use` | 查别人 |
| `/cntier refresh [在线玩家或UUID]` | `cntier.admin` | 绕过有效缓存，强制再查一次 |
| `/cntier reload` | `cntier.admin` | 重读接口、颜色、格式和缓存时间 |
| `/cntier clear` | `cntier.admin` | 清空内存缓存 |

## 开发者 API

推荐从 Bukkit 服务管理器拿，插件重载和依赖顺序会更好处理：

```java
CnTierApi api = Bukkit.getServicesManager().load(CnTierApi.class);
if (api != null) {
    api.fetchProfile(player.getUniqueId(), false).thenAccept(result -> {
        result.optionalProfile().ifPresent(profile -> {
            String swordTag = api.formatTag(profile, GameMode.SWORD);
        });
    });
}
```

也可以在 CNTierTag 已启用后调用：

```java
CnTierApi api = CnTierTagPlugin.api();
```

异步回调里别直接碰 Bukkit 玩家、世界或计分板 API。要更新游戏内容，切回主线程，这个坑真挺常见。

## 构建

PowerShell：

```powershell
$env:JAVA_HOME = "C:\Program Files\Java\jdk-21"
& "E:\apache-maven-3.9.15\bin\mvn.cmd" clean package
```

构建产物是 `target/CNTierTag-1.0.0.jar`，没有额外材质包。

## 参考

占位符命名、缓存思路、开发者 API 结构和选段规则参考过下面这些开源项目。匿名总榜的数据结构与缓存方案主要参考 CnTierTagger，再按 Paper 服务端的并发场景补了 UUID 匹配和全服单份缓存：

- [Vadlox/SimpleMCTiers](https://github.com/Vadlox/SimpleMCTiers)
- [Ceymikey/McTiersBridge](https://github.com/Ceymikey/McTiersBridge)
- [tech-anupam/G1axTierlistPlaceholderExpansion](https://github.com/tech-anupam/G1axTierlistPlaceholderExpansion)
- [tmdakm/CnTierTagger](https://github.com/tmdakm/CnTierTagger)

当前版本没有复制 CnTierTagger 的 PNG、字体映射或其他材质资源；它只保留在选段规则的参考来源里。玩家看到的模式图标来自 PvPCore 使用的原生 Unicode 字符。
