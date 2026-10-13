package cn.gov.cma.hl.dxal.msp.lightningfire.mapper;

import cn.gov.cma.hl.dxal.msp.lightningfire.entity.ConditionDistributionDaily;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 县级逐日条件分布 Mapper
 *
 * <p>对应表 condition_distribution_daily。该表主键是 (assessment_date, region_code) 复合主键，
 * 没有单列主键，仅 {@code selectList} / {@code selectOne} / {@code selectCount} / {@code selectMaps}
 * 等基于 Wrapper 的方法可用；{@code selectById}、{@code updateById}、{@code deleteById} 不可用。</p>
 *
 * <p>取数按「日期区间 + region_code 集合」定位：区划码集合由
 * {@link cn.gov.cma.hl.dxal.msp.lightningfire.constant.RegionCode#selectable(Integer)}
 * 给出（选县返回该县、选地市返回所辖县区、选全省返回全部 125 个县级单元），
 * 服务层再在内存里按因子做加权汇总，不在接口里现算格网条件。</p>
 */
@Mapper
public interface ConditionDistributionDailyMapper extends BaseMapper<ConditionDistributionDaily> {
}
