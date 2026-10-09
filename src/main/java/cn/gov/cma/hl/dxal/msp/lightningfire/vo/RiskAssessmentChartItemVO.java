package cn.gov.cma.hl.dxal.msp.lightningfire.vo;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 区县对比柱状图分类项。
 *
 * <p>综合风险评估与区划的区县对比用危险性评分的区域均值排名，数值单位见所属图表的 unit
 * （评分为 0—100 的百分制，无量纲），percent 不使用，固定为 null。</p>
 */
@Schema(description = "区县对比柱状图分类项")
public record RiskAssessmentChartItemVO(
        @Schema(description = "分类代码；危险性统计只有地区名称、没有区划代码，故为 null", example = "null")
        String code,
        @Schema(description = "行政区名称", example = "漠河市")
        String name,
        @Schema(description = "评分均值（0—100），单位见所属图表的 unit", example = "63.42")
        Double value,
        @Schema(description = "占比（%）；综合风险评估不使用，固定为 null")
        Double percent) {
}
