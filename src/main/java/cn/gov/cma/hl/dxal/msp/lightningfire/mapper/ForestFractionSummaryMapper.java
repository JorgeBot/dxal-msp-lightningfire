package cn.gov.cma.hl.dxal.msp.lightningfire.mapper;

import cn.gov.cma.hl.dxal.msp.lightningfire.entity.ForestFractionSummary;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 承载体暴露度·森林覆盖暴露率 Mapper
 *
 * <p>对应表 forest_fraction_summary。联合主键 (assessment_year, region_code)，
 * 单主键 API（{@code selectById} / {@code updateById} / {@code deleteById}）不可用，请使用 Wrapper 条件操作。</p>
 */
@Mapper
public interface ForestFractionSummaryMapper extends BaseMapper<ForestFractionSummary> {
}
