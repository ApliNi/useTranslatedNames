package aplini.usetranslatednames;

import aplini.usetranslatednames.Enum.Key;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.concurrent.CompletableFuture;

import static aplini.usetranslatednames.UseTranslatedNames.*;

public class onPlayerChat implements Listener {

    static onPlayerChat func = null;
    static Key mode;

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onAsyncPlayerChatEvent(AsyncPlayerChatEvent event){
        // 取消发送消息
        event.setCancelled(true);

        CompletableFuture.runAsync(() -> {

            Player player = event.getPlayer();
            // 获取已格式化的消息
            String msg = String.format(event.getFormat(), player.getName(), event.getMessage());

            if(_debug >= 1){
                plugin.getLogger().info("");
                plugin.getLogger().info("");
                plugin.getLogger().info("[DEBUG] [PlayerChat] [Player: "+ event.getPlayer().getName() +
                        ", Lang: "+ player.getLocale() +"] [Length: "+ msg.length() +"], [MODE: "+ mode.toString() +"]");
                if(_debug >= 2){
                    plugin.getLogger().info("  - [FORMAT]: "+ event.getFormat());
                    plugin.getLogger().info("  - [MSG]: "+ event.getMessage());
                    plugin.getLogger().info("  - [GET]: "+ msg);
                }
            }
            Bukkit.getConsoleSender().sendMessage(msg);
            switch(mode){
                case Convert -> Bukkit.spigot().broadcast(new TextComponent(msg));
                case ConvertBypass -> {
                    // 静默发送消息给每个玩家
                    PacketContainer chatPacket = protocolManager.createPacket(PacketType.Play.Server.SYSTEM_CHAT);
                    chatPacket.getChatComponents().write(0, WrappedChatComponent.fromText(msg));
                    for(Player li : Bukkit.getOnlinePlayers()){
                        protocolManager.sendServerPacket(li, chatPacket, false);
                    }
                }
            }
        });
    }
}