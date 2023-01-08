package aplini.usetranslatednames;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import net.md_5.bungee.chat.ComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static aplini.usetranslatednames.Util.*;


public final class UseTranslatedNames extends JavaPlugin {
    private static UseTranslatedNames plugin;

    @Override
    public void onEnable() {
        plugin = this;
        // 保存默认配置
        plugin.saveDefaultConfig();
        // 加载配置
        plugin.getConfig();
        // 注册指令
        plugin.getCommand("usetranslatednames").setExecutor(new Comment());

        // 添加一个数据包监听器
        ProtocolLibrary.getProtocolManager().addPacketListener(new PacketAdapter(this,
                ListenerPriority.LOW,    // 监听器优先级
                PacketType.Play.Server.SYSTEM_CHAT //监听的数据包类型
        ){
            @Override
            public void onPacketSending(PacketEvent event){
                // 获取消息 JSON
                PacketContainer $packet = event.getPacket();
                String $message = $packet.getStrings().read(0);

                // System.out.println("监听器收到消息: "+ $message);

                // 消息长度限制
                if(
                        // 字符串长度小于配置值
                        $message.length() < fromConfigInt("matcher.original-message-inspect.length") &&
                        // 字符串以此开头
                        $message.startsWith(fromConfig("matcher.original-message-inspect.prefix"))
                ){
                    // 正则匹配
                    Matcher $matcher = Pattern.compile(fromConfig("matcher.regex")).matcher($message);
                    if($matcher.find()){
                        // $matcher.group(0)
                        // 0 = 整个正则, 1 = 捕获组

                        // 获取翻译后的json文本
                        String $messageOfTranslated = fromConfig("matcher.replace-to")
                                .replace("__ItemName__", $matcher.group(1))
                                .replace("__TranslatedName__", toTranslatedName($matcher.group(1)));
                        // 将原消息中的目标字符串替换为翻译后的
                        String $newMessage = $message.replace($matcher.group(0), $messageOfTranslated);

                        // 取消发送原消息, 发送处理后的消息
                        event.setCancelled(true);
                        Player $player = event.getPlayer();
                        $player.spigot().sendMessage(ComponentSerializer.parse($newMessage));
                    }
                }
            }
        });

        System.out.println("UseTranslatedNames 已启动");
    }


    @Override
    public void onDisable() {
        // 注销插件的所有监听器
        ProtocolLibrary.getProtocolManager().removePacketListeners(plugin);

        System.out.println("UseTranslatedNames 已关闭");
    }


    public static UseTranslatedNames getInstance() {
        return plugin;
    }
}
