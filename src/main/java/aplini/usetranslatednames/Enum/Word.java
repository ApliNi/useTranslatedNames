package aplini.usetranslatednames.Enum;

import java.util.Map;

import static aplini.usetranslatednames.Util.SEL;

public class Word {
    // 必选变量
    public String get;
    public String set;
    // 可选变量
    public String lang;


    // 从 Map 中转换数据
    public Word setConfig(Map<?, ?> li){
        // 必选变量
        this.get = (String) li.get("get");
        this.set = (String) li.get("set");
        // 可选变量
        this.lang = (String) SEL(li.get("lang"), "");

        return this;
    }
}
