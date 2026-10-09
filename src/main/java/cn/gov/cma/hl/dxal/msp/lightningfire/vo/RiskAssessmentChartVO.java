package cn.gov.cma.hl.dxal.msp.lightningfire.vo;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 柱状图：标题 + 单位 + 分类项。
 *
 * <p>结构与承载体暴露度（ExposureChartVO）一致，便于前端共用同一套图表组件。
 * 「区县评分均值对比（前 5）」的 items 为所选范围内评分均值最高的行政区。</p>
 */
@Schema(description = "柱状图")
public record RiskAssessmentChartVO(
        @Schema(description = "图表标题", example = "区县评分均值对比（前 5）")
        String title,
        @Schema(description = "数值单位", example = "分")
        String unit,
        @Schema(description = "分类项，顺序即展示顺序")
        RiskAssessmentChartItemVO[] items) {
}
