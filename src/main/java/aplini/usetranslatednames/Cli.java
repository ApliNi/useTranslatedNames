package aplini.usetranslatednames;

import java.util.Map;

import static aplini.usetranslatednames.Util.SEL;

public class Cli {
    public int inspectLength = 9999999;
    public String get;
    public String set;
    public String displayPlace;

    // 从 getConfig 中转换数据
    public Cli setConfig(Map<?, ?> li){
        // 必选变量
        this.inspectLength = (int) li.get("inspectLength");
        this.get = (String) li.get("get");
        this.set = (String) li.get("set");
        // 可选变量
        this.displayPlace = (String) SEL(li.get("displayPlace"), "");

        return this;
    }
}
