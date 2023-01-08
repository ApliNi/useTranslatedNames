// 暂时弃用, 等待更新

//package aplini.usetranslatednames;
//
//import me.clip.placeholderapi.expansion.PlaceholderExpansion;
//import org.bukkit.OfflinePlayer;
//import org.jetbrains.annotations.NotNull;
//
//import static aplini.usetranslatednames.Util.fromConfigArrayList;
//import static aplini.usetranslatednames.Util.fromConfigList;
//
//public class PAPI extends PlaceholderExpansion {
//
//    private final UseTranslatedNames plugin;
//
//    public PAPI(UseTranslatedNames plugin) {
//        this.plugin = plugin;
//    }
//
//    @NotNull
//    @Override
//    public String getAuthor() {
//        return "ApliNi";
//    }
//
//    @NotNull
//    @Override
//    public String getIdentifier() {
//        return "UseTranslatedNames";
//    }
//
//    @NotNull
//    @Override
//    public String getVersion() {
//        return "0.0.1";
//    }
//
//    @Override
//    public String onRequest(final OfflinePlayer player, final String identifier){
//        // 转换为小写
//        String $identifier = identifier.toLowerCase();
//        // log
//        System.out.println("占位符运行: "+ $identifier);
//
//        // 以 "item_" 开头: 物品名称
//        if(identifier.startsWith("name_")){
//            // 获取物品名称
//            String $item = identifier.substring(5);
//            // 如果为空
//            if($item.equals("")){return "物品名称为空";}
//
//            // 判断这个物品是否在指定列表中
//            // 实体列表
//            if(fromConfigArrayList("name.list-entity_minecraft").contains($item)){
//                return "entity.minecraft."+ $item;
//            }
//            // 物品列表
//            else if(fromConfigArrayList("name.list-item_minecraft").contains($item)){
//                return "item.minecraft."+ $item;
//            }
//            // 只剩 block 了
//            else{
//                return "block.minecraft."+ $item;
//            }
//
//        }
//
//        return "Null?";
//    }
//
//}
