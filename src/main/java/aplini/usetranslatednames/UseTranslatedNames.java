package aplini.usetranslatednames;

import aplini.usetranslatednames.Enum.Cli;
import aplini.usetranslatednames.Enum.Status;
import aplini.usetranslatednames.Enum.Word;
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
    // 调试模式
    private boolean _debug = false;
    // 监听器模式
    boolean listeningMode = true;
    // 词配置 Map<组名.词 or 组名.语言.词, 词配置>
    private HashMap<String, Word> words;
    // 配置文件
    List<Cli> list = new ArrayList<>();
    // 记录统计信息
    Status status = new Status();

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
                status.Messages ++;

                Player player = event.getPlayer();

                // 获取消息 JSON
                String json;
                if(listeningMode){
                    // 1.20.4 +
                    json = event.getPacket().getChatComponents().read(0).getJson();
                }else{
                    // 1.20.4 -
                    json = event.getPacket().getStrings().read(0);
                }

                if(json == null) return;

                if(_debug){
                    getLogger().info("[DEBUG] [Player: "+ event.getPlayer().getName() +" (Lang: "+ player.getLocale() +")] [Length: "+ json.length() +"]: -------");
                    getLogger().info("  - [get]: "+ json);
                }

                // 遍历替换配置
                for(Cli cli : list){
                    boolean ok = false;

                    // 如果权限不为空 且 玩家没有该权限, 则不进行替换
                    if(!cli.permission.isEmpty() && !player.hasPermission(cli.permission)){
                        continue;
                    }

                    // 防止处理过长的消息
                    if(json.length() > cli.inspectLength){
                        continue;
                    }

                    // 匹配
                    Matcher matcher = cli.regExp.matcher(json);
                    while(matcher.find()){

                        status.Matches ++;

                        // 取消发送消息
                        event.setCancelled(true);

                        // 匹配到的完整字符串
                        String jsonFrame = cli.set;
                        if(jsonFrame.isEmpty()){ // 如果为空则仅取消发送它
                            ok = true;
                            break;
                        }
                        String oldJson = matcher.group(0);


                        // 处理翻译变量替换 _$1:ItemType_, _$1:TranslatedName_
                        if(cli.enTransVar){
                            cli.dataTransVar.reset();
                            while(cli.dataTransVar.find()){
                                String var = matcher.group(Integer.parseInt(cli.dataTransVar.group(1)));
                                if(cli.dataTransVar.group(2).equals("TranslatedName")){
                                    jsonFrame = jsonFrame.replace(cli.dataTransVar.group(), toTranslatedName(var).transName);
                                }else{
                                    jsonFrame = jsonFrame.replace(cli.dataTransVar.group(), toTranslatedName(var).itemType);
                                }
                            }
                        }


                        // 处理词替换 _$1:Words:xxx_
                        if(cli.enWordReplace){
                            cli.dataWordReplace.reset();
                            while(cli.dataWordReplace.find()){
                                String var = matcher.group(Integer.parseInt(cli.dataWordReplace.group(1)));
                                Word word = (Word) SEL(
                                        words.get(cli.dataWordReplace.group(2) +"."+ player.getLocale() +"."+ var),
                                        words.get(cli.dataWordReplace.group(2) +".."+ var));
                                if(word != null){
                                    jsonFrame = jsonFrame.replace(cli.dataWordReplace.group(), word.set);
                                }
                            }
                        }


                        // 处理正则替换 _$1_
                        if(cli.enRegExpReplace){
                            int matcherLength = matcher.groupCount();
                            for(int i = 1; i <= matcherLength; i++){
                                jsonFrame = jsonFrame.replace("_$" + i + "_", matcher.group(i));
                            }
                        }


                        // 替换原文本中的旧 JSON, 重新发送给玩家
                        jsonFrame = json.replace(oldJson, jsonFrame);
                        if(_debug){
                            getLogger().info("  - [set]: "+ jsonFrame);
                        }
                        // 处理显示位置
                        if(cli.displayPlace.equals("ACTION_BAR")){
                            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, ComponentSerializer.parse(jsonFrame));
                        } else {
                            player.spigot().sendMessage(ComponentSerializer.parse(jsonFrame));
                        }
                        ok = true;
                    }
                    if(ok){break;}
                }

                status.TotalTime += Math.round((System.nanoTime() - _startTime) / 1_000_000.0);
            }
        });

        getLogger().info("UseTranslatedNames 已启动");
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
        if(getConfig().getInt("configVersion") != 3){
            getLogger().warning("配置版本不匹配, 可能无法正常运行, 请更新或重建配置");
            getLogger().warning("配置版本不匹配, 可能无法正常运行, 请更新或重建配置");
            getLogger().warning("配置版本不匹配, 可能无法正常运行, 请更新或重建配置");
        }

        // 监听器模式
        listeningMode = getConfig().getBoolean("dev.listeningMode");

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

        return Math.round((System.nanoTime() - _startTime) / 1_000_000.0);
    }


    @Override // 指令补全
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if(args.length == 1){
            List<String> list = new ArrayList<>();
            list.add("reload"); // 重载配置
            list.add("debug"); // 调试模式
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
                            "  指令: \n"+
                            "    - /utn reload - 重载配置\n"+
                            "    - /utn debug  - 调试模式\n"+
                            "  统计信息: \n"+
                            "    - 监听消息: "+ status.Messages +"\n"+
                            "    - 成功匹配: "+ status.Matches +"\n"+
                            "    - 平均延迟: "+ String.format("%.2f", ((float) status.Matches / status.TotalTime)) +" ms  [累计: "+ status.TotalTime +" ms]"
            );
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
            _debug = ! _debug;
            sender.sendMessage("[UTN] 调试模式: "+ _debug);
            return true;
        }

        // 返回 false 时, 玩家将收到命令不存在的错误
        return false;
    }
}
