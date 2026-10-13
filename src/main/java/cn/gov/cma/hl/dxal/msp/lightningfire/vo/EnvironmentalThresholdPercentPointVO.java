package cn.gov.cma.hl.dxal.msp.lightningfire.vo;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 条件百分比折线图上的一个时间点。
 *
 * <p>period 的格式与粒度一致（与 /ltg-env/time-axis/lightning-fire 的时间轴口径相同）：
 * 天 yyyy-MM-dd、月 yyyy-MM、年 yyyy。</p>
 *
 * <p>percent 为该周期内「有效日区域比例」的均值（见 {@link EnvironmentalThresholdPercentTrendVO}）：
 * 天粒度就是当天值，月 / 年粒度为周期内有数据日的均值；该周期没有有效日时 percent 为 null
 * （缺测断线，不补 0）。validDays 为该周期参与平均的有效日数（天粒度为 1 或 0）。</p>
 */
@Schema(description = "条件百分比折线图时间点")
public record EnvironmentalThresholdPercentPointVO(
        @Schema(description = "时间标签：天 yyyy-MM-dd、月 yyyy-MM、年 yyyy", example = "2024-08-20")
        String period,
        @Schema(description = "该时间的达到条件百分比（0—100，前端直接加 % 显示）；无有效日时为 null",
                example = "87.53")
        Double percent,
        @Schema(description = "该周期参与平均的有效日数（有数据且可判断格网数大于 0 的天数）", example = "1")
        Long validDays) {
}
