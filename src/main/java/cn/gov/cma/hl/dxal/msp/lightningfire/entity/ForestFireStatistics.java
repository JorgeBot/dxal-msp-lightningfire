package cn.gov.cma.hl.dxal.msp.lightningfire.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 雷击火统计
 *
 * <p>对应表 forest_fire_statistics，其中 fire_date 为
 * GENERATED ALWAYS AS ((discovered_at)::date) STORED 生成列，只读不可写。</p>
 */
@Data
@Accessors(chain = true)
@TableName("forest_fire_statistics")
public class ForestFireStatistics {

    /**
     * 自增主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 起火单位
     */
    @TableField("fire_unit")
    private String fireUnit;

    /**
     * 经度，取值 -180 至 180
     */
    @TableField("longitude")
    private BigDecimal longitude;

    /**
     * 纬度，取值 -90 至 90
     */
    @TableField("latitude")
    private BigDecimal latitude;

    /**
     * 发现时间
     */
    @TableField("discovered_at")
    private LocalDateTime discoveredAt;

    /**
     * 发现日期，由 discovered_at 生成，数据库维护，禁止写入
     */
    @TableField(value = "fire_date", insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDate fireDate;

    /**
     * 火灾面积
     */
    @TableField("total_area")
    private BigDecimal totalArea;

    /**
     * 行政单位
     */
    @TableField("region")
    private String region;
}
