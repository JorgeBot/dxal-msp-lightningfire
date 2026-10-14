package cn.gov.cma.hl.dxal.msp.lightningfire.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

/**
 * 危险性实时分析：风险分数折线图排行（当日前八名区县）。
 *
 * <p>数据源为表 dangerousness_realtime_statistics 的单日快照：取所选区域范围内当天危险性分数最高的
 * 8 个区县，按分数降序排列，X 轴为区县（{@code items} 的顺序即名次）、Y 轴为危险性分数，
 * 前端可直接把它当作折线图数据（{@code categories} = items 的 name、{@code series} = items 的 score）。</p>
 *
 * <p>不足 8 个区县时按实际个数返回：所选区域是县市区时只有该县本身一个点；
 * 当天没有结果、或日期不在数据覆盖范围内时 {@code items} 为空数组，接口返回 200 而不是 404。
 * 当天结果为空（表内等级为「无数据」）的区县不参与排行——缺测不是低风险，不能占用名次。</p>
 */
@Schema(description = "危险性实时分析·风险分数折线图排行")
public record DangerousnessRealtimeRankingVO(
        @Schema(description = "统计日期", example = "2026-09-01")
        LocalDate date,
        @Schema(description = "行政区划代码", example = "232700")
        Integer regionCode,
        @Schema(description = "行政区名称", example = "大兴安岭地区")
        String regionName,
        @Schema(description = "行政区级别：province=全省、city=地市、county=县市区", example = "city")
        String regionLevel,
        @Schema(description = "图表标题", example = "风险分数排行（前 8）")
        String title,
        @Schema(description = "分数单位，恒为「分」（0—100 百分制）", example = "分")
        String unit,
        @Schema(description = "折线点，按分数降序、顺序即名次；范围内不足 8 个有效区县时按实际个数返回")
        DangerousnessRealtimeRankingItemVO[] items) {
}
