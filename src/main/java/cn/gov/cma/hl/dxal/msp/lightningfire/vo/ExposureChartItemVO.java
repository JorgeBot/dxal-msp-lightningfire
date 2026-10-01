package cn.gov.cma.hl.dxal.msp.lightningfire.vo;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 柱状图分类项。
 *
 * <p>value 的单位见所属图表的 unit，已按展示口径换算（森林覆盖暴露度、居民地面占比乘 100 记 %）；
 * percent 仅在「类型占比柱状图」的森林类型与居民地分项上有值，其余为 null。</p>
 */
@Schema(description = "柱状图分类项")
public record ExposureChartItemVO(
        @Schema(description = "分类代码；区县对比柱状图为行政区划代码，类型占比柱状图为 null", example = "232701")
        String code,
        @Schema(description = "分类名称", example = "漠河市")
        String name,
        @Schema(description = "数值，单位见所属图表的 unit", example = "31.97")
        Double value,
        @Schema(description = "占比（%）；不适用或分母为 0 时为 null", example = "26.42")
        Double percent) {
}
