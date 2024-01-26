## useTranslatedNames
内置获取翻译名功能的JSON字符串替换插件, 可用于消息替换和翻译 CoreProtect 等插件的消息

![](https://github.com/ApliNi/useTranslatedNames/blob/main/_img/%E6%95%88%E6%9E%9C%E5%9B%BE.png)

下载: https://modrinth.com/plugin/usetranslatednames

---

## 功能和指令
- `/utn` 显示插件信息和统计数据
  - `/utn json <JSON>`  - 测试 JSON 字符串
  - `/utn reload`       - 重新加载配置
  - `/utn debug`        - 调试模式

插件会根据配置捕获对应的服务端消息, 然后替换其中的物品名称并重新发送消息. 详细信息参考"配置"部分. 


### 配置
```yaml
configVersion: 3

dev:
  # true  = 适用于 1.20.4 版本的方法 (需要 ProtocolLib 5.2.0
  # false = 适用于更低版本的旧方法
  listeningMode: true

list: # 替换列表

  # 翻译实体名 :: 24.03/m 前 #**苦力怕** 破坏 草方块
  - inspectLength: 1024
    get: '\{"text":"#([a-z0-9_]+)§f([^"]+)","color":"#31B0E8"\}'
    set: >-
      {"text":"§8#"},
      {"translate":"_$1:TranslatedName_","color":"#31b0e8"},
      {"text":"§f_$2_","color":"#31B0E8"}


  # 翻译物品 :: 24.03/m 前 #苦力怕 破坏 **草方块**
  - inspectLength: 1024
    get: '\{"text":"([a-z0-9_]+)(?:§f.{0,3})?","color":"#31B0E8"\}'
    set: >-
      {"extra":[
        {"translate":"_$1:TranslatedName_"},
        {"text":" §8_$1_",
          "hoverEvent":{
            "action":"show_text",
            "contents":{"text":"§7点击复制ID\n§8minecraft:_$1_"}
          },
        "clickEvent":{"action":"copy_to_clipboard","value":"_$1_"}
        }
      ],"color":"#31b0e8","text":""}


    # [示例] 所有可用配置
    # [可选, 默认所有玩家] 对拥有该权限的玩家处理这条消息
  - permission: 'minecraft.command'
    # [必选] 检查消息长度是否小于此值
    inspectLength: 64
    # [必选] 使用正则表达式匹配
    get: '^\{"text":"","extra":\["Missing required argument (\d+)"\]\}$'
    # [必选] 将消息替换为
    # _$1_    = 正则匹配到的变量 1, 也可以是 `_$2_` (第 2 个变量)...
    # _$1:ItemType_   = 将 _$1_ 用于获取物品类型, 提供给 JSON hoverEvent 使用的物品类型 show_entity, show_item(block)
    # _$1:TranslatedName_   = 将 _$1_ 用于名称翻译, 返回与语言路径对应的 KEY, 例如 `entity.minecraft.allay`
    # _$1:Words:组名_   = 将 _$1_ 用于词替换, 需要配置 words.yml
    # 其他示例:
    # set: >- # YAML 语法中使用 `>-` 可以编写换行的文本, 效果如上
    # set: '' # 如果为空, 则取消发送这条消息
    set: '{"text":"§bIpacEL §f> §b此指令需要至少§a_$1:Words:中文数字_个参数"}'
    # [可选, 默认聊天栏] 将消息显示在操作栏 (物品栏上面)
    displayPlace: 'ACTION_BAR'

```

```yaml
# 词替换配置文件
words:
  # 创建一个组, 组名不应包含下划线 "_"
  中文数字:
    # 添加一个词替换
    # 如果多个词的配置重复, 将会被忽略
    - get: '1' # [必选] 需要匹配的词
      set: '一' # [必选] 替换为
      lang: 'zh_cn' # [可选, 默认直接替换] 当客户端语言与其匹配时进行替换

    - get: '2'
      set: '二'
      lang: 'zh_cn'

    - get: '3'
      set: '三'
      lang: 'zh_cn'

```

### 依赖
- ProtocolLib
