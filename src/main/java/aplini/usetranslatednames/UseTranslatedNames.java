package aplini.usetranslatednames;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.chat.ComponentSerializer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerLoadEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static aplini.usetranslatednames.Util.toTranslatedName;


public final class UseTranslatedNames extends JavaPlugin implements CommandExecutor, TabExecutor, Listener {
    private static UseTranslatedNames plugin;
    // 调试模式
    private boolean _debug = false;
    // 配置文件
    List<Cli> list = new ArrayList<>();

    @Override
    public void onEnable() {
        plugin = this;
        plugin.saveDefaultConfig();
        plugin.getConfig();
        loadConfig();
        Util.load(plugin);

        // bStats
        if(getConfig().getBoolean("bStats", true)){
            new Metrics(this, 20766);
        }

        // 注册指令
        Objects.requireNonNull(plugin.getCommand("usetranslatednames")).setExecutor(this);


        // 添加一个数据包监听器
        ProtocolLibrary.getProtocolManager().addPacketListener(new PacketAdapter(this,
                ListenerPriority.LOW,    // 监听器优先级
                PacketType.Play.Server.SYSTEM_CHAT //监听的数据包类型
        ){
            @Override
            public void onPacketSending(PacketEvent event){
                // 获取消息 JSON
                // String json = event.getPacket().getStrings().read(0); // 1.20.4 -
                String json = event.getPacket().getChatComponents().read(0).getJson(); // 1.20.4 +

                if(json == null) return;

                if(_debug){
                    getLogger().info("[---DEBUG---]: "+ json);
                }

                // 遍历替换配置
                for(Cli cli : list){

                    // 防止处理过长的消息
                    if(json.length() > cli.inspectLength){
                        continue;
                    }

                    // 匹配
                    Matcher matcher = Pattern.compile(cli.get).matcher(json);
                    while(matcher.find()){
                        // 取消发送消息
                        event.setCancelled(true);

                        // 匹配到的完整字符串
                        String jsonFrame = cli.set;
                        if(jsonFrame.isEmpty()){ // 如果为空则仅取消发送它
                            continue;
                        }
                        String oldJson = matcher.group(0);

                        // 处理翻译变量 _$1:ItemType_, _$1:TranslatedName_
                        Matcher matcher2 = Pattern.compile("_\\$(\\d+):(TranslatedName|ItemType)_").matcher(jsonFrame);
                        while(matcher2.find()){
                            String var = matcher.group(Integer.parseInt(matcher2.group(1)));
                            if(matcher2.group(2).equals("TranslatedName")){
                                jsonFrame = jsonFrame.replace(matcher2.group(), toTranslatedName(var)[1]);
                            }else{
                                jsonFrame = jsonFrame.replace(matcher2.group(), toTranslatedName(var)[0]);
                            }
                        }

                        // 处理正则变量 _$1_
                        Matcher matcher3 = Pattern.compile("_\\$\\d+_").matcher(jsonFrame);
                        if(matcher3.find()){
                            int matcherLength = matcher.groupCount();
                            for(int i = 1; i <= matcherLength; i++){
                                jsonFrame = jsonFrame.replace("_$" + i + "_", matcher.group(i));
                            }
                        }


                        // 替换原文本中的旧 JSON, 重新发送给玩家
                        jsonFrame = json.replace(oldJson, jsonFrame);
                        Player player = event.getPlayer();
                        // 处理显示位置
                        if(cli.displayPlace.equals("ACTION_BAR")){
                            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, ComponentSerializer.parse(jsonFrame));
                        } else {
                            player.spigot().sendMessage(ComponentSerializer.parse(jsonFrame));
                        }
                    }
                }
            }
        });

        getLogger().info("UseTranslatedNames 已启动");
    }

    public void loadConfig(){
        list = new ArrayList<>();
        for(Map<?, ?> li : plugin.getConfig().getMapList("list")){
            list.add(new Cli().setConfig(li));
        }
    }

    // 检查配置版本
    public void CheckConfigVersion(){
        if(getConfig().getInt("configVersion") != 3){
            getLogger().warning("配置版本不匹配, 可能无法正常运行, 请更新或重建配置");
            getLogger().warning("配置版本不匹配, 可能无法正常运行, 请更新或重建配置");
            getLogger().warning("配置版本不匹配, 可能无法正常运行, 请更新或重建配置");
        }
    }

    @EventHandler // 服务器启动完成事件
    public void onServerLoad(ServerLoadEvent event) {
        // 检查配置版本
        CheckConfigVersion();
    }

    // 执行指令
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        // 判断执行了此插件的哪个指令
        if(label.equals("usetranslatednames")){
            // 默认输出插件信息
            if(args.length == 0){
                sender.sendMessage("IpacEL > UseTranslatedNames: 使用翻译名称");
                sender.sendMessage("  插件版本: 0, 配置版本: "+ plugin.getConfig().getInt("config-version"));
                sender.sendMessage("  指令: ");
                sender.sendMessage("    - /usetranslatednames reload - 重载配置");
                sender.sendMessage("    - /usetranslatednames debug - 调试模式");
                return true;
            }

            // 重载配置
            else if(args[0].equals("reload")){
                loadConfig();
                sender.sendMessage("UseTranslatedNames 已完成重载");
                CheckConfigVersion();
                return true;
            }

            // 调试模式
            else if(args[0].equals("debug")){
                _debug = ! _debug;
                sender.sendMessage("UseTranslatedNames 调试模式: "+ _debug);
                return true;
            }
        }

        // 返回 false 时, 玩家将收到命令不存在的错误
        return false;
    }

    // 指令补全
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if(args.length == 1){
            List<String> list = new ArrayList<>();
            list.add("reload"); // 重载配置
            list.add("debug"); // 调试模式
            return list;
        }
        return null;
    }


    @Override
    public void onDisable() {
        // 注销插件的所有监听器
        ProtocolLibrary.getProtocolManager().removePacketListeners(plugin);
    }
}
