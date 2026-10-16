package cn.gov.cma.hl.dxal.msp.lightningfire.vo;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 危险性实时分析：单个区县在所选区间内的所选要素值（概览页地图 / 县级横条与排行折线点共用一份结构）。
 *
 * <p>{@code value} 是该区县在 since ~ until 内该要素的<b>平均值</b>（按有记录的日取值、逐日等权，
 * 不按面积或站点数加权），并按要素在表内的小数位数抹掉 double 累加的尾差；
 * 区间只选一天（since = until）时它就是该区县当天的值。
 * 区间内该县没有有效记录时 {@code value} 为 null，不补 0（缺测不是低风险）。</p>
 *
 * <p>单位在所属结果的 {@code unit} 字段给出（同一要素的区县项单位相同，不在每一项里重复）。</p>
 */
@Schema(description = "危险性实时分析·区县要素值")
public record DangerousnessRealtimeCountyVO(
        @Schema(description = "6 位行政区划代码（国标码）；区划枚举未覆盖的区县名为 null", example = "232701")
        Integer code,
        @Schema(description = "区县名称", example = "漠河市")
        String name,
        @Schema(description = "所属地级市名称", example = "大兴安岭地区")
        String cityName,
        @Schema(description = "所选要素在区间内的平均值；区间内无有效记录时为 null", example = "7.072761")
        Double value) {
}
