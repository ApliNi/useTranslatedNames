package aplini.usetranslatednames;

import aplini.usetranslatednames.Enum.Cli;
import aplini.usetranslatednames.Enum.Key;
import aplini.usetranslatednames.Enum.Status;
import aplini.usetranslatednames.Enum.Word;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.chat.ComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.*;
import java.util.regex.Matcher;

import static aplini.usetranslatednames.Util.SEL;
import static aplini.usetranslatednames.Util.toTranslatedName;


public final class UseTranslatedNames extends JavaPlugin implements CommandExecutor, TabExecutor, Listener {
    // 用户配置版本
    public static int _configVersion = 0;
    // 调试模式
    private int _debug = 0;
    // 监听器模式
    boolean listeningMode = true;
    // 序列化消息
    boolean serialization = false;
    // 对未定义 inherit 的配置使用 FINALLY
    public static boolean overallFinally = false;
    // 词配置 Map<组名.词 or 组名.语言.词, 词配置>
    private HashMap<String, Word> words;
    // 配置文件
    List<Cli> list = new ArrayList<>();
    int listSize = 0;
    // 记录统计信息
    Status status = new Status();
    // 用于记录重复消息的表
    private final HashSet<String> duplicateMessage = new HashSet<>();

    @Override
    public void onEnable() {
        loadConfig();
        Util.load(this);

        // bStats
        if(getConfig().getBoolean("bStats", true)){
            new Metrics(this, 20766);
        }

        // 注册指令
        Objects.requireNonNull(getCommand("utn")).setExecutor(this);


        // 添加一个数据包监听器
        ProtocolLibrary.getProtocolManager().addPacketListener(new PacketAdapter(this,
                ListenerPriority.LOW,    // 监听器优先级
                PacketType.Play.Server.SYSTEM_CHAT //监听的数据包类型
        ){
            @Override
            public void onPacketSending(PacketEvent event){

                long _startTime = System.nanoTime(); // 记录运行时间

                Player player = event.getPlayer();

                // 获取消息 JSON
                String json;
                if(listeningMode){
                    // 1.20.4 +
                    json = event.getPacket().getChatComponents().readSafely(0).getJson();
                }else{
                    // 1.20.4 -
                    json = event.getPacket().getStrings().readSafely(0);
                }
                if(json == null) return;

                // 序列化消息
//                getLogger().info(json);
                String serializationJson;
                // 原版的玩家加入消息可能在这里处理时会出错
                // 暂时这样解决一下 (
                try {
                    serializationJson = ComponentSerializer.toString(ComponentSerializer.parse(json));
                } catch (Exception e) {
                    serializationJson = json;
                }
                if(serialization){
                    json = serializationJson;
                }
                // 检查消息是否重复
                String duplicateTest = player.getUniqueId().hashCode() +"."+ serializationJson.toLowerCase().hashCode();
                if(duplicateMessage.contains(duplicateTest)){
                    duplicateMessage.remove(duplicateTest);
                    return;
                }

                // 需要防止匹配重复的消息

                if(_debug >= 1){
                    getLogger().info("");
                    getLogger().info("");
                    getLogger().info("[DEBUG] [Player: "+ event.getPlayer().getName() +", Lang: "+ player.getLocale() +"] [Length: "+ json.length() +"]");
                }
                if(_debug >= 2){
                    getLogger().info("  - [GET]: "+ json);
                }

                // 运行字符串替换
                runJsonTestReplaceAll(event, player, json);

                // 记录运行时间
                status.Messages ++;
                status.TotalTime += (System.nanoTime() - _startTime) / 1_000_000.0;

                if(_debug >= 2){
                    getLogger().info("  - [Time: "+ String.format("%.3f", (System.nanoTime() - _startTime) / 1_000_000.0) +" ms]");
                }
            }
        });

        getLogger().info("UseTranslatedNames 已启动");
    }

    // 处理字符串替换
    public void runJsonTestReplaceAll(PacketEvent event, Player player, String jsonTest){
        // 遍历替换配置
        for(int i = 0; i < listSize; i++){
            // 替换完成后退出循环, 否则继续
            if(runJsonTestReplaceConfig(event, player, jsonTest, i)){
                break;
            }
        }
    }

    public boolean runJsonTestReplaceConfig(PacketEvent event, Player player, String jsonTest, int configIndex){

        Cli cli = list.get(configIndex);

        if(_debug >= 4){
            if(cli.inheritData == Key.LINK || cli.inheritData == Key.LINK_SER){
                getLogger().info("  - [CONFIG: "+ configIndex +"] <=> [CONFIG: "+ (configIndex + 1) +"]");
            }else{
                getLogger().info("  - [CONFIG: "+ configIndex +"]"+ (configIndex == (listSize - 1) ? " <- [END]" : ""));
            }

        }

        boolean forOK = false;

        // 如果权限不为空 且 玩家没有该权限, 则不进行替换
        if(!cli.permission.isEmpty() && !player.hasPermission(cli.permission)){
            return false;
        }

        // 防止处理过长或过短的消息
        if(jsonTest.length() > cli.inspectLengthMax && jsonTest.length() < cli.inspectLengthMin){
            return false;
        }

        // 匹配
        Matcher matcher = cli.regExp.matcher(jsonTest);
        while(matcher.find()){

            status.Matches ++;

            if(_debug >= 4){
                getLogger().info("    - [HIT]");
                getLogger().info("    - [GROUP]: "+ matcher.group());
            }

            // 如果匹配, 则立即退出匹配检查循环, 不再处理任何数据
            if(cli.inheritData == Key.CLOSE){
                return true;
            }

            // 取消发送消息
            event.setCancelled(true);

            // 如果为空, 则取消发送这条消息
            if(cli.setData == Key.NULL){
                forOK = true;
                break;
            }

            // 匹配到的完整字符串
            String oldJson = matcher.group(0);

            // 将 get 匹配到的消息原封不动的搬下来
            String jsonFrame;
            if(cli.setData == Key._USE_GET_){
                jsonFrame = oldJson;
            }else{
                jsonFrame = cli.set;
            }


            // 处理翻译变量替换 _$1:ItemType_, _$1:TranslatedName_
            if(cli.transVarEn){
                cli.transVarData.reset();
                while(cli.transVarData.find()){
                    String var = matcher.group(Integer.parseInt(cli.transVarData.group(1)));
                    if(cli.transVarData.group(2).equals("TranslatedName")){
                        jsonFrame = jsonFrame.replace(cli.transVarData.group(), toTranslatedName(var).transName);
                    }else{
                        jsonFrame = jsonFrame.replace(cli.transVarData.group(), toTranslatedName(var).itemType);
                    }
                }
            }


            // 处理词替换 _$1:Words:xxx_
            if(cli.wordReplaceEn){
                cli.wordReplaceData.reset();
                while(cli.wordReplaceData.find()){
                    String var = matcher.group(Integer.parseInt(cli.wordReplaceData.group(1)));
                    Word word = (Word) SEL(
                            words.get(cli.wordReplaceData.group(2) +"."+ player.getLocale() +"."+ var),
                            words.get(cli.wordReplaceData.group(2) +".."+ var),
                            var);
                    if(word != null){
                        jsonFrame = jsonFrame.replace(cli.wordReplaceData.group(), word.set);
                    }
                }
            }


            // 处理正则替换 _$1_
            if(cli.regExpReplaceEn){
                int matcherLength = matcher.groupCount();
                for(int i = 1; i <= matcherLength; i++){
                    jsonFrame = jsonFrame.replace("_$" + i + "_", matcher.group(i));
                }
            }


            // 替换原文本中的旧 JSON, 重新发送给玩家
            jsonFrame = jsonTest.replace(oldJson, jsonFrame);

            // 与下一条配置合并为组, 将此配置处理完毕的内容直接传递给下一条配置处理
            if(cli.inheritData == Key.LINK){
                runJsonTestReplaceConfig(event, player, jsonFrame, configIndex + 1);
                return true;
            }
            // 使 GROUP 传递序列化后的 JSON 文本
            if(cli.inheritData == Key.LINK_SER){
                String jsonFrameSer = ComponentSerializer.toString(ComponentSerializer.parse(jsonFrame));
                runJsonTestReplaceConfig(event, player, jsonFrameSer, configIndex + 1);
                return true;
            }

            // 序列化消息
            BaseComponent[] message = ComponentSerializer.parse(jsonFrame);

            // 记录即将重复的消息
            duplicateMessage.add(player.getUniqueId().hashCode() +"."+ ComponentSerializer.toString(message).toLowerCase().hashCode());

            // 调试
            if(_debug >= 3){
                getLogger().info("  - [SET]: "+ ComponentSerializer.toString(message));
            }

            // 处理显示对象
            switch(cli.displayObjectData){

                case DEFAULT, COPY_TO_CONSOLE: // 默认方式处理
                    // 将消息复制到控制台
                    if(cli.displayObjectData == Key.COPY_TO_CONSOLE){
                        getLogger().info("["+ player.getName() +"]:");
                        Bukkit.getConsoleSender().spigot().sendMessage(message);
                    }
                    // 处理显示位置
                    if(cli.displayPlaceData == Key.ACTION_BAR){
                        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, message);
                    } else {
                        player.spigot().sendMessage(message);
                    }
                    break;

                case ALL, EXCLUDE: // 将消息广播给所有玩家. 仅限于只有自己能收到消息的情况
                    for(Player tp : Bukkit.getOnlinePlayers()){
                        // 将消息广播给所有玩家, 但不包括自己. 仅限于只有自己能收到消息的情况
                        if(cli.displayObjectData == Key.EXCLUDE){
                            continue;
                        }
                        // 处理显示位置
                        if(cli.displayPlaceData == Key.ACTION_BAR){
                            tp.spigot().sendMessage(ChatMessageType.ACTION_BAR, message);
                        } else {
                            tp.spigot().sendMessage(message);
                        }
                    }
                    break;

                case CONSOLE: // 将消息转发到控制台, 自己不会收到
                    getLogger().info("["+ player.getName() +"]:");
                    Bukkit.getConsoleSender().spigot().sendMessage(message);
                    break;
            }
            forOK = true;
        }
        return forOK;
    }

    // 加载配置文件
    public long loadConfig(){
        long _startTime = System.nanoTime(); // 记录运行时间

        if(!new File(getDataFolder(), "words.yml").exists()){
            saveResource("words.yml", false);
        }
        if(!new File(getDataFolder(), "config.yml").exists()){
            saveResource("config.yml", false);
        }
        reloadConfig();

        // 检查配置版本
        _configVersion = getConfig().getInt("configVersion");
        if(_configVersion != 4){
            getLogger().warning("配置版本不匹配, 可能无法正常运行, 请手动更新或重建配置");
            getLogger().warning("配置版本不匹配, 可能无法正常运行, 请手动更新或重建配置");
        }

        // 监听器模式
        listeningMode = getConfig().getBoolean("dev.listeningMode", true);
        // 序列化消息
        serialization = getConfig().getBoolean("dev.serialization", false);
        // 全局 Finally
        overallFinally = getConfig().getBoolean("dev.overallFinally", false);

        // 处理词替换配置
        words = new HashMap<>();
        Map<String, Object> wordsConfig = Objects.requireNonNull(
                YamlConfiguration.loadConfiguration(new File(getDataFolder(), "words.yml"))
                        .getConfigurationSection("words")).getValues(false);
        for(String groupName : wordsConfig.keySet()){
            List<?> wordList = (List<?>) wordsConfig.get(groupName);
            for(Object _word : wordList){
                Word word = new Word().setConfig((Map<?, ?>) _word);
                // 'groupName.zh_cn.word' or 'groupName..word'
                words.put(groupName +"."+ word.lang +"."+ word.get, word);
            }
        }

        // 处理替换配置表
        list = new ArrayList<>();
        for(Map<?, ?> li : getConfig().getMapList("list")){
            list.add(new Cli().setConfig(li));
        }
        listSize = list.size();

        return Math.round((System.nanoTime() - _startTime) / 1_000_000.0);
    }


    @Override // 指令补全
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if(args.length == 1){
            List<String> list = new ArrayList<>();
            list.add("json"); // 测试json字符串
            list.add("reload"); // 重载配置
            list.add("debug"); // 调试模式
            return list;
        }
        if(args.length == 2 && args[0].equals("debug")){
            List<String> list = new ArrayList<>();
            list.add("0 - 关闭调试");
            list.add("1 - 显示捕获信息");
            list.add("2 - 显示捕获内容");
            list.add("3 - 显示替换内容");
            list.add("4 - 显示过程");
            return list;
        }
        return null;
    }
    @Override // 执行指令
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        // 默认输出插件信息
        if(args.length == 0){
            sender.sendMessage(
                    "\n"+
                            "IpacEL > UseTranslatedNames: 使用翻译名称\n"+
                            "  指令:\n"+
                            "    - /utn json <JSON>     - 测试 JSON 字符串\n"+
                            "    - /utn reload          - 重载配置\n"+
                            "    - /utn debug [Level]   - 调试模式\n"+
                            "  统计信息:\n"+
                            "    - 监听消息: "+ status.Messages +"\n"+
                            "    - 成功匹配: "+ status.Matches +"\n"+
                            "    - 平均延迟: "+ String.format("%.2f", ((float) status.TotalTime / status.Messages)) +" ms  [累计: "+ String.format("%.2f", status.TotalTime) +" ms]\n"+
                            "  调试信息:\n"+
                            "    - duplicateMessageSize: "+ duplicateMessage.size()
            );
            return true;
        }

        // 测试 json 消息
        else if(args[0].equals("json")){
            // 获取消息. 并删除前面的 "json "
            String json = String.join(" ", args).substring(5);
            sender.sendMessage("[UTN] JSON 消息测试: [Length: "+ json.length() +"]");
            sender.spigot().sendMessage(ComponentSerializer.parse(json));
            sender.sendMessage(" ");
            return true;
        }

        // 重载配置
        else if(args[0].equals("reload")){
            long time = loadConfig();
            sender.sendMessage("[UTN] 已完成重载, 耗时: "+ time +" 毫秒");
            return true;
        }

        // 调试模式
        else if(args[0].equals("debug")){
            if(args.length > 1){
                _debug = Integer.parseInt(args[1]);
                if(_debug > 4 || _debug < 0){
                    _debug = 2;
                }
            }else{
                _debug = _debug == 0 ? 2 : 0;
            }
            sender.sendMessage("[UTN] 调试信息等级: "+ _debug);
            return true;
        }

        // 返回 false 时, 玩家将收到命令不存在的错误
        return false;
    }
}
