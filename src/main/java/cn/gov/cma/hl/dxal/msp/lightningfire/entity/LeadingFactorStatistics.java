package cn.gov.cma.hl.dxal.msp.lightningfire.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDate;

/**
 * 主导因子统计
 *
 * <p>对应表 leading_factor_statistics，该表未定义主键，
 * 因此仅支持条件查询等不依赖主键的操作。</p>
 */
@Data
@Accessors(chain = true)
@TableName("leading_factor_statistics")
public class LeadingFactorStatistics {

    /**
     * 日期，年月日
     */
    @TableField("date")
    private LocalDate date;

    /**
     * 相对湿度
     */
    @TableField("rhu")
    private Double rhu;

    /**
     * 温度
     */
    @TableField("tem")
    private Double tem;

    /**
     * 降水
     */
    @TableField("rain")
    private Double rain;

    /**
     * 地面高程
     */
    @TableField("dem")
    private Double dem;

    /**
     * 可燃物含水率
     */
    @TableField("fmc")
    private Double fmc;

    /**
     * 坡度
     */
    @TableField("slope")
    private Double slope;

    /**
     * 风速
     */
    @TableField("wind_speed")
    private Double windSpeed;

    /**
     * 区域/行政单位
     */
    @TableField("region")
    private String region;
}
