package cn.gov.cma.hl.dxal.msp.lightningfire.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDate;

/**
 * 黑龙江省区县级逐日气象要素与年内累计湿润指数
 *
 * <p>对应表 weather_observations，粒度：采集日期 × 区县（密集日历网格，无观测日为 NULL），
 * 覆盖 2018-01-01—2025-12-31，125 个区县，实测 365250 行。来源 tmp/2018-2025.csv 全国站点日值，
 * 按 region.txt 编码聚合；region_level 本表统一为 county。</p>
 *
 * <p>两类列要分清：</p>
 * <ul>
 *   <li>当日值：{@code precipitation_mm}（当日降水）、{@code avg_temperature_c}（当日平均气温）、
 *       {@code avg_relative_humidity_pct}、{@code avg_wind_speed_ms}，均为当日各上报站点等权平均；</li>
 *   <li>年内累计值：{@code precip_ytd_mm}（年初至当日累计降水）、{@code active_temp_sum_ytd_c}
 *       （年初至当日累计活跃积温 ΣT≥10℃）、{@code moisture_index}（年内累计湿润指数
 *       = 年内累计降水 ÷ (0.1 × 年内累计活跃积温)）。三者都从年初重新累计，跨年不连续，
 *       累计积温 ≤ 0 时空值。</li>
 * </ul>
 *
 * <p>主键 (assessment_date, region_code)：MyBatis-Plus 的单主键 API（如 {@code selectById} /
 * {@code updateById} / {@code deleteById}）不可用，请使用 Wrapper 条件操作；日期条件用
 * {@code assessment_date} 的区间表达，不再有年度列。本表是逐日数据，按区间取数时由 SQL 聚合，
 * 不要把日值当年度值再聚合一次（例如年度湿润指数不能由逐日 {@code moisture_index} 平均得到，
 * 见 {@link cn.gov.cma.hl.dxal.msp.lightningfire.mapper.WeatherObservationMapper}）。</p>
 */
@Data
@Accessors(chain = true)
@TableName("weather_observations")
public class WeatherObservation {

    /**
     * 采集日期（取自源数据 Datetime 列），主键之一，覆盖 2018—2025
     *
     * <p>逐日数据：按区间查询时用本列的范围条件表达 since ~ until，不再按自然年取整。</p>
     */
    @TableField("assessment_date")
    private LocalDate assessmentDate;

    /**
     * 区县 6 位行政区划代码，取自 region.txt，主键之二
     */
    @TableField("region_code")
    private Integer regionCode;

    /**
     * 区县名称，取自 region.txt
     */
    @TableField("region_name")
    private String regionName;

    /**
     * 行政层级，本表统一为 'county'
     */
    @TableField("region_level")
    private String regionLevel;

    /**
     * 当日降水量(mm)：当日各上报站点等权平均；自动站 11—3 月多缺测，为空表示当日缺测
     */
    @TableField("precipitation_mm")
    private Double precipitationMm;

    /**
     * 当日平均气温(℃)：当日各上报站点等权平均
     */
    @TableField("avg_temperature_c")
    private Double avgTemperatureC;

    /**
     * 当日平均相对湿度(%)
     */
    @TableField("avg_relative_humidity_pct")
    private Double avgRelativeHumidityPct;

    /**
     * 当日平均风速(m/s)
     */
    @TableField("avg_wind_speed_ms")
    private Double avgWindSpeedMs;

    /**
     * 年内累计湿润指数 = 年初至当日累计降水mm ÷ (0.1 × 年初至当日累计活跃积温℃)；
     * 累计积温 ≤ 0 时为空
     *
     * <p>注意这是「年初至当日」的累计值，不是当日值、也不是年度终值：3—5 月分母很小、数值不稳定，
     * 年内单调性也不保证（累计积温按 ΣT≥10℃ 累加）。年度湿润指数取该年最后一日的本列值，
     * 多年平均不能对逐日值直接求平均。</p>
     */
    @TableField("moisture_index")
    private Double moistureIndex;

    /**
     * 当日参与统计的站点数；为空表示当日无任何站点上报
     */
    @TableField("station_count")
    private Short stationCount;

    /**
     * 年初至当日累计降水量(mm)，等于 {@code precipitation_mm} 列的年内求和
     */
    @TableField("precip_ytd_mm")
    private Double precipYtdMm;

    /**
     * 年初至当日累计活跃积温 ΣT≥10℃(℃)，由入库后的日值累加，保留 2 位小数
     */
    @TableField("active_temp_sum_ytd_c")
    private Double activeTempSumYtdC;
}
