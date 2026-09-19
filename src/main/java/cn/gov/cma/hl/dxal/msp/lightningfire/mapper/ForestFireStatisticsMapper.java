package cn.gov.cma.hl.dxal.msp.lightningfire.mapper;

import cn.gov.cma.hl.dxal.msp.lightningfire.dto.lightningfeature.LightningFireDTO;
import cn.gov.cma.hl.dxal.msp.lightningfire.entity.ForestFireStatistics;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 雷击火统计 Mapper
 *
 * <p>对应表 forest_fire_statistics。fire_date 为数据库生成列，实体上已禁止写入。</p>
 */
@Mapper
public interface ForestFireStatisticsMapper extends BaseMapper<ForestFireStatistics> {

    /**
     * 按条件查询雷击火点位（时间轴 series），按发现时间升序。
     *
     * <p>只取前端绘图所需字段；无匹配数据时返回空列表。</p>
     */
    @Select("""
            SELECT
                discovered_at,
                longitude,
                latitude,
                total_area,
                fire_unit
            FROM forest_fire_statistics
            ${ew.customSqlSegment}
            ORDER BY discovered_at
            """)
    List<LightningFireDTO> selectLightningFireSeries(@Param(Constants.WRAPPER) Wrapper<ForestFireStatistics> wrapper);
}
