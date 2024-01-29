package aplini.usetranslatednames.Enum;

import java.util.ArrayList;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static aplini.usetranslatednames.UseTranslatedNames._configVersion;
import static aplini.usetranslatednames.UseTranslatedNames.overallFinally;
import static aplini.usetranslatednames.Util.SEL;
import static org.bukkit.Bukkit.getLogger;

public class Cli {
    // 必选配置
    public int inspectLengthMin;
    public int inspectLengthMax;
    public String get;
    public Pattern regExp;
    public String set;

    // 可选配置

    // 权限
    public String permission;

    // 显示位置
    public String displayPlace;

    // 显示对象
    public String displayObject;

    // 继承配置
    public String inherit;
    public Key inheritData;

    // set 参数功能
    public Key setData;

    // 翻译功能
    public boolean transVarEn = false;
    public Matcher transVarData;

    // 词替换
    public boolean wordReplaceEn = false;
    public Matcher wordReplaceData;

    // 正则表达式替换
    public boolean regExpReplaceEn = false;

    // 修改显示位置
    public Key displayPlaceData = Key.DEFAULT;

    // 修改显示对象
    public Key displayObjectData = Key.DEFAULT;



    // 从 getConfig 中转换数据
    public Cli setConfig(Map<?, ?> li){

        if(_configVersion >= 4){
            ArrayList<?> temp = (ArrayList<?>) li.get("inspectLength");
            this.inspectLengthMin = (int) temp.get(0);
            this.inspectLengthMax = (int) temp.get(1);
        }else{
            this.inspectLengthMin = 0;
            this.inspectLengthMax = (int) li.get("inspectLength");
        }

        this.get = (String) li.get("get");
        this.regExp = Pattern.compile(this.get);

        // set 参数功能
        this.set = (String) li.get("set");
        if(this.set.isEmpty()){
            setData = Key.NULL;
        }else if(this.set.equals("_USE_GET_")){
            setData = Key._USE_GET_;
        }

        // 可选配置
        this.permission = (String) SEL(li.get("permission"), "");

        // 继承和其他配置, 将多个配置合并为组同时处理来提高性能
        this.inherit = (String) SEL(li.get("inherit"), "");
        switch(this.inherit){
            // 支持全局 FINALLY
            case "" -> this.inheritData = overallFinally ? Key.FINALLY : Key.NULL;
            case "LINK" -> this.inheritData = Key.LINK;
            case "CLOSE" -> this.inheritData = Key.CLOSE;
            case "FINALLY" -> this.inheritData = Key.FINALLY;
            case "LINK_SER" -> this.inheritData = Key.LINK_SER;
        }

        // 翻译功能
        this.transVarData = Pattern.compile("_\\$(\\d+):(TranslatedName|ItemType)_").matcher(this.set);
        this.transVarEn = this.transVarData.find();

        // 词替换
        this.wordReplaceData = Pattern.compile("_\\$(\\d+):Words:([^_]+)_").matcher(this.set);
        this.wordReplaceEn = this.wordReplaceData.find();

        // 正则表达式替换
        this.regExpReplaceEn = Pattern.compile("_\\$\\d+_").matcher(this.set).find();

        // 修改显示位置
        this.displayPlace = (String) SEL(li.get("displayPlace"), "");
        if(this.displayPlace.equals("ACTION_BAR")){
            this.displayPlaceData = Key.ACTION_BAR;
        }

        // 修改显示对象
        this.displayObject = (String) SEL(li.get("displayObject"), "");
        switch(this.displayObject){
            case "ALL": // 将消息广播给所有玩家. 仅限于只有自己能收到消息的情况
                this.displayObjectData = Key.ALL;
                break;
            case "EXCLUDE": // 将消息广播给所有玩家, 但不包括自己. 仅限于只有自己能收到消息的情况
                this.displayObjectData = Key.EXCLUDE;
                break;
            case "CONSOLE": // 将消息转发到控制台, 自己不会收到
                // 如果启用了 displayPlace = ACTION_BAR, 则发出一个警告
                if(this.displayPlaceData == Key.ACTION_BAR){
                    getLogger().warning("[UTN] 已启用的配置 `displayPlace: ACTION_BAR` 与 `displayObject: CONSOLE` 冲突");
                }
                this.displayObjectData = Key.CONSOLE;
                break;
            case "COPY_TO_CONSOLE": // 将消息复制到控制台
                this.displayObjectData = Key.COPY_TO_CONSOLE;
                break;
        }

        return this;
    }
}
