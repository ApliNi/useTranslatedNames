## useTranslatedNames `v0.2`
内置获取翻译名功能的JSON字符串替换插件, 主要用于翻译 CoreProtect 插件的消息

![](https://github.com/ApliNi/useTranslatedNames/blob/main/_img/%E6%95%88%E6%9E%9C%E5%9B%BE.png)

下载: https://modrinth.com/plugin/usetranslatednames

---

## 功能和指令
- `/usetranslatednames` 显示插件信息
  - `/usetranslatednames reload` 重新加载配置
  - `/usetranslatednames debug` 调试模式

插件会根据配置捕获对应的服务端消息, 然后替换其中的物品名称并重新发送消息. 详细信息参考"配置"部分. 


### 配置
```yaml
# 我没有写更详细的说明, 但你应该用不到这些配置
config-version: 1

list:
  # 检查消息长度是否小于此值
  - inspect-length: 320
    # 检查消息是否以此字符串开头
    inspect-prefix: '{"extra":[{"hov'
    # 替换, 使用正则表达式匹配
    replace-regex: '\{"color":"#31b0e8","text":"([a-z0-9_]+)§f\."\}' # (中文) '\{"color":"#31b0e8","text":"([a-z0-9_]+)§f\。"\}'
    # 替换为
    # {"translate":"__TranslatedName__"}
    #   __ItemName__        = 正则匹配到的实体/物品/方块名
    #   __TranslatedName__  = 转换后用于翻译的名称 "entity.minecraft.allay"
    #   __ItemType_show__   = 提供给 JSON hoverEvent 使用的物品类型 show_entity, show_item(block)
    replace-to: '{"color":"#31b0e8","hoverEvent":{"action":"show_text","contents":{"extra":[{"translate":"__TranslatedName__"},{"text":" §o§7点击复制§r"},{"text":"\n§8minecraft:__ItemName__"}],"text":""}},"clickEvent":{"action":"copy_to_clipboard","value":"__ItemName__"},"extra":[{"translate":"__TranslatedName__"},{"text":" §8__ItemName__"}],"text":""}'
    # 是否启用正则变量 $1...
    use-regex-var: 'false'

```

### 依赖
- ProtocolLib 5.0.0
