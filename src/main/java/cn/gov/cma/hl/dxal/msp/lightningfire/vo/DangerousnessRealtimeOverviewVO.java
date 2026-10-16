package cn.gov.cma.hl.dxal.msp.lightningfire.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

/**
 * 危险性实时分析：所选区域在 since ~ until 内所选要素的要素概览统计。
 *
 * <p>数据源为表 dangerousness_realtime_statistics（日期 × 区县），一行一个区县一天的结果。
 * 本结果只给区域级的四项统计（最大值、最小值、平均值、极差），逐区县的数值在
 * {@code /dangerousness-realtime/counties} 单独返回：概览页的数值展示由前端按所选区域的行政级别决定——
 * 选区县一级直接取该县的值，选地市 / 全省一级取所辖县区的平均值，因此两个接口分开取、避免把
 * 125 个区县的明细塞进概览响应。</p>
 *
 * <p>统计范围：{@code regionCode} 范围内（选择地市即所辖县区、选择县市区即该县区本身、
 * 选择 230000 即全省 125 个县级单元）在 since ~ until 上的全部「区县 × 日」有效记录，
 * 逐日等权、不按面积或站点数加权；{@code validRecordCount} 即参与统计的记录条数，
 * 用于提示源数据完整程度（区间内一条有效记录都没有时为 0，四项统计的值都为 null，接口返回 200）。</p>
 */
@Schema(description = "危险性实时分析·要素概览统计")
public record DangerousnessRealtimeOverviewVO(
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
        @Schema(description = "参与统计的有效记录数（区间内所选区域范围内的「区县 × 日」记录）", example = "210")
        Integer validRecordCount,
        @Schema(description = "要素概览统计，顺序固定：最大值、最小值、平均值、极差（最大值 − 最小值）")
        DangerousnessRealtimeElementStatVO[] statistics) {
}
