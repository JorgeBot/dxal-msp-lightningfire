package cn.gov.cma.hl.dxal.msp.lightningfire.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

/**
 * 功能二：所选区域内所有区县在所选日期的达到条件百分比（地图配色 / 县级横条 / 点击县弹窗的数据）。
 *
 * <p>数据源为县级逐日条件分布表 condition_distribution_daily 的 {@code {因子}_hit_pct} 列。
 * 区县范围是 regionCode 内的全部县区：选地市返回所辖县区、选县市区返回该县区本身、
 * 选 230000 返回全省全部 125 个县级单元；当天没有判定结果的县照常返回、percent 为 null，
 * 因此「无数据」与「0%（有数据但没有格网符合）」在结果里可区分。</p>
 *
 * <p>granularity 决定取值窗口：DAY（默认）取所选日期当天，与页面「月 / 年折线取均值、
 * 地图仍显示所选单日」的约定一致；MONTH / YEAR 取该日期所在月 / 年内该县
 * <b>有效日</b>比例的平均值，validDays 为参与平均的有效日数。前端把粒度统一传给各接口即可。</p>
 *
 * <p>counties 按百分比降序排列（无数据的县排在最后，同值按区划码升序）；
 * regionPercent 为区域内多县汇总：DAY 为按 valid_grid_count 加权的当天比例
 * （Σ(县比例 × 可判断格网数) ÷ Σ可判断格网数，等价于「符合数之和 ÷ 可判断数之和」），
 * MONTH / YEAR 为该周期内有效日区域比例的均值；区域内没有任何有效数据时为 null。</p>
 */
@Schema(description = "环境影响阈值·所选区域各区县达到条件百分比")
public record EnvironmentalThresholdCountyDistributionVO(
        @Schema(description = "行政区划代码", example = "232700")
        Integer regionCode,
        @Schema(description = "行政区名称", example = "大兴安岭地区")
        String regionName,
        @Schema(description = "行政区级别：province=全省、city=地市、county=县市区", example = "city")
        String regionLevel,
        @Schema(description = "所选日期（DAY 粒度即统计日；MONTH / YEAR 为取值窗口所在日）", example = "2024-08-20")
        LocalDate date,
        @Schema(description = "取值粒度：DAY=当天、MONTH=该日期所在月、YEAR=该日期所在年", example = "DAY")
        String granularity,
        @Schema(description = "环境因子 id", example = "rain")
        String factor,
        @Schema(description = "环境因子名称", example = "日降水")
        String factorName,
        @Schema(description = "百分比单位，恒为「%」（数值已经是 0—100，前端直接加 % 显示）", example = "%")
        String unit,
        @Schema(description = "区域合计百分比（0—100）：DAY 为按可判断格网数加权的各县比例，"
                + "MONTH / YEAR 为有效日区域比例的均值；无数据时为 null", example = "87.53")
        Double regionPercent,
        @Schema(description = "区域合计参与平均的有效日数", example = "1")
        Long regionValidDays,
        @Schema(description = "所选区域内的区县百分比，按百分比降序（无数据的排最后）")
        EnvironmentalThresholdCountyPercentVO[] counties) {
}
