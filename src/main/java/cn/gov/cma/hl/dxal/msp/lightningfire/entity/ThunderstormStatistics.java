package cn.gov.cma.hl.dxal.msp.lightningfire.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 大兴安岭雷暴统计
 *
 * <p>对应表 thunderstorm_statistics，唯一约束 (station_code, observation_year)。</p>
 */
@Data
@Accessors(chain = true)
@TableName("thunderstorm_statistics")
public class ThunderstormStatistics {

    /**
     * 自增主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 站号，按文本保存，取值需满足 ^[0-9]{5}$
     */
    @TableField("station_code")
    private String stationCode;

    /**
     * 站名/行政单位
     */
    @TableField("region")
    private String region;

    /**
     * 年份，取值 1 至 9999（数据库 smallint）
     */
    @TableField("observation_year")
    private Short observationYear;

    /**
     * 年雷暴日数，单位：天，取值 0 至当年天数（平年 365、闰年 366）（数据库 smallint）
     */
    @TableField("thunderstorm_days")
    private Short thunderstormDays;
}
