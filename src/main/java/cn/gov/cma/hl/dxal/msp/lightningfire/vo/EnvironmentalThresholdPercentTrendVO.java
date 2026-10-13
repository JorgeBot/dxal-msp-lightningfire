package cn.gov.cma.hl.dxal.msp.lightningfire.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

/**
 * 功能二：所选区域、所选环境因子在 since ~ until 内的「达到条件百分比」折线图。
 *
 * <p>数据源为县级逐日条件分布表 condition_distribution_daily 的 {@code {因子}_hit_pct} 列
 * （每行 = 一个县一天，值为符合格网数 ÷ 可判断格网数 × 100，已经是 0—100 的百分比）。</p>
 *
 * <p>多县汇总按 {@code valid_grid_count} 加权：区域当天的百分比 =
 * Σ(县比例 × 该县可判断格网数) ÷ Σ可判断格网数，等价于《开发字段说明》要求的
 * 「符合数之和 ÷ 可判断数之和」，不是各县百分比的简单平均。月 / 年粒度为该周期内
 * <b>有效日</b>区域比例的均值（与《开发字段说明》「月年为有效日比例平均」一致），
 * 无有效日的周期 percent 为 null、缺测断线，不补 0。</p>
 *
 * <p>x 轴恒按 granularity 展开 since ~ until（含无数据的时间点），保证前端折线连续；
 * 数据源只在 5—9 月的研究应用范围内有判定结果（2018-05-01—2024-09-30），
 * 其余时间的比例列为 NULL，表示无数据而不是 0%。</p>
 */
@Schema(description = "环境影响阈值·条件百分比折线图")
public record EnvironmentalThresholdPercentTrendVO(
        @Schema(description = "行政区划代码", example = "232700")
        Integer regionCode,
        @Schema(description = "行政区名称", example = "大兴安岭地区")
        String regionName,
        @Schema(description = "行政区级别：province=全省、city=地市、county=县市区", example = "city")
        String regionLevel,
        @Schema(description = "环境因子 id", example = "rain")
        String factor,
        @Schema(description = "环境因子名称", example = "日降水")
        String factorName,
        @Schema(description = "统计起始日期（含）", example = "2024-05-01")
        LocalDate since,
        @Schema(description = "统计结束日期（含）", example = "2024-09-30")
        LocalDate until,
        @Schema(description = "时间轴粒度：DAY=天（yyyy-MM-dd）、MONTH=月（yyyy-MM）、YEAR=年（yyyy）",
                example = "DAY")
        String granularity,
        @Schema(description = "百分比单位，恒为「%」（数值已经是 0—100，前端直接加 % 显示）", example = "%")
        String unit,
        @Schema(description = "折线点：按 granularity 展开 since ~ until，无有效日的时间点 percent 为 null")
        EnvironmentalThresholdPercentPointVO[] points) {
}
