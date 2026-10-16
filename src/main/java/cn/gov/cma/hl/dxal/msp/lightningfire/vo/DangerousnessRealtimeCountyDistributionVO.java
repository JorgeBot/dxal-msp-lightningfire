package cn.gov.cma.hl.dxal.msp.lightningfire.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

/**
 * 危险性实时分析：所选区域在 since ~ until 内每个区县的所选要素值（地图配色 / 县级横条 / 点击县弹窗）。
 *
 * <p>数据源为表 dangerousness_realtime_statistics（日期 × 区县）。区县范围是 regionCode 内的全部县区：
 * 选择地市返回所辖县区、选择县市区返回该县区本身、选择 230000 返回全省 125 个县级单元；
 * 区间内没有有效记录的县照常返回、{@code value} 为 null（不补 0），因此 {@code countyCount} 是范围内的
 * 区县总数，{@code validCountyCount} 才是区间内有有效记录的区县个数。</p>
 *
 * <p>取值口径：每县取该要素在区间内的平均值（逐日等权）；区间只选一天（since = until）时
 * 即该县当天的值，与「单日快照」同一份数据。{@code counties} 按数值降序（无记录的排最后、
 * 同值按名称升序）。</p>
 */
@Schema(description = "危险性实时分析·各区县要素值")
public record DangerousnessRealtimeCountyDistributionVO(
        @Schema(description = "统计起始日期（含）", example = "2026-09-01")
        LocalDate since,
        @Schema(description = "统计结束日期（含）", example = "2026-09-30")
        LocalDate until,
        @Schema(description = "行政区划代码", example = "232700")
        Integer regionCode,
        @Schema(description = "行政区名称", example = "大兴安岭地区")
        String regionName,
        @Schema(description = "行政区级别：province=全省、city=地市、county=县市区", example = "city")
        String regionLevel,
        @Schema(description = "要素标识", example = "temperature")
        String element,
        @Schema(description = "要素名称", example = "气温")
        String elementName,
        @Schema(description = "数值单位", example = "℃")
        String unit,
        @Schema(description = "范围内区县总数", example = "7")
        Integer countyCount,
        @Schema(description = "区间内有有效记录的区县个数", example = "7")
        Integer validCountyCount,
        @Schema(description = "各区县的所选要素值，按数值降序（无记录的排最后）")
        DangerousnessRealtimeCountyVO[] counties) {
}
