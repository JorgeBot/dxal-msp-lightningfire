package cn.gov.cma.hl.dxal.msp.lightningfire.vo;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 综合风险评分区间：按 0—100 等宽 20 分分档的连续值区间统计。
 *
 * <p>分档边界为 [0,20,40,60,80,100]：前四档左闭右开，末档含上界 100。
 * 区间下界与上界均在加权计算之后才取整，界面按「0—20」形式的整数区间展示。
 * 等宽 20 分档是连续评分的展示统计，不是由参考分位冻结的风险等级阈值。</p>
 */
@Schema(description = "综合风险评分区间")
public record RiskAssessmentBinVO(
        @Schema(description = "区间标签", example = "0—20")
        String label,
        @Schema(description = "区间下界（含）", example = "0.0")
        Double lowerBound,
        @Schema(description = "区间上界；末档含上界，其余为不含", example = "20.0")
        Double upperBound,
        @Schema(description = "落在该区间的有记录日数（全省口径为地市有记录日数合计）；无记录为 0", example = "126")
        Long recordDays,
        @Schema(description = "该区间记录日数 ÷ 有记录日数 × 100（%）；分母为 0 时为 null", example = "34.62")
        Double share) {
}
