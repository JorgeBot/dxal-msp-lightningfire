package cn.gov.cma.hl.dxal.msp.lightningfire.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDate;

/**
 * 县级逐日动态条件分布（功能三：动态阈值）
 *
 * <p>对应表 dynamic_condition_distribution_daily，粒度：日期 × 县，一行一个县一天应用「当年候选阈值」后
 * 的九个因子结果（实测 2019-05-01—2024-09-30、125 个县级单元 × 6 年 × 153 天 = 114750 行，
 * 覆盖全省 125 个县级单元，没有地市与省级汇总行）。</p>
 *
 * <p>主键是 (assessment_date, region_code) 复合主键，没有单列主键，因此仅支持基于 Wrapper 的查询；
 * {@code selectById}、{@code updateById}、{@code deleteById} 等依赖单列主键的方法不可用。</p>
 *
 * <p>列口径：{@code assessment_year} 为当天应用的年度候选年份（与日期同年，日期 2024-08-20 用年度表的
 * year=2024）；{@code valid_grid_count} 为该县当天可判断的含林 1km 格网数；
 * 九个因子各有两列百分比：{@code {因子}_hit_pct} 为「符合新条件」的格网占比，
 * {@code {因子}_changed_pct} 为「与旧（固定基线）条件判定不同」的格网占比，
 * 两列都是「计数 ÷ 可判断格网数 × 100」后的 0—100 数值（如 87.53 表示 87.53%），前端直接加 % 显示。
 * 两者一般不互补：新条件 0% 而变化 100% 表示原先全部符合、现在全部不符合。</p>
 *
 * <p>该日无判定结果（5—9 月研究应用范围之外、没有含林格网、或该年只有候选线没有逐日结果，
 * 如 2025 年缺 FMC）时两列均为 NULL，表示「无数据」而不是 0%。本表没有组合环境条件列。</p>
 *
 * <p>年度候选阈值不在本表，来自交接包提取的常量
 * {@link cn.gov.cma.hl.dxal.msp.lightningfire.constant.DynamicThreshold}；
 * 固定基线（当期固定阈值）见 {@link cn.gov.cma.hl.dxal.msp.lightningfire.constant.FixedThreshold}。</p>
 */
@Data
@Accessors(chain = true)
@TableName("dynamic_condition_distribution_daily")
public class DynamicConditionDistributionDaily {

    /**
     * 评估日期（研究应用范围内为 5—9 月）
     */
    @TableField("assessment_date")
    private LocalDate assessmentDate;

    /**
     * 当天应用的年度候选年份
     */
    @TableField("assessment_year")
    private Short assessmentYear;

    /**
     * 行政区划代码（6 位，GB/T 2260），取值参照 {@link cn.gov.cma.hl.dxal.msp.lightningfire.constant.RegionCode}
     */
    @TableField("region_code")
    private Integer regionCode;

    /**
     * 区县名称（与行政区字典一致，本模块按区划码取数，名称只作冗余展示）
     */
    @TableField("region_name")
    private String regionName;

    /**
     * 可判断格网数：该县当天参与判定的含林 1km 格网数，为 0 时各比例为 NULL
     */
    @TableField("valid_grid_count")
    private Integer validGridCount;

    /**
     * 雷电记录数符合新条件比例（%）
     */
    @TableField("lightning_count_hit_pct")
    private Double lightningCountHitPct;

    /**
     * 雷电记录数判定变化比例（%）
     */
    @TableField("lightning_count_changed_pct")
    private Double lightningCountChangedPct;

    /**
     * 雷电平均绝对强度符合新条件比例（%）
     */
    @TableField("mean_abs_intensity_hit_pct")
    private Double meanAbsIntensityHitPct;

    /**
     * 雷电平均绝对强度判定变化比例（%）
     */
    @TableField("mean_abs_intensity_changed_pct")
    private Double meanAbsIntensityChangedPct;

    /**
     * 气温符合新条件比例（%）
     */
    @TableField("temperature_hit_pct")
    private Double temperatureHitPct;

    /**
     * 气温判定变化比例（%）
     */
    @TableField("temperature_changed_pct")
    private Double temperatureChangedPct;

    /**
     * 风速符合新条件比例（%）
     */
    @TableField("wind_speed_hit_pct")
    private Double windSpeedHitPct;

    /**
     * 风速判定变化比例（%）
     */
    @TableField("wind_speed_changed_pct")
    private Double windSpeedChangedPct;

    /**
     * 相对湿度符合新条件比例（%）
     */
    @TableField("relative_humidity_hit_pct")
    private Double relativeHumidityHitPct;

    /**
     * 相对湿度判定变化比例（%）
     */
    @TableField("relative_humidity_changed_pct")
    private Double relativeHumidityChangedPct;

    /**
     * 日降水符合新条件比例（%）
     */
    @TableField("rain_hit_pct")
    private Double rainHitPct;

    /**
     * 日降水判定变化比例（%）
     */
    @TableField("rain_changed_pct")
    private Double rainChangedPct;

    /**
     * 可燃物含水率 FMC 符合新条件比例（%）
     */
    @TableField("fmc_hit_pct")
    private Double fmcHitPct;

    /**
     * 可燃物含水率 FMC 判定变化比例（%）
     */
    @TableField("fmc_changed_pct")
    private Double fmcChangedPct;

    /**
     * 高程符合新条件比例（%）
     */
    @TableField("dem_hit_pct")
    private Double demHitPct;

    /**
     * 高程判定变化比例（%）
     */
    @TableField("dem_changed_pct")
    private Double demChangedPct;

    /**
     * 坡度符合新条件比例（%）
     */
    @TableField("slope_hit_pct")
    private Double slopeHitPct;

    /**
     * 坡度判定变化比例（%）
     */
    @TableField("slope_changed_pct")
    private Double slopeChangedPct;
}
