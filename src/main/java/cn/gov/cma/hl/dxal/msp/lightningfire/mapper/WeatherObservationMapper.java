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
 * 区县级逐日气象要素与年内累计湿润指数 Mapper
 *
 * <p>对应表 weather_observations。主键 (assessment_date, region_code)，
 * 单主键 API（{@code selectById} / {@code updateById} / {@code deleteById}）不可用，请使用 Wrapper 条件操作；
 * 日期条件用 {@code assessment_date} 的区间表达（逐日数据，不再按自然年取整）。</p>
 */
@Mapper
public interface WeatherObservationMapper extends BaseMapper<WeatherObservation> {

    /**
     * 雷击火关键要素统计：给定日期区间与区划条件下各气象要素的平均、最大、最小，一行一要素。
     *
     * <p>表已改为逐日粒度，故本方法在所选日期区间内的全部「区县 × 日」记录上聚合。注意这是
     * 全部区县记录的汇总，不是某一个区的值：调用方按区划范围过滤，得到的「平均」是范围内
     * 各区县日的平均、「最大 / 最小」是范围内任一天的值。</p>
     *
     * <p>逐元素口径：</p>
     * <ul>
     *   <li>气温、风速、相对湿度：AVG / MAX / MIN 即区间内日值的平均、最高、最低；</li>
     *   <li>降水：第一格取 AVG（日平均降水），MAX / MIN 为区间内最大、最小日降水；</li>
     *   <li>湿润指数：表内是「年初至当日」的累计值，不能 AVG（会把年初低值一起平均而系统性偏低，
     *       实测漠河市 2024 年逐日平均 3.27、年末 1.95），也不能 MAX（3—5 月累计积温分母极小，
     *       会取到年内尖峰：漠河市 2024 年 5 月尖峰 4.77、年末 1.95），故第一格取区间内
     *       <b>最后一个非空值</b>，即该区间最新的年内累计湿润指数；MAX / MIN 仍为区间内的极值。</li>
     * </ul>
     *
     * <p>五个要素列用 {@code CROSS JOIN LATERAL (VALUES ...)} 折成「要素标识 + 取值 + 日期」的纵表，
     * 这样条件片段（{@code ${ew.customSqlSegment}}）只出现一次，新增要素只需在 VALUES 里加一行，
     * 不必把同一段 WHERE 抄五遍。要素标识取 Option.KeyElementOption 的枚举名。</p>
     *
     * <p>聚合都跳过 NULL（某区县某日缺测时不参与统计）；某要素在条件下没有任何有效值时三项均为 NULL，
     * 不在这里补 0。</p>
     *
     * @param wrapper 统计条件（assessment_date 区间 + 区划范围），用 {@code Wrappers.lambdaQuery()} 构造
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
     * 区间内最新的年内累计湿润指数（第一格）。
     *
     * <p>单独成一条查询，是为了让条件片段（{@code ${ew.customSqlSegment}}）在 SQL 里只出现一次：
     * MyBatis-Plus 的 wrapper 参数名是固定的（MPGENVAL1…），同一段条件在一句 SQL 里出现两次会撞名。
     * 调用方传入与 {@link #selectKeyElementStatistics} 完全相同的 wrapper，两条查询的口径因此一致。</p>
     *
     * <p>取区间内 {@code assessment_date} 最大的那一天，再对当天有非空值的区县取平均：
     * 只取某一个区县的值会把「选区内最新湿润状况」变成某一个县的值（大兴安岭 7 县区同一天可相差
     * 一倍以上），故与其它要素一样在选区内取平均，只是先按「最新一天」筛过一轮。
     * 区间通常落在同一年内，该值即区间最新的年初至当日累计湿润指数
     * （如大兴安岭 2024-06-01 ~ 2024-08-31 得到 2024-08-31 各县的平均）。
     * 条件内没有任何非空湿润指数时返回 null。</p>
     *
     * @param wrapper 与要素统计相同的日期区间 + 区划范围条件
     */
    @Select("""
            SELECT AVG(latest.moisture_index)
            FROM (
                SELECT DISTINCT ON (weather_observations.region_code)
                       weather_observations.moisture_index
                FROM weather_observations
                ${ew.customSqlSegment}
                  AND weather_observations.moisture_index IS NOT NULL
                ORDER BY weather_observations.region_code,
                         weather_observations.assessment_date DESC
            ) AS latest
            """)
    Double selectLatestMoistureIndex(@Param(Constants.WRAPPER) Wrapper<WeatherObservation> wrapper);

    /**
     * 多年平均湿润指数，作为统计概览里湿润指数距平的基准。
     *
     * <p>表内的 {@code moisture_index} 是「年初至当日」的累计值：</p>
     * <ul>
     *   <li>不能对逐日值求平均——年初低值会拉低结果（漠河市 2024 年逐日平均 3.27，年末 1.95）；</li>
     *   <li>不能取 MAX——3—5 月累计积温分母极小，年内会出现尖峰（漠河市 2024 年 5 月 4.77），
     *       按 MAX 取到的「年度值」不是年末值。</li>
     * </ul>
     *
     * <p>故先取每个「区县 × 年」的年内最后一个非空值作为该区县该年的年度湿润指数，再对这些
     * 年度值求平均：</p>
     *
     * <pre>
     * AVG(该范围 2018—2025 各年、各县的年内最后一个非空湿润指数)
     * </pre>
     *
     * <p>实测大兴安岭 7 县区该基准约 2.27（用 MAX 会得到 25.30 的错误基准）。
     * 距平 = 区间值 − 多年平均，正值表示比多年平均更湿润；这是本表能给出的最长基准（2018—2025），
     * 不使用 1991—2020 常年值（数据不在本库内）。范围内没有任何记录时返回 null。</p>
     *
     * @param wrapper 区划范围条件（不含日期区间）
     */
    @Select("""
            SELECT AVG(year_end.moisture_index)
            FROM (
                SELECT DISTINCT ON (EXTRACT(YEAR FROM weather_observations.assessment_date),
                                    weather_observations.region_code)
                       weather_observations.moisture_index
                FROM weather_observations
                ${ew.customSqlSegment}
                  AND weather_observations.moisture_index IS NOT NULL
                ORDER BY EXTRACT(YEAR FROM weather_observations.assessment_date),
                         weather_observations.region_code,
                         weather_observations.assessment_date DESC
            ) AS year_end
            """)
    Double selectAverageMoistureIndex(@Param(Constants.WRAPPER) Wrapper<WeatherObservation> wrapper);
}
