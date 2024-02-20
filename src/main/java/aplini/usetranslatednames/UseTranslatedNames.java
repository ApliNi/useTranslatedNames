package aplini.usetranslatednames;

import aplini.usetranslatednames.Enum.Cli;
import aplini.usetranslatednames.Enum.Key;
import aplini.usetranslatednames.Enum.Status;
import aplini.usetranslatednames.Enum.Word;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.chat.ComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.*;
import java.util.regex.Matcher;

import static aplini.usetranslatednames.Util.SEL;
import static aplini.usetranslatednames.Util.toTranslatedName;


public final class UseTranslatedNames extends JavaPlugin implements CommandExecutor, TabExecutor, Listener {

    public static UseTranslatedNames plugin;

    // 用户配置版本
    public static int _configVersion;
    // 解析器模式
    Key parser;
    // 序列化输入 JSON
    Key serializedInput;

    // 词配置 Map<组名.词 or 组名.语言.词, 词配置>
    private HashMap<String, Word> words;
    // 配置文件
    List<Cli> list;
    int listSize = 0;

    // 调试模式
    public static int _debug = 0;

    // 记录统计信息
    static Status status = new Status();

    // 用于静默发送聊天消息, 绕过消息监听器
    public static ProtocolManager protocolManager;


    @Override
    public void onEnable() {
        plugin = this;
        loadConfig();
        Util.load(this);

        // bStats
        // 收集一些性能数据和消息总数
        Metrics metrics = new Metrics(this, 20766);
        // 平均消息延迟
        metrics.addCustomChart(new Metrics.SimplePie("AverageTime", () -> {
            double TotalTime = status.TotalTime / 1_000_000.0;
            double AverageTime = TotalTime / status.MsgCount;
            return String.format("%.2f", AverageTime) +"ms";
        }));
        // 其他统计信息
//        metrics.addCustomChart(new Metrics.MultiLineChart("Status", () -> {
//            Map<String, Integer> map = new HashMap<>();
//            map.put("NumberOfMessages", Math.toIntExact(status.MsgCount));
//            map.put("NumberOfMatches", Math.toIntExact(status.MatchesCount));
//            map.put("NumberOfTotalTime", Math.toIntExact(Math.round(status.TotalTime / 1e9)));
//            return map;
//        }));
        metrics.addCustomChart(new Metrics.SingleLineChart("MsgCount", () -> Math.toIntExact(status.MsgCount)));
        metrics.addCustomChart(new Metrics.SingleLineChart("MatchesCount", () -> Math.toIntExact(status.MatchesCount)));
        metrics.addCustomChart(new Metrics.SingleLineChart("TotalTime", () -> Math.toIntExact(Math.round(status.TotalTime / 1e9))));

        // 注册指令
        Objects.requireNonNull(getCommand("utn")).setExecutor(this);

        // 用于静默发送聊天消息, 绕过消息监听器
        protocolManager = ProtocolLibrary.getProtocolManager();

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
                String json = serializationJson(switch(parser){
                    // 1.20.4 +
                    case ChatComponents -> event.getPacket().getChatComponents().readSafely(0).getJson();
                    // 1.20.4 -
                    case GetStrings -> event.getPacket().getStrings().readSafely(0);
                    //
                    default -> null;
                });
                if(json == null){return;}

                // 处理调试信息
                if(_debug >= 1){
                    getLogger().info("");
                    getLogger().info("");
                    getLogger().info("[DEBUG] [Player: "+ event.getPlayer().getName() +", Lang: "+ player.getLocale() +"] [Length: "+ json.length() +"]");
                    if(_debug >= 2){
                        getLogger().info("  - [GET]: "+ json);
                    }
                }

                // 运行字符串替换
                runJsonTestReplaceAll(event, player, json);

                if(_debug >= 2){
                    getLogger().info("  - [Time: "+ String.format("%.3f", (System.nanoTime() - _startTime) / 1_000_000.0) +" ms]");
                }

                // 记录运行时间
                status.MsgCount++;
                status.TotalTime += System.nanoTime() - _startTime;
            }
        });

        getLogger().info("UseTranslatedNames 已启动");
    }

    // 遍历所有替换配置, 直到 runJsonTestReplaceConfig 返回 true
    public void runJsonTestReplaceAll(PacketEvent event, Player player, String jsonTest){
        // 遍历替换配置
        for(int i = 0; i < listSize; i++){
            // 替换完成后退出循环, 否则继续
            if(runJsonTestReplaceConfig(event, player, jsonTest, i)){
                break;
            }
        }
    }

    /**
     * 处理字符串替换
     * @param event     PacketEvent
     * @param player    接收消息的玩家
     * @param jsonTest  即将发送的 JSON 字符串
     * @param configIndex 处理这条消息的配置所在的位置
     * @return true = 完成替换, 无需继续循环. false = 不匹配
     */
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

        // 启用权限时, 玩家没有该权限则不进行替换
        if(cli.permissionEn && !player.hasPermission(cli.permission)){
            return false;
        }

        // 防止处理过长或过短的消息
        if(jsonTest.length() > cli.inspectLengthMax || jsonTest.length() < cli.inspectLengthMin){
            return false;
        }

        // 匹配
        Matcher matcher = cli.regExp.matcher(jsonTest);
        while(matcher.find()){

            status.MatchesCount++;

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
//            BaseComponent[] message = ComponentSerializer.parse(jsonFrame);

            // 调试
            if(_debug >= 3){
                getLogger().info("  - [SET]: "+ serializationJson(jsonFrame));
            }

            // 处理显示对象
            switch(cli.displayObjectData){

                case DEFAULT: // 默认方式处理
                    sendJsonMessage(cli, player, jsonFrame);
                    break;

                case REG_VAR: // 消息仅发送给匹配到的玩家名称, 其他玩家不会收到消息
                    if(player.getName().equalsIgnoreCase(matcher.group(cli.displayObjectRegVarId))){
                        sendJsonMessage(cli, player, jsonFrame);
                    }
                    break;

                case ALL: // 将消息广播给所有玩家. 仅限于只有自己能收到消息的情况
                    for(Player tp : Bukkit.getOnlinePlayers()){
                        sendJsonMessage(cli, tp, jsonFrame);
                    }
                    break;

                case EXCLUDE: // 将消息广播给所有玩家, 但不包括自己. 仅限于只有自己能收到消息的情况
                    for(Player tp : Bukkit.getOnlinePlayers()){
                        if(player == tp){continue;}
                        sendJsonMessage(cli, tp, jsonFrame);
                    }
                    break;

                case CONSOLE: // 将消息转发到控制台, 自己不会收到
                    getLogger().info("["+ player.getName() +"]:");
                    Bukkit.getConsoleSender().spigot().sendMessage(ComponentSerializer.parse(jsonFrame));
                    break;

                case COPY_TO_CONSOLE: // 将消息复制到控制台
                    getLogger().info("["+ player.getName() +"]:");
                    Bukkit.getConsoleSender().spigot().sendMessage(ComponentSerializer.parse(jsonFrame));
                    sendJsonMessage(cli, player, jsonFrame);
                    break;
            }
            forOK = true;
        }
        return forOK;
    }

    // 将消息显示给玩家
    public void sendJsonMessage(Cli cli, Player player, String json){
        // 处理 "显示位置"
        if(cli.displayPlaceData == Key.ACTION_BAR){
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, ComponentSerializer.parse(json));
        }else{
            switch(parser){
                // 1.20.4 +
                case ChatComponents -> {
                    // 使用 protocolLib 静默发送, 防止重复处理聊天消息
                    PacketContainer chatPacket = protocolManager.createPacket(PacketType.Play.Server.SYSTEM_CHAT);
                    chatPacket.getChatComponents().write(0, WrappedChatComponent.fromJson(json));
                    protocolManager.sendServerPacket(player, chatPacket, false);
                }
                // 1.20.4 -
                case GetStrings -> {
                    player.spigot().sendMessage(ComponentSerializer.parse(json));
                }
                //
                default -> {}
            };
        }
    }

    // 序列化 JSON 消息
    // null  = 出错, 抛弃这条消息; 字符串 = 正常处理
    public String serializationJson(String inp){
        switch (serializedInput){

            // 使用 protocolLib 创建新数据包再解析, 这没有意义
            case CreatePacket -> {
                PacketContainer chatPacket = protocolManager.createPacket(PacketType.Play.Server.SYSTEM_CHAT);
                chatPacket.getChatComponents().write(0, WrappedChatComponent.fromJson(inp));
                return chatPacket.getChatComponents().readSafely(0).getJson();
            }

            // 这会使 JSON 内部的顺序发生变化, 并可能丢失部分原版消息
            case ComponentSerializer -> {
                try {
                    // 序列化消息
                    return ComponentSerializer.toString(ComponentSerializer.parse(inp));
                } catch (Exception e) {
                    if(_debug >= 4){
                        getLogger().warning("[DEBUG] 消息序列化错误");
                        throw new RuntimeException(e);
                    }
                    return null;
                }
            }

            // 不进行序列化
            case NULL -> {
                return inp;
            }
            default -> {
                return inp;
            }
        }
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

        // 解析器设置
        switch(getConfig().getString("dev.parser", "")){
            case "ChatComponents" -> parser = Key.ChatComponents;
            case "GetStrings" -> parser = Key.GetStrings;
            default -> {
                // 兼容旧配置
                getLogger().warning("缺少配置选项 `dev.parser` 或存在已弃用的配置 `dev.listeningMode`");
                if(getConfig().getBoolean("dev.listeningMode", true)){
                    parser = Key.ChatComponents;
                }else{
                    parser = Key.GetStrings;
                }
            }
        }

        // 消息序列化设置
        serializedInput = switch(getConfig().getString("dev.serializedInput", "")){
            case "CreatePacket" -> Key.CreatePacket;
            case "ComponentSerializer" -> Key.ComponentSerializer;
            default -> Key.NULL;
        };

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

        // 配置玩家消息监听器
        onPlayerChat.mode = switch(getConfig().getString("dev.convertPlayerMessages", "NONE")){
            case "Convert" -> Key.Convert;
            case "ConvertBypass" -> Key.ConvertBypass;
            default -> Key.NONE;
        };
        // 注册和注销监听器
        if(onPlayerChat.mode == Key.NONE){
            if(onPlayerChat.func != null){
                HandlerList.unregisterAll(onPlayerChat.func);
                onPlayerChat.func = null;
            }
        }else{
            if(onPlayerChat.func == null){
                onPlayerChat.func = new onPlayerChat();
                getServer().getPluginManager().registerEvents(onPlayerChat.func, this);
            }
        }

        return Math.round((System.nanoTime() - _startTime) / 1_000_000.0);
    }


    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event){
        String message = event.getMessage();

        // 在这里处理玩家发送的消息
        // 你可以根据需要进行逻辑处理或条件检查

        // 示例：将玩家发送的消息改为大写
        String upperCaseMessage = message.toUpperCase();
        event.setMessage(upperCaseMessage);
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
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args){

        double TotalTime = status.TotalTime / 1_000_000.0;
        double AverageTime = TotalTime / status.MsgCount;

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
                            "    - 监听消息: "+ status.MsgCount +"\n"+
                            "    - 成功匹配: "+ status.MatchesCount +"\n"+
                            "    - 平均延迟: "+ String.format("%.2f", AverageTime) +" ms  [累计: "+ String.format("%.2f", TotalTime) +" ms]\n"
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
