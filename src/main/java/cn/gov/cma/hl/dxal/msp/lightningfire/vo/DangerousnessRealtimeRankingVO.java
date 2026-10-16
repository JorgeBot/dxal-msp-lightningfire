package cn.gov.cma.hl.dxal.msp.lightningfire.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

/**
 * 危险性实时分析：所选区域在 date 当天的要素排行（前八名区县）。
 *
 * <p>排行对象是 {@code element} 指定的要素（危险性评分也算一个要素），不再是固定的风险分数：
 * 取所选区域范围内当天该要素最高 / 最低的 8 个区县，按数值降序排列，X 轴为区县（{@code items} 的顺序
 * 即名次）、Y 轴为该要素值，前端可直接把它当作柱状图或折线图数据
 * （{@code categories} = items 的 name、{@code series} = items 的 value）。</p>
 *
 * <p>不足 8 个区县时按实际个数返回：所选区域是县市区时只有该县本身一个点；
 * 当天该要素没有有效值、或日期不在数据覆盖范围内时 {@code items} 为空数组，接口返回 200 而不是 404。
 * 当天缺测（无值）的区县不参与排行——缺测不是低值，不能占用名次。</p>
 */
@Schema(description = "危险性实时分析·要素排行")
public record DangerousnessRealtimeRankingVO(
        @Schema(description = "统计日期", example = "2026-09-15")
        LocalDate date,
        @Schema(description = "行政区划代码", example = "230000")
        Integer regionCode,
        @Schema(description = "行政区名称", example = "全省")
        String regionName,
        @Schema(description = "行政区级别：province=全省、city=地市、county=县市区", example = "province")
        String regionLevel,
        @Schema(description = "要素标识", example = "score")
        String element,
        @Schema(description = "要素名称", example = "危险性评分")
        String elementName,
        @Schema(description = "数值单位", example = "分")
        String unit,
        @Schema(description = "图表标题", example = "危险性评分排行（前 8）")
        String title,
        @Schema(description = "排行数据项，按所选要素值降序、顺序即名次；范围内不足 8 个有效区县时按实际个数返回")
        DangerousnessRealtimeRankingItemVO[] items) {
}
