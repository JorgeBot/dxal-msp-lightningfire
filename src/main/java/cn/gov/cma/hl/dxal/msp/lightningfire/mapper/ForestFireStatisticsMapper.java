package cn.gov.cma.hl.dxal.msp.lightningfire.mapper;

import cn.gov.cma.hl.dxal.msp.lightningfire.entity.ForestFireStatistics;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 雷击火统计 Mapper
 *
 * <p>对应表 forest_fire_statistics。fire_date 为数据库生成列，实体上已禁止写入。</p>
 */
@Mapper
public interface ForestFireStatisticsMapper extends BaseMapper<ForestFireStatistics> {
}
