package cn.gov.cma.hl.dxal.msp.lightningfire.vo;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 固定阈值规则：所选环境因子的条件界线与检验结果。
 *
 * <p>数据来自交接包提取的常量 {@link cn.gov.cma.hl.dxal.msp.lightningfire.constant.FixedThreshold}
 * （来源：结果/阈值汇总.csv 的九条单因子条件 + 固定阈值参数与检验.json 的 combined 节点），
 * 接口不读 CSV/JSON，也不重新训练。</p>
 *
 * <p>两种情况：</p>
 * <ul>
 *   <li>单因子（九个）：{@code threshold}、{@code unit}、{@code ciLower}、{@code ciUpper} 有值，
 *       {@code paths} 为 null——一条线就是一个方向加一个阈值；</li>
 *   <li>{@code combined}（组合环境条件）：{@code threshold} 等四项为 null，{@code paths} 为三条路径，
 *       路径内条件「且」、路径间「或」；组合不能当成单个阈值展示。</li>
 * </ul>
 *
 * <p>阈值只适用于 5—9 月研究应用范围（冬季没有正例支撑），是参考条件而不是报警线：
 * 单因子区分能力弱，例如降水大面积 100% 符合完全可能，"百分比" 不是起火概率。</p>
 */
@Schema(description = "环境影响阈值·固定阈值规则")
public record EnvironmentalThresholdRuleVO(
        @Schema(description = "环境因子 id", example = "rain")
        String factor,
        @Schema(description = "环境因子名称", example = "日降水")
        String factorName,
        @Schema(description = "成果状态：RESEARCH=研究口径（可作参考条件）、"
                + "WEAK_OR_UNSUPPORTED=弱或不支持（不能当报警线）", example = "WEAK_OR_UNSUPPORTED")
        String status,
        @Schema(description = "比较方向取值：ge=≥、gt=>、le=≤；组合环境条件为 null", example = "le")
        String operator,
        @Schema(description = "比较方向符号，与 operator 一一对应；组合环境条件为 null", example = "≤")
        String operatorSymbol,
        @Schema(description = "阈值（完整值，判断用完整值，展示可四舍五入）；组合环境条件为 null",
                example = "8.04280460357666")
        Double threshold,
        @Schema(description = "阈值单位；组合环境条件为 null", example = "mm/d")
        String unit,
        @Schema(description = "阈值 95% 置信区间下限；组合环境条件为 null", example = "1.0853916692733765")
        Double ciLower,
        @Schema(description = "阈值 95% 置信区间上限；组合环境条件为 null", example = "13.578001194953888")
        Double ciUpper,
        @Schema(description = "2024 年完整格网 holdout 检验结果（历史回看）")
        EnvironmentalThresholdMetricsVO metrics2024,
        @Schema(description = "组合环境条件的路径集合（组内且、组间或）；单因子规则为 null")
        EnvironmentalThresholdPathVO[] paths) {
}
