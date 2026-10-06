package cn.gov.cma.hl.dxal.msp.lightningfire.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 黑龙江省区县级年度气象要素与湿润指数
 *
 * <p>对应表 weather_observations，粒度：评价年份 × 区县。
 * 来源 tmp/2018-2025.csv 全国站点日值，按 region.txt 编码聚合，覆盖 2018—2025 年。</p>
 *
 * <p>联合主键 (assessment_year, region_code)，MyBatis-Plus 的单主键 API（如 {@code selectById} /
 * {@code updateById} / {@code deleteById}）不可用，请使用 Wrapper 条件操作；
 * 表上另有索引 (region_code, assessment_year)。</p>
 */
@Data
@Accessors(chain = true)
@TableName("weather_observations")
public class WeatherObservation {

    /**
     * 年份（取自 Datetime 列），联合主键之一，取值 2018—2025
     *
     * <p>仅标注联合主键中的首列，避免 MyBatis-Plus 元数据初始化报
     * “There must be only one, but 2 was found”的一般错误；
     * 第二条主键列 region_code 以普通字段参与，条件查询请使用 Wrapper。</p>
     */
    @TableId(value = "assessment_year", type = IdType.INPUT)
    private Short assessmentYear;

    /**
     * 区县 6 位行政区划代码，取自 region.txt，联合主键之二
     */
    @TableField("region_code")
    private Integer regionCode;

    /**
     * 区县名称，取自 region.txt
     */
    @TableField("region_name")
    private String regionName;

    /**
     * 行政层级，本表统一为 'county'（数据库默认值 county）
     */
    @TableField("region_level")
    private String regionLevel;

    /**
     * 年降水量合计(mm)，县内各站点年降水量的等权平均
     */
    @TableField("precipitation_mm")
    private Double precipitationMm;

    /**
     * 年平均气温(℃)，先按站点算年均再对县内站点等权平均
     */
    @TableField("avg_temperature_c")
    private Double avgTemperatureC;

    /**
     * 年平均相对湿度(%)
     */
    @TableField("avg_relative_humidity_pct")
    private Double avgRelativeHumidityPct;

    /**
     * 年平均风速(m/s)
     */
    @TableField("avg_wind_speed_ms")
    private Double avgWindSpeedMs;

    /**
     * 湿润指数 = 谢利亚尼诺夫水热系数 K = 年降水量mm / (0.1 × ΣT≥10℃)；积温 ≤ 0 时为空
     */
    @TableField("moisture_index")
    private Double moistureIndex;

    /**
     * 参与该县当年统计的站点数
     */
    @TableField("station_count")
    private Short stationCount;

    /**
     * 参与统计的有效记录数（站点 × 日，已去重）
     */
    @TableField("valid_days")
    private Integer validDays;

    /**
     * 活跃积温 ΣT≥10℃(℃)：日平均气温 ≥ 10℃ 的积温合计，县内站点等权平均
     */
    @TableField("active_temp_sum_c")
    private Double activeTempSumC;

    /**
     * 降水有效记录数合计（站点 × 日）
     */
    @TableField("precip_days")
    private Integer precipDays;
}
