package cn.gov.cma.hl.dxal.msp.lightningfire.mapper;

import cn.gov.cma.hl.dxal.msp.lightningfire.dto.lightningfeature.GridDTO;
import cn.gov.cma.hl.dxal.msp.lightningfire.dto.lightningfeature.RegionCountDTO;
import cn.gov.cma.hl.dxal.msp.lightningfire.dto.lightningfeature.TimeSeriesDTO;
import cn.gov.cma.hl.dxal.msp.lightningfire.entity.DailyRegionStatistics;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface DailyRegionStatisticsMapper extends BaseMapper<DailyRegionStatistics> {

    @Select("""
            SELECT
                COALESCE(SUM(record_count), 0) AS record_count,
                COALESCE(ROUND(SUM(positive_ratio * record_count) / NULLIF(SUM(record_count), 0) * 100, 3), 0) AS avg_positive_ratio_per,
                COALESCE(ROUND(SUM(density * record_count) / NULLIF(SUM(record_count), 0), 3), 0) AS avg_density,
                COALESCE(ROUND(SUM(avg_abs_intensity * record_count) / NULLIF(SUM(record_count), 0), 3), 0) AS avg_abs_intensity
            FROM daily_region_statistics
            ${ew.customSqlSegment}
            """)
    GridDTO selectLightningSummaryByCondition(@Param(Constants.WRAPPER) Wrapper<DailyRegionStatistics> wrapper);

    @Select("""
            SELECT
                TO_CHAR(stat_date, '${periodFormat}') AS period,
                COALESCE(SUM(record_count), 0) AS count
            FROM daily_region_statistics
            ${ew.customSqlSegment}
            GROUP BY TO_CHAR(stat_date, '${periodFormat}')
            ORDER BY MIN(stat_date)
            """)
    List<TimeSeriesDTO> selectTimeSeriesByCondition(@Param(Constants.WRAPPER) Wrapper<DailyRegionStatistics> wrapper,
                                                    @Param("periodFormat") String periodFormat);

    @Select("""
            SELECT
                region,
                COALESCE(SUM(record_count), 0) AS count
            FROM daily_region_statistics
            ${ew.customSqlSegment}
            GROUP BY region
            ORDER BY region
            """)
    List<RegionCountDTO> selectRegionCountByCondition(@Param(Constants.WRAPPER) Wrapper<DailyRegionStatistics> wrapper);
}
