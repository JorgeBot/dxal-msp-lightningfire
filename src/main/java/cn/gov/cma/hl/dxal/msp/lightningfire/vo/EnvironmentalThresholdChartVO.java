package cn.gov.cma.hl.dxal.msp.lightningfire.vo;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 柱状图：标题 + 单位 + 分类项。
 *
 * <p>结构与综合风险评估与区划的 {@link RiskAssessmentChartVO}、承载体暴露度的
 * {@link ExposureChartVO} 一致，便于前端共用同一套图表组件；
 * 本模块用于「当日分数最高值前五」的区县柱状图，分类项为
 * {@link EnvironmentalThresholdCountyVO}（含区划代码、名称与当日分数）。</p>
 */
@Schema(description = "柱状图")
public record EnvironmentalThresholdChartVO(
        @Schema(description = "图表标题", example = "当日区县分数对比（前 5）")
        String title,
        @Schema(description = "数值单位", example = "分")
        String unit,
        @Schema(description = "分类项，顺序即展示顺序")
        EnvironmentalThresholdCountyVO[] items) {
}
