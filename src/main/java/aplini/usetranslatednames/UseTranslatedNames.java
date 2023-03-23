package aplini.usetranslatednames;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import net.md_5.bungee.chat.ComponentSerializer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static aplini.usetranslatednames.Util.*;


public final class UseTranslatedNames extends JavaPlugin implements CommandExecutor, TabExecutor {
    private static UseTranslatedNames plugin;
    // 调试模式
    private boolean _debug = false;

    @Override
    public void onEnable() {
        plugin = this;
        // 保存默认配置
        plugin.saveDefaultConfig();
        // 加载配置
        plugin.getConfig();
        // 注册指令
        Objects.requireNonNull(plugin.getCommand("usetranslatednames")).setExecutor(this);
        // 初始化 Util
        new Util();


        // 添加一个数据包监听器
        ProtocolLibrary.getProtocolManager().addPacketListener(new PacketAdapter(this,
                ListenerPriority.LOW,    // 监听器优先级
                PacketType.Play.Server.SYSTEM_CHAT //监听的数据包类型
        ){
            @Override
            public void onPacketSending(PacketEvent event){
                // 获取消息 JSON
                PacketContainer packet = event.getPacket();
                String message = packet.getStrings().read(0);

                if(message == null) return;

                if(_debug){
                    getLogger().info("[调试] 监听到的JSON消息: "+ message);
                }

                // 列表
                for(Map<?, ?> list : plugin.getConfig().getMapList("list")){
                    // 限制
                    if(message.length() < (Integer.parseInt(list.get("inspect-length").toString())) &&
                            message.startsWith(list.get("inspect-prefix").toString())){

                        // 匹配
                        Matcher matcher = Pattern.compile(list.get("replace-regex").toString()).matcher(message);
                        if(matcher.find()){
                            // matcher.group(0)
                            // 0 = 整个正则, 1 = 捕获组

                            // 获取翻译后的json文本
                            String[] translated = toTranslatedName(matcher.group(1));
                            String messageOfTranslated = list.get("replace-to").toString()
                                    .replace("__ItemName__", matcher.group(1))
                                    .replace("__ItemType_show__", translated[0])
                                    .replace("__TranslatedName__", translated[1]);
                            // 将原消息中的目标字符串替换为翻译后的
                            String newMessage = message.replace(matcher.group(0), messageOfTranslated);

                            // 取消发送原消息, 发送处理后的消息
                            event.setCancelled(true);
                            Player player = event.getPlayer();
                            player.spigot().sendMessage(ComponentSerializer.parse(newMessage));

                            break;
                        }
                    }
                }
            }
        });

        System.out.println("UseTranslatedNames 已启动");
    }

    // 执行指令
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // sender = 发送命令的对象, 比如玩家/ 控制台/ 命令方块...
        // command = 命令的内容
        // label = 主命令, 不包括命令后面的参数
        // args = 命令参数数组, 不保留主命令字符串

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
                plugin.reloadConfig();
                sender.sendMessage("UseTranslatedNames 已完成重载");
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
    public List<String> onTabComplete(CommandSender sendermm, Command command, String label, String[] args) {
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

        System.out.println("UseTranslatedNames 已关闭");
    }
}
