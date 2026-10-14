package cn.gov.cma.hl.dxal.msp.lightningfire.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

/**
 * 危险性实时分析：所选区域在某一天的概览统计（危险等级、危险性分数与四个要素的分数）。
 *
 * <p>数据源为表 dangerousness_realtime_statistics（日期 × 区县），本结果返回所选区域范围内的
 * <b>全部区县</b>各自的危险等级、危险性分数与四个要素的分数，不做区域汇总：
 * 概览页的数值展示由前端按所选区域的行政级别决定——选区县一级直接取该县那一行，
 * 选地市一级取该市下所辖县区的分数平均值（要平均的县区就是本结果的 counties）。</p>
 *
 * <p>区县范围：选择地市返回所辖县区，选择县市区返回该县区本身，选择 230000 返回全省 125 个县级单元。
 * 当天没有结果的区县照常返回、{@code score} 与四个要素为 null（表内有记录但无结果时
 * {@code level} 为「无数据」），因此 {@code countyCount} 是范围内的区县总数，
 * {@code validCountyCount} 才是当天结果非 null 的区县个数。</p>
 *
 * <p>{@code counties} 按分数降序排列（无结果的排在最后，同值按名称升序）。当天一条结果都没有时
 * 返回一份空快照（{@code validCountyCount} 为 0、各区县分数为 null），接口返回 200 而不是 404——
 * 「这一天还没有结果」本身就是实时页面要展示的状态。</p>
 */
@Schema(description = "危险性实时分析·概览统计")
public record DangerousnessRealtimeOverviewVO(
        @Schema(description = "统计日期", example = "2026-09-01")
        LocalDate date,
        @Schema(description = "行政区划代码", example = "232700")
        Integer regionCode,
        @Schema(description = "行政区名称", example = "大兴安岭地区")
        String regionName,
        @Schema(description = "行政区级别：province=全省、city=地市、county=县市区", example = "city")
        String regionLevel,
        @Schema(description = "分数单位，恒为「分」（0—100 百分制）", example = "分")
        String unit,
        @Schema(description = "范围内区县总数", example = "7")
        Integer countyCount,
        @Schema(description = "当天结果有效的区县个数（分数非 null）", example = "7")
        Integer validCountyCount,
        @Schema(description = "所选区域内区县的概览数据，按分数降序（无结果的排最后）")
        DangerousnessRealtimeCountyVO[] counties) {
}
