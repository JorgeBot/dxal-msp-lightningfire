package cn.gov.cma.hl.dxal.msp.lightningfire.mapper;

import cn.gov.cma.hl.dxal.msp.lightningfire.dto.lightningfeature.CorrelationCoefficientDTO;
import cn.gov.cma.hl.dxal.msp.lightningfire.entity.CorrelationCoefficientStatistics;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;


@Mapper
public interface CorrelationCoefficientStatisticsMapper extends BaseMapper<CorrelationCoefficientStatistics> {


    @Select("""
            SELECT
                COALESCE(avg(rhu), 0) AS relative_humidity,
                COALESCE(avg(tem), 0) AS temperature,
                COALESCE(avg(rain), 0) AS rain,
                COALESCE(avg(dem), 0) AS dem,
                COALESCE(avg(fmc), 0) AS fmc,
                COALESCE(avg(slope), 0) AS slope,
                COALESCE(avg(wind_speed), 0) AS wind_speed
            FROM correlation_coefficient_statistics
            ${ew.customSqlSegment}
            """)
    CorrelationCoefficientDTO selectAverageCorrelationCoefficient(@Param(Constants.WRAPPER) Wrapper<CorrelationCoefficientStatistics> wrapper);
}
