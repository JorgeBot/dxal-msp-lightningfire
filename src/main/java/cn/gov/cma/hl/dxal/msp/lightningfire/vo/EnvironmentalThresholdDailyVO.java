package cn.gov.cma.hl.dxal.msp.lightningfire.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

/**
 * 雷击火环境影响阈值：所选区域在某一天的区县分数快照。
 *
 * <p>数据源为危险性统计表 dangerousness_statistics 的 score 列（0—100 百分制），
 * 按「地市 + 区县名」定位所选范围（表中只有地区名，没有行政区划代码）。</p>
 *
 * <p>统计范围是 regionCode 范围内的全部县区（不含地市与省级汇总行，表内也没有省级行）：
 * 选择地市返回所辖县区，选择县市区返回该县区本身，选择 230000 返回全省全部县区。
 * 当日没有记录的县区照常返回、{@code score} 为 null，因此 {@code countyCount} 是范围内的县区总数，
 * {@code validCountyCount} 才是当日有记录（分数非 null）的县区个数。</p>
 *
 * <p>{@code counties} 按分数降序排列（分数相同时名称升序），当日无记录的县区排在最后；
 * {@code topChart} 就是它的前 5 条（只含有记录的县区）。该日一条记录都没有时：
 * {@code validCountyCount} 为 0，最小 / 最大分数与对应县区名为 null，{@code topChart.items} 为空数组，
 * 接口返回 200 而不是 404——「当天没数据」本身就是这个页面需要展示的状态。</p>
 */
@Schema(description = "环境影响阈值·当日区县分数快照")
public record EnvironmentalThresholdDailyVO(
        @Schema(description = "统计日期", example = "2024-05-01")
        LocalDate date,
        @Schema(description = "行政区划代码", example = "232700")
        Integer regionCode,
        @Schema(description = "行政区名称", example = "大兴安岭地区")
        String regionName,
        @Schema(description = "行政区级别：province=全省、city=地市、county=县市区", example = "city")
        String regionLevel,
        @Schema(description = "范围内县区总数（区划枚举内的县区 + 表中出现但枚举未覆盖的县区）")
        Integer countyCount,
        @Schema(description = "当日有效区县个数：当日有评分记录（分数非 null）的县区数", example = "7")
        Integer validCountyCount,
        @Schema(description = "当日区县分数最小值；当日无任何记录时为 null", example = "12.35")
        Double minScore,
        @Schema(description = "取到最小值的区县名；分数相同时取名称靠前者；当日无记录时为 null", example = "塔河县")
        String minRegionName,
        @Schema(description = "当日区县分数最大值；当日无任何记录时为 null", example = "88.10")
        Double maxScore,
        @Schema(description = "取到最大值的区县名；分数相同时取名称靠前者；当日无记录时为 null", example = "呼中区")
        String maxRegionName,
        @Schema(description = "当日分数最高值前五柱状图（只含有记录的县区，不足 5 个时按实际个数返回）")
        EnvironmentalThresholdChartVO topChart,
        @Schema(description = "所选地区每个县的分数，按分数降序（无记录的县区 score 为 null，排在最后）")
        EnvironmentalThresholdCountyVO[] counties) {
}
