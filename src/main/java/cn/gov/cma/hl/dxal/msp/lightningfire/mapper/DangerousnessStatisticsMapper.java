package cn.gov.cma.hl.dxal.msp.lightningfire.mapper;

import cn.gov.cma.hl.dxal.msp.lightningfire.entity.DangerousnessStatistics;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 危险性统计 Mapper
 *
 * <p>对应表 dangerousness_statistics。该表未定义主键，仅 {@code selectList} / {@code selectOne} /
 * {@code selectCount} / {@code selectMaps} 等基于 Wrapper 的方法可用；{@code selectById}、
 * {@code updateById}、{@code deleteById} 等依赖主键的方法不可用。</p>
 */
@Mapper
public interface DangerousnessStatisticsMapper extends BaseMapper<DangerousnessStatistics> {
}
