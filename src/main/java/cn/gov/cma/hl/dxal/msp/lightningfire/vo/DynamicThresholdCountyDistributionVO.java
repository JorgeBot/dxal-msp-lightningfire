package cn.gov.cma.hl.dxal.msp.lightningfire.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

/**
 * 功能三：所选区域内各区县在所选日期的动态阈值结果（符合新条件地图 / 变化地图 / 点击县弹窗）。
 *
 * <p>数据源为县级逐日动态条件分布表 dynamic_condition_distribution_daily：该日使用的是
 * {@code assessmentYear} 那一年的年度候选阈值（日期 2024-08-20 用 year=2024 的候选线），
 * 表里存的是应用该线后的结果，接口不重新逐格判定。</p>
 *
 * <p>区县范围是 regionCode 内的全部县区：选地市返回所辖县区、选县市区返回该县区本身、
 * 选 230000 返回全省全部 125 个县级单元；当天没有结果的县照常返回、两个百分比为 null，
 * 因此「无数据」与「0%（有数据但没有格网符合）」可区分。</p>
 *
 * <p>counties 按符合新条件百分比降序（无数据的排最后，同值按区划码升序）；
 * regionHitPercent / regionChangedPercent 为区域合计，按可判断格网数加权：
 * Σ(县百分比 × 该县可判断格网数) ÷ Σ可判断格网数，等价于开发字段说明要求的
 * 「符合数（或变化数）之和 ÷ 可判断数之和」，不是各县百分比的简单平均；
 * 区域内一个有效数据都没有时为 null。数据覆盖 2019-05-01—2024-09-30（5—9 月），
 * 2025 年只有候选线、没有逐日结果。</p>
 */
@Schema(description = "环境影响阈值·所选区域各区县动态阈值结果")
public record DynamicThresholdCountyDistributionVO(
        @Schema(description = "行政区划代码", example = "232700")
        Integer regionCode,
        @Schema(description = "行政区名称", example = "大兴安岭地区")
        String regionName,
        @Schema(description = "行政区级别：province=全省、city=地市、county=县市区", example = "city")
        String regionLevel,
        @Schema(description = "所选日期", example = "2024-08-20")
        LocalDate date,
        @Schema(description = "该日应用的年度候选年份（取自逐日动态表的 assessment_year；该日无行时按日期年份）",
                example = "2024")
        Integer assessmentYear,
        @Schema(description = "环境因子 id", example = "rain")
        String factor,
        @Schema(description = "环境因子名称", example = "日降水")
        String factorName,
        @Schema(description = "百分比单位，恒为「%」（数值已经是 0—100，前端直接加 % 显示）", example = "%")
        String unit,
        @Schema(description = "区域符合新条件百分比（按可判断格网数加权）；无数据时为 null", example = "100.0")
        Double regionHitPercent,
        @Schema(description = "区域判定改变百分比（按可判断格网数加权）；无数据时为 null", example = "0.0")
        Double regionChangedPercent,
        @Schema(description = "所选区域内的区县结果，按符合新条件百分比降序（无数据的排最后）")
        DynamicThresholdCountyVO[] counties) {
}
