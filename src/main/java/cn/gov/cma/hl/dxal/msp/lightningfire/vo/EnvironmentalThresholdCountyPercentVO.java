package cn.gov.cma.hl.dxal.msp.lightningfire.vo;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 单个区县在所选日期（或该日期所在周期）的达到条件百分比。
 *
 * <p>值为该县 {@code {因子}_hit_pct}，已经是 0—100 的百分比（前端直接加 % 显示）；
 * 该县当天没有判定结果（不在 5—9 月研究应用范围、没有含林格网）时为 null，不补 0。</p>
 *
 * <p>粒度不是 DAY 时为该县在周期内有效日比例的平均值，validDays 为参与平均的有效日数
 * （DAY 粒度为 1 或 0）。</p>
 */
@Schema(description = "区县达到条件百分比")
public record EnvironmentalThresholdCountyPercentVO(
        @Schema(description = "6 位行政区划代码", example = "232722")
        Integer code,
        @Schema(description = "区县名称", example = "塔河县")
        String name,
        @Schema(description = "达到条件百分比（0—100，前端直接加 % 显示）；无数据时为 null", example = "100.0")
        Double percent,
        @Schema(description = "参与平均的有效日数", example = "1")
        Long validDays) {
}
