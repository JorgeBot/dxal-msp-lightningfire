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
 *
 * <p>该表只有区县名与地级市名（无区划代码），按行政区查询时用地区名匹配；
 * 综合风险评估与区划模块按年份区间取出逐日评分后自行按地区名分组聚合。</p>
 */
@Mapper
public interface DangerousnessStatisticsMapper extends BaseMapper<DangerousnessStatistics> {
}
