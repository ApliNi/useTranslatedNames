package aplini.usetranslatednames;

import java.util.ArrayList;
import java.util.List;


public class Util {

    // 获取配置, 通过配置路径
    public static String fromConfig(String path) {
        return UseTranslatedNames.getInstance().getConfig().getString(path);
    }
//    public static Boolean fromConfigBoolean(String path) {
//        return UseTranslatedNames.getInstance().getConfig().getBoolean(path);
//    }
    public static int fromConfigInt(String path) {
        return UseTranslatedNames.getInstance().getConfig().getInt(path);
    }
    public static List<String> fromConfigList(String path) {
        return (List<String>) UseTranslatedNames.getInstance().getConfig().getList(path);
    }
    public static ArrayList<String> fromConfigArrayList(String path) {
        return (ArrayList<String>) UseTranslatedNames.getInstance().getConfig().getList(path);
    }

    // 重载配置文件
    public static void reloadConfig() {
        UseTranslatedNames.getInstance().reloadConfig();
    }

    // 将实体/物品/方块名转换为翻译使用的键
    public static String toTranslatedName(String itemName){
        // 判断这个物品是否在指定列表中
        // 实体列表
        if(fromConfigArrayList("name.list-entity_minecraft").contains(itemName)){
            return "entity.minecraft."+ itemName;
        }
        // 物品列表
        else if(fromConfigArrayList("name.list-item_minecraft").contains(itemName)){
            return "item.minecraft."+ itemName;
        }
        // 只剩 block 了
        else{
            return "block.minecraft."+ itemName;
        }
    }

}
