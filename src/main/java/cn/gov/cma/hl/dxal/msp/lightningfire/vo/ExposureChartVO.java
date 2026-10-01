package cn.gov.cma.hl.dxal.msp.lightningfire.vo;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 柱状图数据：标题、数值单位与分类项。
 *
 * <p>items 的顺序即为图表 x 轴顺序：类型占比柱状图按森林类型 / 居民地分项 / 分项贡献的固定顺序，
 * 指标均值对比柱状图按数值降序（相同取行政区划代码升序）。</p>
 */
@Schema(description = "柱状图数据")
public record ExposureChartVO(
        @Schema(description = "图表标题", example = "森林类型面积占比")
        String title,
        @Schema(description = "数值单位", example = "km²")
        String unit,
        @Schema(description = "分类项，顺序即 x 轴顺序")
        ExposureChartItemVO[] items) {
}
