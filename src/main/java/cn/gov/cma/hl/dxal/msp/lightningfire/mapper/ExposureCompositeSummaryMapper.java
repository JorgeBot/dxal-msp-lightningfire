package cn.gov.cma.hl.dxal.msp.lightningfire.mapper;

import cn.gov.cma.hl.dxal.msp.lightningfire.entity.ExposureCompositeSummary;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 承载体暴露度·综合暴露度 E Mapper
 *
 * <p>对应表 exposure_composite_summary。联合主键 (assessment_year, region_code, scheme_id)，
 * 单主键 API（{@code selectById} / {@code updateById} / {@code deleteById}）不可用，请使用 Wrapper 条件操作。</p>
 */
@Mapper
public interface ExposureCompositeSummaryMapper extends BaseMapper<ExposureCompositeSummary> {
}
