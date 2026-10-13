package cn.gov.cma.hl.dxal.msp.lightningfire.vo;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 规则在 2024 年完整格网 holdout 上的检验结果。
 *
 * <p>数据来自交接包的 结果/阈值汇总.csv 的 2024 列与 固定阈值参数与检验.json 的 holdout 节点
 * （两者同源同值，已提取为 {@link cn.gov.cma.hl.dxal.msp.lightningfire.constant.FixedThreshold}）。
 * 前四项是格日计数，后五项是比例：pod、fpr、far、csi、j 均为 0—1（前端乘 100 显示百分比）。</p>
 *
 * <p>这些是「历史回看」的模型检验指标，不是所选县当天的准确率，也不是起火概率：
 * pod 是 2024 年有火记录格日中符合该条件的比例，fpr 是无火记录格日中也符合该条件的比例，
 * j = pod − fpr（约登指数），值越接近 0 说明条件越没有区分能力。</p>
 */
@Schema(description = "固定阈值规则的 2024 年检验结果（历史回看，不是当天准确率）")
public record EnvironmentalThresholdMetricsVO(
        @Schema(description = "命中格日数（有火且符合条件）", example = "30.0")
        Double tp,
        @Schema(description = "漏报格日数（有火但不符合条件）", example = "2.0")
        Double fn,
        @Schema(description = "空报格日数（无火但符合条件）", example = "9861399.0")
        Double fp,
        @Schema(description = "正确否定格日数（无火且不符合条件）", example = "1086276.0")
        Double tn,
        @Schema(description = "有火记录中符合该条件的比例（0—1，显示时乘 100）", example = "0.9375")
        Double pod,
        @Schema(description = "符合条件中实际无火的比例（0—1，显示时乘 100）", example = "0.9999969578445477")
        Double far,
        @Schema(description = "无火记录中也符合该条件的比例（0—1，显示时乘 100）", example = "0.9007756441436195")
        Double fpr,
        @Schema(description = "临界成功指数 CSI（0—1）", example = "3.042154835337792e-06")
        Double csi,
        @Schema(description = "约登指数 J = POD − FPR（−1—1），越接近 0 越没有区分能力", example = "0.03672435585638045")
        Double j) {
}
