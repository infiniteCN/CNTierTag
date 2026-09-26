# CNTierTag

CNTierTag 是一个用于 Paper 服务端的 CN Tierlist 段位显示插件。它可以直接在玩家头顶和 Tab 中显示模式、段位与玩家名，也提供 PlaceholderAPI 占位符、查询命令和开发者 API。

默认显示格式：

```text
🗡 HT2 | PlayerName
```

模式图标使用原生 Unicode 字符，不需要材质包。

## 功能

- 直接显示玩家头顶称号和 Tab 前缀
- 支持 Axe、Sword、BUHC、Vanilla、NPOT、Potion、SMP 和 Mace
- 优先按 UUID 匹配；离线模式 UUID 不一致时按玩家名匹配
- 全服共享一份总榜缓存，避免为每名玩家重复下载数据
- HTTP 请求异步执行，不阻塞 Paper 主线程
- 同一份总榜请求会自动合并
- 缓存有效时直接复用，不重复创建异步回调
- TextDisplay 内容未变化时不会重复写入
- 支持退役状态、当前段位和巅峰段位
- 可自定义模式图标、颜色、显示规则和文本格式
- PlaceholderAPI 为可选依赖

## 运行环境

- Java 21
- Paper 1.21+
- PlaceholderAPI 2.11.6+（可选）

当前构建使用 Paper 1.21.11 API，并在 Paper 1.21.11 上完成运行验证。其他 1.21.x 版本未逐个测试。

## 安装

1. 将 `CNTierTag-1.0.0.jar` 放入服务端的 `plugins` 目录。
2. 启动服务器。
3. 按需修改 `plugins/CNTierTag/config.yml`。
4. 使用 `/cntier reload` 重载配置，或重启服务器。

插件默认读取公开总榜：

```text
GET https://cntier.win/api/ranking/overall
```

公开总榜未找到玩家或请求失败时，如果配置了 API Key，插件会尝试 `/api/v1/player/{UUID}`。建议通过 `CNTIER_API_KEY` 环境变量提供 Key，避免将凭据写入配置备份。

PowerShell 示例：

```powershell
$env:CNTIER_API_KEY = "你的 API Key"
java -jar paper.jar --nogui
```

环境变量的优先级高于 `config.yml` 中的 `api.key`。

## 直接显示

```yaml
direct-display:
  enabled: true
  nametag: true
  tab-list: true
  refresh-ticks: 100
```

| 配置项 | 默认值 | 作用 |
|---|---:|---|
| `enabled` | `true` | 开关内置显示功能 |
| `nametag` | `true` | 在玩家头顶创建 TextDisplay |
| `tab-list` | `true` | 修改玩家的 Tab 显示名 |
| `refresh-ticks` | `100` | 检查缓存、内容和可见性的周期，最小 20，最大 1200 |

头顶显示由 `formats.head`、灰色分隔符和白色玩家名组成。`formats.head` 只配置称号部分，不需要手动加入玩家名。

Tab 显示会把 `formats.tag` 作为前缀，接到玩家原有的 Tab 名称前。关闭插件显示时，插件只会恢复自己最后一次写入的内容，不会覆盖其他插件后来设置的名称。

玩家离线、切换世界、关闭显示或插件停用时，TextDisplay 会被清理。对其他玩家不可见或处于隐身状态的玩家，其头顶称号也会隐藏。

如果希望由其他插件处理显示，可设置：

```yaml
direct-display:
  enabled: false
```

然后在对应插件中使用 `%cntier_tag%` 等占位符。

## 显示模式

`default-mode` 支持以下值：

```text
axe, sword, buhc, vanilla, npot, potion, smp, mace, best
```

`display-rule` 支持三种规则：

| 值 | 行为 |
|---|---|
| `selected_only` | 只显示 `default-mode`，该模式没有段位时留空 |
| `highest_only` | 忽略 `default-mode`，显示最高的未退役段位 |
| `mixed` | 优先显示 `default-mode`，没有时回退到最高段位 |

当 `default-mode` 为 `best` 时，会直接选择最佳段位。

## 格式变量

`formats.head`、`formats.tag` 和 `formats.formatted` 支持：

| 变量 | 内容 |
|---|---|
| `<tier>` | 当前段位，例如 `HT2` |
| `<tier_color>` | 当前段位颜色 |
| `<icon>` | 段位图标或退役图标 |
| `<retired>` | 退役标记 |
| `<retired_color>` | 退役标记颜色 |
| `<mode>` | 模式键名 |
| `<mode_name>` | 模式中文名 |
| `<mode_icon>` | 模式图标 |
| `<mode_color>` | 模式颜色 |

颜色支持 `&a` 等传统颜色码和 `&#RRGGBB` 十六进制格式。
默认格式会让退役标记使用 `retired.color`，段位本身仍使用对应的段位颜色。

## 默认模式图标

| 模式 | 图标 | 默认颜色 |
|---|---|---|
| Axe | `🪓` | `#55FF55` |
| Sword | `🗡` | `#A4FDF0` |
| BUHC | `❤` | `#FF5555` |
| Vanilla | `✦` | `#FF55FF` |
| NPOT | `☠` | `#7D4A40` |
| Potion | `⚗` | `#FF0000` |
| SMP | `🛡` | `#ECCB45` |
| Mace | `🔨` | `#AAAAAA` |

所有图标和颜色都可以在 `config.yml` 中修改。

## 占位符

PlaceholderAPI 未安装时，内置头顶显示、Tab 显示和 `/cntier` 命令仍可使用。

模式参数支持正式键名，也支持以下别名：

- `uhc` → `buhc`
- `pot` → `potion`
- `nethop`、`netheritepot` → `npot`
- `crystal`、`cpvp` → `vanilla`

| 占位符 | 返回内容 |
|---|---|
| `%cntier_tag%` | 按当前显示规则生成的标签 |
| `%cntier_mode%` | 当前标签使用的模式键名 |
| `%cntier_mode_name%` | 当前标签使用的模式中文名 |
| `%cntier_mode_icon%` | 当前标签使用的模式图标 |
| `%cntier_mode_color%` | 当前标签使用的模式颜色 |
| `%cntier_tag_sword%` | 指定模式的完整标签 |
| `%cntier_tier_sword%` | 指定模式的当前段位，带颜色 |
| `%cntier_tier_sword_raw%` | 指定模式的当前段位，纯文本 |
| `%cntier_tier_sword_color%` | 指定模式的当前段位颜色 |
| `%cntier_tier_sword_icon%` | 指定模式的段位图标 |
| `%cntier_tier_sword_formatted%` | 使用 `formats.formatted` 生成的文本 |
| `%cntier_peak_sword%` | 指定模式的巅峰段位，带颜色 |
| `%cntier_peak_sword_raw%` | 指定模式的巅峰段位，纯文本 |
| `%cntier_peak_sword_color%` | 指定模式的巅峰段位颜色 |
| `%cntier_retired_sword%` | 是否退役，返回 `true` 或 `false` |
| `%cntier_last_updated_sword%` | 记录更新时间，UTC；数据源未提供时留空 |
| `%cntier_best_tag%` | 最佳段位标签 |
| `%cntier_best_tier%` | 最佳段位，带颜色 |
| `%cntier_best_tier_raw%` | 最佳段位，纯文本 |
| `%cntier_best_mode%` | 最佳段位对应的模式键名 |
| `%cntier_best_mode_name%` | 最佳段位对应的模式中文名 |
| `%cntier_best_mode_icon%` | 最佳段位对应的模式图标 |
| `%cntier_best_mode_color%` | 最佳段位对应的模式颜色 |
| `%cntier_region%` | 玩家地区 |
| `%cntier_blacklisted%` | 是否在黑名单中 |
| `%cntier_blacklist_reason%` | 黑名单原因 |
| `%cntier_blacklist_date%` | 黑名单日期，UTC |
| `%cntier_uuid%` | 数据源中的玩家 UUID |
| `%cntier_status%` | 有可用数据时返回 `有数据` |

首次请求尚无缓存时，占位符返回 `formats.loading`，同时在后台获取数据。默认值为空字符串。

## 命令与权限

| 命令 | 权限 | 说明 |
|---|---|---|
| `/cntier` | `cntier.use` | 查询自己的数据 |
| `/cntier <在线玩家或 UUID>` | `cntier.use` | 查询指定玩家 |
| `/cntier refresh [在线玩家或 UUID]` | `cntier.admin` | 绕过有效缓存并重新查询 |
| `/cntier reload` | `cntier.admin` | 重载配置 |
| `/cntier clear` | `cntier.admin` | 清空内存缓存 |

`cntier.use` 默认对所有玩家开放，`cntier.admin` 默认仅管理员可用。

## 开发者 API

推荐通过 Bukkit `ServicesManager` 获取 API：

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

也可以在插件启用后调用：

```java
CnTierApi api = CnTierTagPlugin.api();
```

`fetchProfile` 返回 `CompletableFuture`。异步回调中不要直接操作 Bukkit 玩家、世界或实体；需要修改游戏状态时，应切回服务器主线程。

## 构建

```powershell
$env:JAVA_HOME = "C:\Program Files\Java\jdk-21"
& "E:\apache-maven-3.9.15\bin\mvn.cmd" clean package
```

构建产物：

```text
target/CNTierTag-1.0.0.jar
```

## 常见问题

### 玩家没有显示段位

1. 使用 `/cntier <玩家名>` 检查是否能查到数据。
2. 检查 `direct-display.enabled` 是否为 `true`。
3. 检查 `display-rule` 和 `default-mode` 是否允许回退到其他模式。
4. 查看服务器日志中的 `[Fetch]` 或 `[Display]` 记录。

### 占位符原样显示

确认 PlaceholderAPI 已安装并成功加载，然后执行 PlaceholderAPI 的重载命令或重启服务器。

### API Key 验证失败

检查 `api.key` 或 `CNTIER_API_KEY`。插件不会在日志中输出 Key 内容。
