package aplini.usetranslatednames.Enum;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static aplini.usetranslatednames.Util.SEL;

public class Cli {
    // 必选变量
    public int inspectLength;
    public String get;
    public Pattern regExp;
    public String set;
    // 可选变量
    public String permission;
    public String displayPlace;


    // 这个配置启用了哪些功能
    public boolean enTransVar = false;    // 翻译功能
    public Matcher dataTransVar;

    public boolean enWordReplace = false;    // 词替换
    public Matcher dataWordReplace;

    public boolean enRegExpReplace = false;     // 正则表达式替换


    // 从 getConfig 中转换数据
    public Cli setConfig(Map<?, ?> li){
        // 必选变量
        this.inspectLength = (int) li.get("inspectLength");
        this.get = (String) li.get("get");
        this.regExp = Pattern.compile(this.get);
        this.set = (String) li.get("set");
        // 可选变量
        this.permission = (String) SEL(li.get("permission"), "");
        this.displayPlace = (String) SEL(li.get("displayPlace"), "");


        // 这个配置启用了哪些功能

        // 翻译功能
        this.dataTransVar = Pattern.compile("_\\$(\\d+):(TranslatedName|ItemType)_").matcher(this.set);
        this.enTransVar = this.dataTransVar.find();

        // 词替换
        this.dataWordReplace = Pattern.compile("_\\$(\\d+):Words:([^_]+)_").matcher(this.set);
        this.enWordReplace = this.dataWordReplace.find();

        // 正则表达式替换
        this.enRegExpReplace = Pattern.compile("_\\$\\d+_").matcher(this.set).find();

        return this;
    }
}
