package cn.gov.cma.hl.dxal.msp.lightningfire.entity;

import cn.gov.cma.hl.dxal.msp.lightningfire.constant.RegionCode;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 每日各地区强度记录统计
 *
 */
@Data
@Accessors(chain = true)
@TableName("lightning_region_statistics")
public class LightningRegionStatistics {

    /**
     * 统计日期，取原始 datetime 的自然日
     */
    @TableField(value = "stat_date")
    private LocalDate statDate;

    /**
     * 行政区划代码（6 位，GB/T 2260），取值参照 {@link RegionCode}
     */
    @TableField(value = "region_code")
    private Integer regionCode;

    /**
     * 地区名称，对应原始 country 字段
     */
    @TableField(value = "region_name")
    private String regionName;

    /**
     * 地闪记录总数
     */
    @TableField("record_count")
    private Long recordCount;

    /**
     * 平均绝对强度：SUM(ABS(intens)) / 记录数
     */
    @TableField("avg_abs_intensity")
    private BigDecimal avgAbsIntensity;

    /**
     * intens > 0 的记录数 / 总记录数，取值0至1
     */
    @TableField("positive_ratio")
    private BigDecimal positiveRatio;

    /**
     * 记录密度：记录数 / 区域面积，单位条/平方公里
     */
    @TableField("density")
    private BigDecimal density;
}
