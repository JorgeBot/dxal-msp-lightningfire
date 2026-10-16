package cn.gov.cma.hl.dxal.msp.lightningfire.vo;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 危险性实时分析：要素排行的单个数据项（一个区县）。
 *
 * <p>{@code rank} 为名次（1 开始，按所选要素值降序）；图表的 x 轴即 items 的顺序，
 * 也就是「当日所选要素最高的 8 个区县」由高到低排列，y 轴取 {@code value}
 * （柱状图与折线图用的是同一份数据）。单位在所属结果的 {@code unit} 字段给出。</p>
 */
@Schema(description = "危险性实时分析·排行的单个数据项")
public record DangerousnessRealtimeRankingItemVO(
        @Schema(description = "名次（1 开始，按所选要素值降序）", example = "1")
        Integer rank,
        @Schema(description = "6 位行政区划代码（国标码）；区划枚举未覆盖的区县名为 null", example = "231005")
        Integer code,
        @Schema(description = "区县名称", example = "西安区")
        String name,
        @Schema(description = "所属地级市名称", example = "牡丹江市")
        String cityName,
        @Schema(description = "所选要素当日值", example = "91.0054")
        Double value) {
}
