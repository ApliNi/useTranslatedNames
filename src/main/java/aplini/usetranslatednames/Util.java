package aplini.usetranslatednames;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;

import java.util.HashMap;
import java.util.Map;


public class Util {

    // 遍历所有实体和物品
    private static final Map<String,EntityType> enumEntity = new HashMap<>();
    private static final Map<String,Material> enumBlock = new HashMap<>();

    static void load(UseTranslatedNames plugin) {
        plugin.getLogger().info("加载物品列表...");
        for (EntityType value : EntityType.values()) {
            if(value.isAlive()){
                enumEntity.put(String.valueOf(value.getKey()), value);
            }
        }
        for (Material value : Material.values()) {
            if(value.isBlock()){
                enumBlock.put(String.valueOf(value.getKey()), value);
            }
        }
    }

    // 将实体/物品/方块名转换为翻译使用的键
    public static String[] toTranslatedName(String itemName){
        String[] arr = new String[2];
        // String[0]    = 匹配到的类型 entity, item(包括block). 用于 JSON hoverEvent
        // String[1]    = 用于 JSON translate 的名称

        // 实体列表
        if(enumEntity.containsKey("minecraft:"+ itemName)){
            arr[0] = "show_entity";
            arr[1] = "entity.minecraft."+ itemName;
        }

        // 方块列表
        else if(enumBlock.containsKey("minecraft:"+ itemName)){
            arr[0] = "show_item";
            arr[1] = "block.minecraft."+ itemName;
        }

        // 物品列表
        else{
            arr[0] = "show_item";
            arr[1] = "item.minecraft."+ itemName;
        }

        return arr;
    }

}



