package aplini.usetranslatednames;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;

import java.util.ArrayList;
import java.util.List;


public class Comment implements CommandExecutor, TabExecutor {

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
                sender.sendMessage("  插件版本: 0, 配置版本: "+ Util.fromConfig("config-version"));
                sender.sendMessage("  指令: ");
                sender.sendMessage("    - /usetranslatednames reload - 重载配置");
                return true;
            }

            // 重载配置
            else if(args[0].equals("reload")){
                Util.reloadConfig();
                sender.sendMessage("UseTranslatedNames 已完成重载");
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
            return list;
        }
        return null;
    }
}
