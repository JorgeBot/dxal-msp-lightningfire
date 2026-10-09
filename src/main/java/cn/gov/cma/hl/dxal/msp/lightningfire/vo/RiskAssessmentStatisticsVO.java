package cn.gov.cma.hl.dxal.msp.lightningfire.vo;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 综合风险评估与区划：某一评价年份、某一行政区（全省 / 地市 / 县市区）的风险评分统计。
 *
 * <p>数据源为危险性统计表 dangerousness_statistics 的 score 列（0—100 的百分制危险性评分）。
 * 该表按「日期 × 区/县 × 地级市」逐日存放评分，且没有行政区划代码，只有地区名称；
 * 因此本模块按 {@link cn.gov.cma.hl.dxal.msp.lightningfire.constant.RegionCode} 的地区名匹配，
 * 评价年份按自然年取该年全部有记录的日期。</p>
 *
 * <p>口径与缺数约定：</p>
 * <ul>
 *   <li>{@code riskIndex} 为选区内有记录日期的评分均值。县市区为该区县记录均值；地市为所辖县区
 *       记录均值的等权均值；全省为各地市均值的等权均值。先按行政区算均值再取均值，不做面积加权
 *       （危险性统计没有面积列与逐格数据）。</li>
 *   <li>该年份、该行政区没有记录时接口返回 404，不以 0 或空图表冒充无数据。</li>
 *   <li>2020 年该表只有 34 天记录（源数据缺日），{@code validDays}、{@code expectedDays} 与
 *       {@code coverage} 用于提示完整性；缺日不补 0，评分区间占比的分母也只有记录日。</li>
 *   <li>{@code validDays} 与 {@code coverage} 按所选范围内的行政区取平均：县市区即该区县有记录的
 *       日数与完整率；地市、全省为所辖县区（或各地市）的平均有记录日数与平均完整率，
 *       不是把各级日数相加，避免随单位数量放大。它们只提示源数据完整程度。</li>
 * </ul>
 */
@Schema(description = "综合风险评估与区划统计")
public record RiskAssessmentStatisticsVO(
        @Schema(description = "评价年份", example = "2018")
        Short assessmentYear,
        @Schema(description = "行政区划代码", example = "232700")
        Integer regionCode,
        @Schema(description = "行政区名称", example = "大兴安岭地区")
        String regionName,
        @Schema(description = "行政区级别：province=全省、city=地市、county=县市区", example = "city")
        String regionLevel,
        @Schema(description = "风险评分均值（0—100 百分制），即该区间的风险指数", example = "56.31")
        Double riskIndex,
        @Schema(description = "评分区间（0—100 等宽 20 分五档）的记录日数与占比，顺序即展示顺序")
        RiskAssessmentBinVO[] bins,
        @Schema(description = "平均有记录日数：县市区为该区县记录日数，地市、全省为所辖县区（或各地市）"
                + "有记录日数的平均（四舍五入）")
        Long validDays,
        @Schema(description = "该评价年份的日历天数（365 或 366）")
        Integer expectedDays,
        @Schema(description = "平均数据完整率（%）：所选范围内各行政区完整率的平均值，"
                + "县市区即该区县完整率", example = "98.36")
        Double coverage,
        @Schema(description = "区县评分均值对比柱状图：选择全省返回各地市前 5，选择地市返回所辖县区前 5，"
                + "选择县区返回其本身")
        RiskAssessmentChartVO countyChart) {
}
