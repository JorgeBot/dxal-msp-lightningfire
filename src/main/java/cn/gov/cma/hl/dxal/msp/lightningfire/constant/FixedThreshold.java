package cn.gov.cma.hl.dxal.msp.lightningfire.constant;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 功能二「固定阈值」的阈值成果常量：把交接包 {@code 功能二_固定阈值/结果} 下的
 * 阈值汇总.csv（9 个单因子）与 固定阈值参数与检验.json 的 {@code combined} 节点（组合环境条件）
 * 提取到项目里，接口按因子直接查表返回，不在运行时读 CSV/JSON。
 *
 * <p>成果版本 {@value #VERSION}：比较方向 ge=≥、gt=&gt;、le=≤；阈值为拟合出的完整值，
 * 展示可以四舍五入但判断用完整值；ciLower / ciUpper 为 95% 置信区间；
 * status 中 RESEARCH=研究口径（可作参考条件）、WEAK_OR_UNSUPPORTED=弱或不支持
 * （雷电记录数、雷电平均绝对强度、日降水三条，且 2024 检验 J 很低，不能当报警线用）。</p>
 *
 * <p>metrics2024 为该规则在 2024 完整格网 holdout 上的检验结果（tp/fn/fp/tn 为格日计数，
 * pod/far/fpr/csi/j 为比例，未乘 100，接口返回原值由前端显示百分比）：
 * pod=有火记录中符合条件比例、fpr=无火记录中也符合条件比例、j=pod−fpr。
 * 这些是模型检验指标，不是所选县当天的准确率。</p>
 *
 * <p>组合环境条件不是九条线简单求交：{@link #COMBINED_PATHS} 中每条路径内部是「且」，
 * 路径之间是「或」（浅层树选出的多组条件），因此 {@code combined} 没有单列阈值，
 * 只有路径集合与整体检验指标。所有阈值只适用于 5—9 月研究应用范围，冬季没有正例支撑，
 * 也不代表起火概率。</p>
 */
public final class FixedThreshold {

    /**
     * 成果版本号，取自 固定阈值参数与检验.json 的 version 字段。
     */
    public static final String VERSION = "native-1km-fixed-reviewed-20260930";

    /**
     * 提取来源说明，用于接口文档与排查：常量值改动必须同步这两个文件。
     */
    public static final String SOURCE = "功能二_固定阈值/结果/阈值汇总.csv（9 个单因子）"
            + " + 固定阈值参数与检验.json 的 combined 节点（组合环境条件）";

    /**
     * 组合环境条件的因子 id。
     */
    public static final String COMBINED_FACTOR = "combined";

    /**
     * 组合环境条件的展示名称（下拉框与接口统一口径）。
     */
    public static final String COMBINED_NAME = "组合环境条件";

    /**
     * 条件比较方向：code 与成果文件一致，symbol 为展示符号。
     */
    public enum Direction {

        GE("ge", "≥"),
        GT("gt", ">"),
        LE("le", "≤"),
        ;

        private final String code;

        private final String symbol;

        Direction(String code, String symbol) {
            this.code = code;
            this.symbol = symbol;
        }

        public String getCode() {
            return code;
        }

        public String getSymbol() {
            return symbol;
        }

        /**
         * 按成果文件的取值（ge / gt / le）反查方向；未知取值返回 null。
         *
         * <p>动态阈值的年度候选也可能给出与基线不同的方向（如风速 2019、2020 年为 ge），
         * 接口按本方法统一转成符号展示。</p>
         */
        public static Direction fromCode(String code) {
            for (Direction direction : values()) {
                if (direction.code.equals(code)) {
                    return direction;
                }
            }
            return null;
        }
    }

    /**
     * 2024 年完整格网 holdout 检验结果：前四项为格日计数，后五项为比例（未乘 100）。
     */
    public record Metrics(double tp, double fn, double fp, double tn, double pod, double far, double fpr,
                          double csi, double j) {
    }

    /**
     * 单因子固定阈值行：因子 id 与名称、方向、阈值、单位、95% 置信区间、状态与 2024 检验结果。
     */
    public record Rule(String factor, String name, Direction direction, double threshold, String unit,
                       double ciLower, double ciUpper, String status, Metrics metrics2024) {
    }

    /**
     * 组合条件的一条判定条件：组内「且」的单个因子条件。
     */
    public record Condition(String factor, String factorName, Direction direction, double threshold, String unit) {
    }

    /**
     * 组合条件的一条路径：leaf 为源树叶子编号，路径内条件为「且」，路径之间为「或」。
     */
    public record CombinedPath(int leaf, List<Condition> conditions) {
    }

    /**
     * 九个单因子的固定阈值行，顺序与 阈值汇总.csv 及下拉框一致（不含 combined）。
     */
    public static final List<Rule> RULES = List.of(
            // ===== 单因子固定阈值：逐行来自 结果/阈值汇总.csv，因子 id 由 结果/因子字典.csv 对照 =====
            new Rule("lightning_count", "雷电记录数", Direction.GE, 1.0, "次/1km格日", 1.0, 1.0,
                    "WEAK_OR_UNSUPPORTED",
                    new Metrics(1.0, 31.0, 15016.0, 10932659.0, 0.03125, 0.9999334088033562, 0.00137161543432738, 6.645401382243488e-05, 0.02987838456567262)),
            new Rule("mean_abs_intensity", "雷电平均绝对强度", Direction.GE, 20.206000671386718, "源值（单位未确认）", 20.206000671386718, 26.891999588012702,
                    "WEAK_OR_UNSUPPORTED",
                    new Metrics(0.0, 32.0, 11094.0, 10936581.0, 0.0, 1.0, 0.0010133658516534333, 0.0, -0.0010133658516534333)),
            new Rule("temperature", "气温", Direction.GE, 13.988782787322998, "℃", 10.182234191894532, 22.4949951171875,
                    "RESEARCH",
                    new Metrics(26.0, 6.0, 6195355.0, 4752320.0, 0.8125, 0.9999958033250901, 0.565906002872756, 4.1966708455823665e-06, 0.24659399712724395)),
            new Rule("wind_speed", "风速", Direction.LE, 3.2149999141693115, "m/s", 1.752500057220459, 3.3234594821929933,
                    "RESEARCH",
                    new Metrics(31.0, 1.0, 8831758.0, 2115917.0, 0.96875, 0.9999964899523754, 0.8067245328345973, 3.5100472271193043e-06, 0.16202546716540267)),
            new Rule("relative_humidity", "相对湿度", Direction.LE, 74.5587075805664, "%", 65.90238800048829, 83.26909746170043,
                    "RESEARCH",
                    new Metrics(23.0, 9.0, 5839348.0, 5108327.0, 0.71875, 0.9999960612196074, 0.5333870433676557, 3.938774321931438e-06, 0.18536295663234426)),
            new Rule("rain", "日降水", Direction.LE, 8.04280460357666, "mm/d", 1.0853916692733765, 13.578001194953888,
                    "WEAK_OR_UNSUPPORTED",
                    new Metrics(30.0, 2.0, 9861399.0, 1086276.0, 0.9375, 0.9999969578445477, 0.9007756441436195, 3.042154835337792e-06, 0.03672435585638045)),
            new Rule("fmc", "可燃物含水率FMC", Direction.LE, 0.7686539959907531, "源值（单位未确认）", 0.755673885345459, 0.8145137739181518,
                    "RESEARCH",
                    new Metrics(20.0, 12.0, 5861210.0, 5086465.0, 0.625, 0.9999965877469404, 0.5353839970587362, 3.412246073443137e-06, 0.08961600294126382)),
            new Rule("dem", "高程", Direction.GE, 503.6494616699219, "m", 424.5829772949219, 619.8527130126948,
                    "RESEARCH",
                    new Metrics(26.0, 6.0, 6078089.0, 4869586.0, 0.8125, 0.9999957223580008, 0.5551945047692775, 4.277637776543113e-06, 0.25730549523072255)),
            new Rule("slope", "坡度", Direction.GE, 2.8018367290496826, "°", 1.9075766801834106, 3.718158483505249,
                    "RESEARCH",
                    new Metrics(19.0, 13.0, 4144163.0, 6803512.0, 0.59375, 0.9999954152592719, 0.37854274994462295, 4.584726346129948e-06, 0.21520725005537705)));

    /**
     * 因子 id → 单因子阈值行；{@code combined} 不在其中（它是路径集合，没有单列阈值）。
     */
    public static final Map<String, Rule> RULE_BY_FACTOR = ruleByFactor();

    /**
     * 组合环境条件的整体状态，取自 JSON 的 combined.status（RESEARCH）。
     */
    public static final String COMBINED_STATUS = "RESEARCH";

    /**
     * 组合环境条件的 2024 完整格网 holdout 检验结果。
     */
    public static final Metrics COMBINED_METRICS_2024 =
            new Metrics(20, 12, 2768573, 8179102, 0.625, 0.9999927761140767, 0.25289141301691914,
                    7.223854612702065E-06, 0.37210858698308086);

    // ===== 组合环境条件（浅层树规则）：来自 固定阈值参数与检验.json 的 combined 节点 =====
    /**
     * 组合环境条件的三条路径：路径内条件为「且」，路径之间为「或」。
     */
    public static final List<CombinedPath> COMBINED_PATHS = List.of(
            new CombinedPath(6, List.of(
                    new Condition("temperature", "气温", Direction.LE, 17.10856819152832, "℃"),
                    new Condition("temperature", "气温", Direction.GT, 14.05312442779541, "℃"),
                    new Condition("relative_humidity", "相对湿度", Direction.LE, 69.88364791870117, "%"))),
            new CombinedPath(10, List.of(
                    new Condition("temperature", "气温", Direction.GT, 17.10856819152832, "℃"),
                    new Condition("relative_humidity", "相对湿度", Direction.LE, 74.81907653808594, "%"),
                    new Condition("wind_speed", "风速", Direction.LE, 3.612941026687622, "m/s"))),
            new CombinedPath(14, List.of(
                    new Condition("temperature", "气温", Direction.GT, 17.10856819152832, "℃"),
                    new Condition("relative_humidity", "相对湿度", Direction.GT, 74.81907653808594, "%"),
                    new Condition("dem", "高程", Direction.GT, 619.3994140625, "m"))));

    private FixedThreshold() {
    }

    /**
     * 按因子 id 建索引，保持 阈值汇总.csv 的行顺序。
     */
    private static Map<String, Rule> ruleByFactor() {
        Map<String, Rule> rules = new LinkedHashMap<>();
        for (Rule rule : RULES) {
            rules.put(rule.factor(), rule);
        }
        return Collections.unmodifiableMap(rules);
    }

    /**
     * 因子 id 对应的单因子阈值行；{@code combined} 或未知因子返回 null。
     */
    public static Rule ruleOf(String factor) {
        return factor == null ? null : RULE_BY_FACTOR.get(factor);
    }
}
