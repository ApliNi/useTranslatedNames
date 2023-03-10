## useTranslatedNames `v0.0.2`
用于翻译 CoreProtect 插件的物品名称的插件, 当然也可以用来做其他事

![](https://github.com/ApliNi/useTranslatedNames/blob/main/_img/%E6%95%88%E6%9E%9C%E5%9B%BE.png)

---

本机版本: [1.19], 经过测试的版本: [1.19.2], 预计支持版本: [1.19, 1.19.1, 1.19.2, 1.19.3]. 


**依赖**
- ProtocolLib 5.0.0


**功能**
1. 翻译 CoreProtect 插件的物品名, 插件会使用客户端的翻译, 所以支持各种语言
2. 编辑 JSON 文本


**指令**
- `/usetranslatednames` 显示插件信息
  - `/usetranslatednames reload` 重新加载配置


**配置**
```yaml
# 我没有写更详细的说明, 但你应该用不到这些配置
config-version: 0

matcher:
  # 原消息检查, 用轻量的方法检查原消息是不是我们需要的, 防止正则处理其他消息而浪费性能
  original-message-inspect:
    # 消息长度是否小于此值
    length: 320
    # 消息是否以此字符串开头
    prefix: '{"extra":[{"hov'

  # 用于匹配原消息中的物品名, 程序将获取第一个捕获组的结果
  # 如果设置了其他语言, 则可能需要调整此配置
  regex: '\{"color":"#31b0e8","text":"([a-z0-9_]+)§f\."\}'
  # (中文) regex: '\{"color":"#31b0e8","text":"([a-z0-9_]+)§f\。"\}'

  # 将以上正则匹配到的消息替换为以下内容. 可用变量:
  #   __ItemName__        = 正则匹配到的实体/物品/方块名
  #   __ItemType_show__   = 提供给 JSON hoverEvent 使用的物品类型 show_entity, show_item(block)
  #   __TranslatedName__  = 转换后用于翻译的名称 "entity.minecraft.allay"

  # 这条默认配置支持鼠标悬停显示实体/物品名称(暂时使用文本实现), 点击自动复制__ItemName__
  replace-to: '{"color":"#31b0e8","hoverEvent":{"action":"show_text","contents":{"extra":[{"translate":"__TranslatedName__"},{"text":" §o§7点击复制§r"},{"text":"\n§8minecraft:__ItemName__"}],"text":""}},"clickEvent":{"action":"copy_to_clipboard","value":"__ItemName__"},"extra":[{"translate":"__TranslatedName__"},{"text":" §8__ItemName__"}],"text":""}'
```
