package cn.gov.cma.hl.dxal.msp.lightningfire.mapper;

import cn.gov.cma.hl.dxal.msp.lightningfire.entity.DangerousnessRealtimeStatistics;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 危险性实时分析统计 Mapper
 *
 * <p>对应表 dangerousness_realtime_statistics。主键 (date, county_code) 是复合主键，
 * {@code selectById}、{@code updateById}、{@code deleteById} 等单主键方法不可用；
 * 本模块只用 {@code selectList} 按「日期 + 地市名 / 区县名」取一天的记录（一天最多 125 行）。</p>
 *
 * <p>按行政区过滤时用 {@code city} + {@code region} 两个名称列，不按 {@code county_code}：
 * 该列在大兴安岭地区有三个区是非国标码（见实体注释），按名称定位才不会漏区。</p>
 */
@Mapper
public interface DangerousnessRealtimeStatisticsMapper extends BaseMapper<DangerousnessRealtimeStatistics> {
}
