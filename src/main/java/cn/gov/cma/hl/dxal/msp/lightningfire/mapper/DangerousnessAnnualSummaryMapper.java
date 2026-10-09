package cn.gov.cma.hl.dxal.msp.lightningfire.mapper;

import cn.gov.cma.hl.dxal.msp.lightningfire.entity.DangerousnessAnnualSummary;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 危险性 H 年度汇总 Mapper
 *
 * <p>对应表 dangerousness_annual_summary。该表未声明主键，仅 {@code selectList} / {@code selectOne} /
 * {@code selectCount} / {@code selectMaps} 等基于 Wrapper 的方法可用；{@code selectById}、
 * {@code updateById}、{@code deleteById} 等依赖主键的方法不可用。</p>
 *
 * <p>按「年份 + region_code」取一行即可：综合风险评估与区划模块用本表的 mean 作为分项指数中的
 * 危险性 H（0—1），与逐日评分表 dangerousness_statistics 的 0—100 评分各自独立。</p>
 */
@Mapper
public interface DangerousnessAnnualSummaryMapper extends BaseMapper<DangerousnessAnnualSummary> {
}
