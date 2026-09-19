package cn.gov.cma.hl.dxal.msp.lightningfire.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDate;

/**
 * 相关系数统计
 *
 * <p>对应表 correlation_coefficient_statistics，该表未定义主键，
 * 因此仅支持条件查询等不依赖主键的操作。</p>
 *
 * <p>注意：各因子列的 DDL 注释虽为物理量名（相对湿度、温度、降水…），但实际存放的是
 * 已由上游计算完成、按日一条的 Pearson 相关系数（取值 -1 ~ 1）；
 * 且 region 实际为空，mean_intensity / lightning_count 基本为空、不参与相关计算。</p>
 */
@Data
@Accessors(chain = true)
@TableName("correlation_coefficient_statistics")
public class CorrelationCoefficientStatistics {

    /**
     * 日期，年月日
     */
    @TableField("date")
    private LocalDate date;

    /**
     * 区域/行政单位
     */
    @TableField("region")
    private String region;

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
     * 平均地闪强度
     */
    @TableField("mean_intensity")
    private Double meanIntensity;

    /**
     * 地闪次数，取值需大于等于 0
     */
    @TableField("lightning_count")
    private Long lightningCount;
}
