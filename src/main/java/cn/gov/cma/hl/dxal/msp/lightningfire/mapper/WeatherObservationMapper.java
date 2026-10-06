package cn.gov.cma.hl.dxal.msp.lightningfire.mapper;

import cn.gov.cma.hl.dxal.msp.lightningfire.dto.lightningfeature.KeyElementStatisticDTO;
import cn.gov.cma.hl.dxal.msp.lightningfire.entity.WeatherObservation;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 区县级年度气象要素与湿润指数 Mapper
 *
 * <p>对应表 weather_observations。联合主键 (assessment_year, region_code)，
 * 单主键 API（{@code selectById} / {@code updateById} / {@code deleteById}）不可用，请使用 Wrapper 条件操作。</p>
 */
@Mapper
public interface WeatherObservationMapper extends BaseMapper<WeatherObservation> {

    /**
     * 雷击火关键要素统计：给定条件下各气象要素的平均、最大、最小，一行一要素。
     *
     * <p>五个要素列用 {@code CROSS JOIN LATERAL (VALUES ...)} 折成「要素标识 + 取值」的纵表，
     * 这样条件片段（{@code ${ew.customSqlSegment}}）只出现一次，新增要素只需在 VALUES 里加一行，
     * 不必把同一段 WHERE 抄五遍。要素标识取 Option.KeyElementOption 的枚举名。</p>
     *
     * <p>AVG / MAX / MIN 都跳过 NULL（某区县某要素缺测时不参与统计）；某要素在条件下没有任何有效值时
     * 三项均为 NULL，不在这里补 0。</p>
     *
     * @param wrapper 统计条件（年份区间 + 区划范围），用 {@code Wrappers.lambdaQuery()} 构造
     * @return 每个要素一行；条件下没有任何记录时返回空列表
     */
    @Select("""
            SELECT element_values.element,
                   AVG(element_values.value) AS avg_value,
                   MAX(element_values.value) AS max_value,
                   MIN(element_values.value) AS min_value
            FROM weather_observations
            CROSS JOIN LATERAL (VALUES
                ('temperature', weather_observations.avg_temperature_c),
                ('windSpeed', weather_observations.avg_wind_speed_ms),
                ('precipitation', weather_observations.precipitation_mm),
                ('relativeHumidity', weather_observations.avg_relative_humidity_pct),
                ('wetnessIndex', weather_observations.moisture_index)
            ) AS element_values(element, value)
            ${ew.customSqlSegment}
            GROUP BY element_values.element
            """)
    List<KeyElementStatisticDTO> selectKeyElementStatistics(@Param(Constants.WRAPPER) Wrapper<WeatherObservation> wrapper);

    /**
     * 多年平均湿润指数，作为统计概览里湿润指数距平的基准。
     *
     * <p>距平 = 区间平均 − 多年平均，本方法取后者：条件只限定区划范围、不加年份区间，
     * 即用该范围全部年份（2018—2025）的平均值作基准。范围内没有任何记录时返回 null。</p>
     *
     * @param wrapper 区划范围条件（不含年份区间）
     */
    @Select("""
            SELECT AVG(moisture_index)
            FROM weather_observations
            ${ew.customSqlSegment}
            """)
    Double selectAverageMoistureIndex(@Param(Constants.WRAPPER) Wrapper<WeatherObservation> wrapper);
}
