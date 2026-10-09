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
 * <p>对应表 dangerousness_statistics，粒度：日期 × 区/县 × 地级市，一行一个危险性评分
 * （实测 2018-01-01—2024-12-31，199278 行，13 个地级市、96 个区县）。</p>
 *
 * <p>注意：该表未定义主键，因此仅支持条件查询等不依赖主键的操作；
 * 同一天同一区县在不同地市下可能存在多行，按条件查询时需自行限定 city。</p>
 *
 * <p>该表只有区县名（{@code region}）与地级市名（{@code city}）两个文本列，没有行政区划代码，
 * 与 {@code region_code} 口径的区划对照统一由 {@link cn.gov.cma.hl.dxal.msp.lightningfire.constant.RegionCode}
 * 的 label 提供：综合风险评估与区划模块按地区名匹配本表，不按代码过滤。</p>
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
     * 区/县名（数据库列为 region；字段名与列名不同，故显式声明 {@code @TableField}）
     *
     * <p>实测本列只放区县名（如漠河市、加格达奇区），地市名在 {@link #cityName} 列，
     * 省级汇总行不存在；按行政区取数时先按地市定位、再按区县名定位。</p>
     */
    @TableField("region")
    private String regionName;

    /**
     * 地级市名
     *
     * <p>实测为地市名（全省 13 个地市），是区县记录归属地市的唯一依据；区县名在各地市间不重名，
     * 但按行政区取数仍先按本列定位地市。</p>
     */
    @TableField("city")
    private String cityName;

    /**
     * 危险性评分
     *
     * <p>0—100 的百分制评分（实测 0.0000—99.9998），数值越高危险性越高；
     * 综合风险评估与区划模块的分区评分即本列在评价年份内的均值。</p>
     */
    @TableField("score")
    private BigDecimal score;
}
