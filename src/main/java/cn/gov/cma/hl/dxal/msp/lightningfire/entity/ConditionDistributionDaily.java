package cn.gov.cma.hl.dxal.msp.lightningfire.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDate;

/**
 * 县级逐日条件分布（功能二：固定阈值）
 *
 * <p>对应表 condition_distribution_daily，粒度：日期 × 县，一行一个县一天的十个环境因子
 * 「达到固定条件的比例」（实测 2018-01-01—2024-12-31，125 个县级单元 × 2557 天 = 319625 行，
 * 覆盖全省 125 个县级单元，没有地市与省级汇总行）。</p>
 *
 * <p>主键是 (assessment_date, region_code) 复合主键，没有单列主键，因此仅支持基于 Wrapper 的查询；
 * {@code selectById}、{@code updateById}、{@code deleteById} 等依赖单列主键的方法不可用。</p>
 *
 * <p>列口径：{@code valid_grid_count} 为该县当天可判断的含林 1km 格网数；
 * 十个 {@code {因子}_hit_pct} 为「符合格网数 ÷ 可判断格网数 × 100」，
 * 已经是 0—100 的百分比（如 87.53 表示 87.53%），前端直接加 % 显示、不再乘 100；
 * 该日不在研究应用范围（5—9 月）、该县没有含林格网或没有结果时该列为 NULL，
 * 表示「无数据」而不是「0%」——0 是有数据但没有格网符合。</p>
 *
 * <p>阈值本身不在本表，来自交接包提取的常量 {@link cn.gov.cma.hl.dxal.msp.lightningfire.constant.FixedThreshold}；
 * 本表只存「拿固定尺子量完的结果」，换阈值必须用原始格网重算，不能套用旧计数。</p>
 */
@Data
@Accessors(chain = true)
@TableName("condition_distribution_daily")
public class ConditionDistributionDaily {

    /**
     * 评估日期（研究应用范围内为 5—9 月）
     */
    @TableField("assessment_date")
    private LocalDate assessmentDate;

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
     * 雷电记录数符合比例（%）
     */
    @TableField("lightning_count_hit_pct")
    private Double lightningCountHitPct;

    /**
     * 雷电平均绝对强度符合比例（%）
     */
    @TableField("mean_abs_intensity_hit_pct")
    private Double meanAbsIntensityHitPct;

    /**
     * 气温符合比例（%）
     */
    @TableField("temperature_hit_pct")
    private Double temperatureHitPct;

    /**
     * 风速符合比例（%）
     */
    @TableField("wind_speed_hit_pct")
    private Double windSpeedHitPct;

    /**
     * 相对湿度符合比例（%）
     */
    @TableField("relative_humidity_hit_pct")
    private Double relativeHumidityHitPct;

    /**
     * 日降水符合比例（%）
     */
    @TableField("rain_hit_pct")
    private Double rainHitPct;

    /**
     * 可燃物含水率 FMC 符合比例（%）
     */
    @TableField("fmc_hit_pct")
    private Double fmcHitPct;

    /**
     * 高程符合比例（%）
     */
    @TableField("dem_hit_pct")
    private Double demHitPct;

    /**
     * 坡度符合比例（%）
     */
    @TableField("slope_hit_pct")
    private Double slopeHitPct;

    /**
     * 组合环境条件符合比例（%）：浅层树多组条件「组内且、组间或」的判定结果
     */
    @TableField("combined_hit_pct")
    private Double combinedHitPct;
}
