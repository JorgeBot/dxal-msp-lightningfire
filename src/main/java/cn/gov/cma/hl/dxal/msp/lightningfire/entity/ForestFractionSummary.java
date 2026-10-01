package cn.gov.cma.hl.dxal.msp.lightningfire.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 承载体暴露度·森林覆盖暴露率
 *
 * <p>对应表 forest_fraction_summary，粒度：年份 × 地区（全业务区/地市/县市区），
 * 存放区县指标、森林覆盖率区间面积、森林类型面积。数据覆盖 2018—2025。</p>
 *
 * <p>联合主键 (assessment_year, region_code)，MyBatis-Plus 的单主键 API（如 {@code selectById} /
 * {@code updateById} / {@code deleteById}）不可用，请使用 Wrapper 条件操作。</p>
 */
@Data
@Accessors(chain = true)
@TableName("forest_fraction_summary")
public class ForestFractionSummary {

    /**
     * 评价年份 2018—2025
     */
    @TableField("assessment_year")
    private Short assessmentYear;

    /**
     * 行政区划代码（6 位，GB/T 2260）：230000=全省；230100、230200…232700=地市（原 4 位地市码后补 00）；
     * 230102、230103…=县市区
     */
    @TableField("region_code")
    private Integer regionCode;

    /**
     * 地区名称
     */
    @TableField("region_name")
    private String regionName;

    /**
     * province=全业务区（全省）；city=地市；county=县市区
     */
    @TableField("region_level")
    private String regionLevel;

    /**
     * 区县指标（%）= 源森林面积 ÷ 该区评价范围内总面积 × 100
     */
    @TableField("region_value")
    private Double regionValue;

    /**
     * 森林覆盖暴露率落在 [0.0,0.2) 的网格在区内真实交叠面积合计 km²
     */
    @TableField("bin_area_km2_1")
    private Double binAreaKm21;

    /**
     * 落在 [0.2,0.4) 的面积合计 km²
     */
    @TableField("bin_area_km2_2")
    private Double binAreaKm22;

    /**
     * 落在 [0.4,0.6) 的面积合计 km²
     */
    @TableField("bin_area_km2_3")
    private Double binAreaKm23;

    /**
     * 落在 [0.6,0.8) 的面积合计 km²
     */
    @TableField("bin_area_km2_4")
    private Double binAreaKm24;

    /**
     * 落在 [0.8,1.0]（含上界）的面积合计 km²
     */
    @TableField("bin_area_km2_5")
    private Double binAreaKm25;

    /**
     * 常绿针叶林面积 km²
     */
    @TableField("type1_area_km2")
    private Double type1AreaKm2;

    /**
     * 常绿阔叶林面积 km²
     */
    @TableField("type2_area_km2")
    private Double type2AreaKm2;

    /**
     * 落叶针叶林面积 km²
     */
    @TableField("type3_area_km2")
    private Double type3AreaKm2;

    /**
     * 落叶阔叶林面积 km²
     */
    @TableField("type4_area_km2")
    private Double type4AreaKm2;

    /**
     * 混交林面积 km²
     */
    @TableField("type5_area_km2")
    private Double type5AreaKm2;

    /**
     * 指标平均值（源 regions[].mean）：区内有效格按真实交叠面积加权的森林覆盖暴露率均值，0—1，未乘 100；
     * 与 region_value（= 已识别森林面积 ÷ 评价范围总面积 × 100，百分比）口径不同，不能互相替代
     */
    @TableField("mean")
    private Double mean;
}
