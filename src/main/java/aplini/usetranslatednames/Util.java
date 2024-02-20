package aplini.usetranslatednames;

import aplini.usetranslatednames.Enum.TransVar;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;

import java.util.HashMap;
import java.util.Map;


public class Util {

    // 遍历所有实体和物品
    private static final Map<String, EntityType> enumEntity = new HashMap<>();
    private static final Map<String, Material> enumBlock = new HashMap<>();

    static void load(UseTranslatedNames plugin) {
        // 遍历服务器中的物品和实体
        for(EntityType value : EntityType.values()){
            if(value.isAlive()){
                enumEntity.put(String.valueOf(value.getKey()), value);
            }
        }
        for(Material value : Material.values()){
            if(value.isBlock()){
                enumBlock.put(String.valueOf(value.getKey()), value);
            }
        }
        plugin.getLogger().info("已加载 "+ enumEntity.size() +" 个实体和 "+ enumBlock.size() +" 个物品");
    }

    // 将实体/物品/方块名转换为翻译使用的键
    public static TransVar toTranslatedName(String itemName){
        TransVar transVar = new TransVar();
        // itemType     = 匹配到的类型 entity, item(包括block). 用于 JSON hoverEvent
        // transName    = 用于 JSON translate 的名称

        // 实体列表
        if(enumEntity.containsKey("minecraft:"+ itemName)){
            transVar.itemType = "show_entity";
            transVar.transName = "entity.minecraft."+ itemName;
        }

        // 方块列表
        else if(enumBlock.containsKey("minecraft:"+ itemName)){
            transVar.itemType = "show_item";
            transVar.transName = "block.minecraft."+ itemName;
        }

        // 物品列表
        else{
            transVar.itemType = "show_item";
            transVar.transName = "item.minecraft."+ itemName;
        }

        return transVar;
    }

    // 如果 in1 为空则选择 in2, 否则选择 in1
    public static Object SEL(Object in1, Object in2) {
        return in1 == null ? in2 : in1;
    }
    public static Object SEL(Object in1, Object in2, Object in3) {
        return SEL(SEL(in1, in2), in3);
    }
}



