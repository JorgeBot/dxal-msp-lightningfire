package cn.gov.cma.hl.dxal.msp.lightningfire.vo;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 危险性实时分析：风险分数折线图排行的单个折线点（一个区县）。
 *
 * <p>{@code rank} 为名次（1 开始，按分数降序）；折线图的 x 轴即 items 的顺序，
 * 也就是「当日分数最高的 8 个区县」由高到低排列，y 轴取 {@code score}。
 * {@code level} 为表内的等级文案，供折线点着色 / 提示框展示。</p>
 */
@Schema(description = "危险性实时分析·折线图排行的折线点")
public record DangerousnessRealtimeRankingItemVO(
        @Schema(description = "名次（1 开始，按分数降序）", example = "1")
        Integer rank,
        @Schema(description = "6 位行政区划代码（国标码）；区划枚举未覆盖的区县名为 null", example = "231005")
        Integer code,
        @Schema(description = "区县名称", example = "西安区")
        String name,
        @Schema(description = "所属地级市名称", example = "牡丹江市")
        String cityName,
        @Schema(description = "危险性评分（0—100 百分制，越高越危险）", example = "91.0054")
        Double score,
        @Schema(description = "危险性等级文案", example = "V 高")
        String level) {
}
