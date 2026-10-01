package cn.gov.cma.hl.dxal.msp.lightningfire.mapper;

import cn.gov.cma.hl.dxal.msp.lightningfire.entity.ThunderstormStatistics;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 大兴安岭雷暴统计 Mapper
 *
 * <p>对应表 thunderstorm_statistics。</p>
 */
@Mapper
public interface ThunderstormStatisticsMapper extends BaseMapper<ThunderstormStatistics> {

    /**
     * 年平均雷暴天数：给定站号范围内全部年份的年雷暴日数平均。
     *
     * <p>表里只有 1961—2013 年，这段年限本身就是雷暴统计的时间口径，故这里不加年份条件，
     * 由调用方只用站号（ThunderstormStation）限定范围。聚合不带 GROUP BY，恒返回一行；
     * 无记录时 AVG 为 NULL，已用 COALESCE 记 0。</p>
     *
     * @param wrapper 站号范围条件，用 {@code Wrappers.lambdaQuery()} 构造
     */
    @Select("""
            SELECT COALESCE(AVG(thunderstorm_days), 0)
            FROM thunderstorm_statistics
            ${ew.customSqlSegment}
            """)
    Double selectAverageThunderstormDays(@Param(Constants.WRAPPER) Wrapper<ThunderstormStatistics> wrapper);
}
