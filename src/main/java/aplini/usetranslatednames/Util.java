package aplini.usetranslatednames;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class Util {

    // 为 toTranslatedName 类生成用于查找物品的散列表
    private static final Map<String,EntityType> enumHashMap_EntityType = new HashMap<>();
    private static final Map<String,Material> enumHashMap_Material = new HashMap<>();
    static {
        System.out.println("开始遍历枚举类并生成散列表");
        for (EntityType value : EntityType.values()) {
            if(value.isAlive()){
                String name = String.valueOf(value.getKey());
                enumHashMap_EntityType.put(name, value);
            }
        }
        for (Material value : Material.values()) {
            String name = String.valueOf(value.getKey());
            enumHashMap_Material.put(name, value);
        }
    }

    // 将实体/物品/方块名转换为翻译使用的键
    public static String[] toTranslatedName(String itemName){
        String[] arr = new String[2];
        // String[0]    = 匹配到的类型 entity, item(包括block). 用于 JSON hoverEvent
        // String[1]    = 用于 JSON translate 的名称

        // 实体列表
        if(enumHashMap_EntityType.containsKey("minecraft:"+ itemName)){
            arr[0] = "show_entity";
            arr[1] = "entity.minecraft."+ itemName;
        }

        // 物品列表
        else if(enumHashMap_Material.containsKey("minecraft:"+ itemName)){
            arr[0] = "show_item";
            // 是否为方块
            if(enumHashMap_Material.get("minecraft:"+ itemName).isBlock()){
                arr[1] = "block.minecraft."+ itemName;
            }else{
                arr[1] = "item.minecraft."+ itemName;
            }
        }else{
            // 找不到, 输出空气
            arr[0] = "show_item";
            arr[1] = "block.minecraft.air";
        }

        return arr;
    }

}



