## useTranslatedNames `v0.1.1`
内置获取翻译名功能的JSON字符串替换插件, 主要用于翻译 CoreProtect 插件的消息

![](https://github.com/ApliNi/useTranslatedNames/blob/main/_img/%E6%95%88%E6%9E%9C%E5%9B%BE.png)

---

本机版本: [1.19], 经过测试的版本: [1.19.3], 预计支持版本: [1.19+]. 


**依赖**
- ProtocolLib 5.0.0


**功能**
1. 翻译部分插件显示的物品名, 插件会使用客户端的翻译, 所以支持各种语言
2. 编辑 JSON 文本


**指令**
- `/usetranslatednames` 显示插件信息
  - `/usetranslatednames reload` 重新加载配置
  - `/usetranslatednames debug` 调试模式


**配置**
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
    replace-to: '{"color":"#31b0e8","hoverEvent":{"action":"show_text","contents":{"extra":[{"translate":"__TranslatedName__"},{"text":" §o§7点击复制§r"},{"text":"\n§8minecraft:__ItemName__"}],"text":""}},"clickEvent":{"action":"copy_to_clipboard","value":"__ItemName__"},"extra":[{"translate":"__TranslatedName__"},{"text":" §8__ItemName__"}],"text":""}'
    #   __ItemName__        = 正则匹配到的实体/物品/方块名
    #   __ItemType_show__   = 提供给 JSON hoverEvent 使用的物品类型 show_entity, show_item(block)
    #   __TranslatedName__  = 转换后用于翻译的名称 "entity.minecraft.allay"
    # {"translate":"__TranslatedName__"}
```
