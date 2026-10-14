package cn.gov.cma.hl.dxal.msp.lightningfire.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 危险性实时分析统计
 *
 * <p>对应表 dangerousness_realtime_statistics，粒度：日期 × 区县，主键 (date, county_code)，
 * 一行一个区县一天的实时危险性结果。实测 2026-09-01—2026-09-30 共 30 天、每天 125 个县级单元、
 * 合计 3750 行（覆盖全省 13 个地市）；日期是实时 / 预报结果的投递日期，与逐日历史评分表
 * {@link DangerousnessStatistics} 的 2018—2024 不是同一批数据、也不能拼接成一条时间序列。</p>
 *
 * <p>主键是复合主键（date + county_code），MyBatis-Plus 的单主键 API（{@code selectById} /
 * {@code updateById} / {@code deleteById}）不可用，请使用 Wrapper 条件操作。</p>
 *
 * <p>列语义：</p>
 * <ul>
 *   <li>{@code score}：危险性评分，numeric(10,4)，实测 50.6110—91.0054，数值越高越危险，
 *       与逐日评分表的 score 同为 0—100 百分制；</li>
 *   <li>{@code dangerousness_level}：评分等级文案，实测四种取值 III 中等（1260 行）、IV 较高（2190 行）、
 *       V 高（240 行）、无数据（60 行）。等级由底层危险性 H 的原始分档给出，与本表 0—100 评分的分段
 *       不是逐点重合（实测 III 为 50.6110—59.9789、IV 为 61.3939—77.8645、V 为 82.0936—91.0054，
 *       段间留有空隙），故接口直接返回本列、不按分数重算等级；</li>
 *   <li>{@code temperature} / {@code relative_humidity} / {@code wind_speed} / {@code rain}：
 *       与评分同时算出的四个要素值，numeric(12,6)，量纲与同名气象要素一致（气温、相对湿度、风速、降水），
 *       表内没有单位列，接口原样返回、不做换算；</li>
 *   <li>缺数：{@code score} 与四个要素值、{@code dangerousness_level} 同进同出——实测 60 行
 *       （鹤岗市向阳区、工农区各 30 天）完全没有结果，score 与四个要素为 NULL、等级为「无数据」，
 *       接口照常返回该区县、值留 null，不补 0。</li>
 * </ul>
 *
 * <p>区划口径：{@code county_code} 是 varchar(6)，绝大多数取值与
 * {@link cn.gov.cma.hl.dxal.msp.lightningfire.constant.RegionCode} 一致，但大兴安岭地区有三个区用的是
 * 非国标码（232702=松岭区、232703=新林区、232704=呼中区，国标为 232762 / 232763 / 232764），
 * 直接按本列与区划枚举对齐会让这三个区落不进大兴安岭地区的范围。因此定位行政区一律用
 * {@code city}（地市名）+ {@code region}（区县名）两个名称列，与逐日评分表同一套做法，
 * 返回给前端的区划码取 {@link cn.gov.cma.hl.dxal.msp.lightningfire.constant.RegionCode} 的国标码。</p>
 */
@Data
@Accessors(chain = true)
@TableName("dangerousness_realtime_statistics")
public class DangerousnessRealtimeStatistics {

    /**
     * 统计日期（年月日），主键之一；实测只覆盖 2026-09-01—2026-09-30
     */
    @TableField("date")
    private LocalDate date;

    /**
     * 地级市名（实测全省 13 个地市），按行政区定位时的地市依据
     */
    @TableField("city")
    private String cityName;

    /**
     * 区县 6 位行政区划代码（varchar(6)），主键之二
     *
     * <p>不参与区划匹配：大兴安岭地区的松岭区 / 新林区 / 呼中区在本表用的是非国标码
     * 232702 / 232703 / 232704，按本列对齐区划枚举会漏掉这三个区，故行政区一律用
     * {@link #cityName} + {@link #regionName} 的名称定位（见类注释）。</p>
     */
    @TableField("county_code")
    private String countyCode;

    /**
     * 区/县名（数据库列为 region；字段名与列名不同，故显式声明 {@code @TableField}）
     */
    @TableField("region")
    private String regionName;

    /**
     * 危险性评分（0—100 百分制，numeric(10,4)）；无结果为 NULL，不补 0
     */
    @TableField("score")
    private BigDecimal score;

    /**
     * 危险性等级文案（III 中等 / IV 较高 / V 高 / 无数据），直接展示，不由评分重算
     */
    @TableField("dangerousness_level")
    private String dangerousnessLevel;

    /**
     * 要素「气温」的值（numeric(12,6)）；无结果为 NULL
     */
    @TableField("temperature")
    private Double temperature;

    /**
     * 要素「相对湿度」的值（numeric(12,6)）；无结果为 NULL
     */
    @TableField("relative_humidity")
    private Double relativeHumidity;

    /**
     * 要素「风速」的值（numeric(12,6)）；无结果为 NULL
     */
    @TableField("wind_speed")
    private Double windSpeed;

    /**
     * 要素「降水」的值（numeric(12,6)）；无结果为 NULL
     */
    @TableField("rain")
    private Double rain;
}
