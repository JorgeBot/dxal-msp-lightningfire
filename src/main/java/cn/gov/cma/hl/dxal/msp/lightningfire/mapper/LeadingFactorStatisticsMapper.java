package cn.gov.cma.hl.dxal.msp.lightningfire.mapper;

import cn.gov.cma.hl.dxal.msp.lightningfire.dto.lightningfeature.LeadingFactorDTO;
import cn.gov.cma.hl.dxal.msp.lightningfire.entity.LeadingFactorStatistics;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 主导因子统计 Mapper
 *
 * <p>对应表 leading_factor_statistics。该表未定义主键，
 * 因此不使用 selectById / updateById / deleteById 等依赖主键的方法，
 * 请通过 QueryWrapper 条件查询。</p>
 */
@Mapper
public interface LeadingFactorStatisticsMapper extends BaseMapper<LeadingFactorStatistics> {

    /**
     * 统计区间内各环境因子主导权重的均值。
     *
     * <p>权重已由上游计算完成并按日存入各因子列，此查询只做区间聚合。
     * avg() 自动忽略 NULL，区间内无数据或整列为 NULL 时结果为 NULL，统一 COALESCE 为 0。
     * 聚合查询无 GROUP BY，恒返回一行。</p>
     */
    @Select("""
            SELECT
                COALESCE(avg(rhu), 0) AS relative_humidity,
                COALESCE(avg(tem), 0) AS temperature,
                COALESCE(avg(rain), 0) AS rain,
                COALESCE(avg(dem), 0) AS dem,
                COALESCE(avg(fmc), 0) AS fmc,
                COALESCE(avg(slope), 0) AS slope,
                COALESCE(avg(wind_speed), 0) AS wind_speed
            FROM leading_factor_statistics
            ${ew.customSqlSegment}
            """)
    LeadingFactorDTO selectAverageLeadingFactor(@Param(Constants.WRAPPER) Wrapper<LeadingFactorStatistics> wrapper);
}
