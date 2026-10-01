package cn.gov.cma.hl.dxal.msp.lightningfire.vo;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 承载体暴露度指标统计：某一评价年份、某一行政区、某一评估指标的左侧统计与图表数据。
 *
 * <p>数据源为承载体暴露度汇总表（forest_fraction_summary、settlement_fraction_summary、
 * road_exposure_summary、exposure_composite_summary），即交接包年度统计 JSON 的入库结果。
 * 记录不存在时接口返回失败响应，不以 0 或空图表冒充无数据。</p>
 */
@Schema(description = "承载体暴露度指标统计")
public record ExposureStatisticsVO(
        @Schema(description = "评价年份", example = "2018")
        Short assessmentYear,
        @Schema(description = "数据来源年份；直接取请求的评价年份，与 assessmentYear 相同（不按 index.json 的产品来源年份换算）",
                example = "2018")
        Short sourceYear,
        @Schema(description = "评估指标取值", example = "forestFraction")
        String indicator,
        @Schema(description = "评估指标名称", example = "森林覆盖暴露度")
        String indicatorName,
        @Schema(description = "行政区划代码", example = "232700")
        Integer regionCode,
        @Schema(description = "行政区名称", example = "大兴安岭地区")
        String regionName,
        @Schema(description = "行政区级别：province=全省、city=地市、county=县市区", example = "city")
        String regionLevel,
        @Schema(description = "指标区间面积，五档，km²")
        ExposureBinVO[] bins,
        @Schema(description = "指标区总面积（五档面积之和），km²；任一档缺测时为 null", example = "82572.64")
        Double totalAreaKm2,
        @Schema(description = "类型占比柱状图")
        ExposureChartVO typeChart,
        @Schema(description = "指标均值对比柱状图")
        ExposureChartVO meanChart) {
}
