package cn.gov.cma.hl.dxal.msp.lightningfire.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 危险性统计
 *
 * <p>对应表 dangerousness_statistics，粒度：日期 × 区/县 × 地级市，一行一个危险性评分。</p>
 *
 * <p>注意：该表未定义主键，因此仅支持条件查询等不依赖主键的操作；
 * 同一天同一区县在不同地市下可能存在多行，按条件查询时需自行限定 city。</p>
 */
@Data
@Accessors(chain = true)
@TableName("dangerousness_statistics")
public class DangerousnessStatistics {

    /**
     * 日期（年月日）
     */
    @TableField("date")
    private LocalDate date;

    /**
     * 区/县名
     */
    @TableField("region")
    private String region;

    /**
     * 地级市
     */
    @TableField("city")
    private String city;

    /**
     * 危险性评分
     */
    @TableField("score")
    private BigDecimal score;
}
