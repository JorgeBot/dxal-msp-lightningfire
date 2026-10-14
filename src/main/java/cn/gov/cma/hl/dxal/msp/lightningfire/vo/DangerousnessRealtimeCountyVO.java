package cn.gov.cma.hl.dxal.msp.lightningfire.vo;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 危险性实时分析：单个区县当日的危险性分数（概览统计的数组元素、排行的折线点共用一份结构）。
 *
 * <p>{@code score} 是 0—100 百分制的危险性评分，{@code level} 是表内给出的等级文案
 * （III 中等 / IV 较高 / V 高 / 无数据）。等级不按分数重算：表内等级取自底层危险性 H 的原始分档，
 * 与评分的分段并不逐点重合（见实体注释）。</p>
 *
 * <p>区分两种「没有值」：表内当天有该区县记录、但结果为空时 {@code score} 为 null、
 * {@code level} 为「无数据」；表内当天根本没有该区县记录时（如日期不在数据覆盖范围内）
 * {@code score} 与 {@code level} 都为 null。两者都不补 0。</p>
 */
@Schema(description = "危险性实时分析·区县当日分数")
public record DangerousnessRealtimeCountyVO(
        @Schema(description = "6 位行政区划代码（国标码）；区划枚举未覆盖的区县名为 null", example = "232701")
        Integer code,
        @Schema(description = "区县名称", example = "漠河市")
        String name,
        @Schema(description = "所属地级市名称", example = "大兴安岭地区")
        String cityName,
        @Schema(description = "危险性评分（0—100 百分制，越高越危险）；无结果为 null", example = "66.2260")
        Double score,
        @Schema(description = "危险性等级文案；表内无结果时为「无数据」，当天没有该区县记录时为 null",
                example = "IV 较高")
        String level,
        @Schema(description = "四个要素的分数，顺序固定：气温、相对湿度、风速、降水")
        DangerousnessRealtimeElementVO[] elements) {
}
