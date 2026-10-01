package cn.gov.cma.hl.dxal.msp.lightningfire.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 承载体脆弱性·植被燃烧敏感度
 *
 * <p>对应表 vegetation_sensitivity_summary，粒度：年份 × 地区（全业务区/地市/县市区），
 * 存放区县指标、敏感度区间面积、森林面积与各森林类型面积。数据覆盖 2018—2025；
 * 2025 沿用 2024 年 LC_Type1 森林底表。</p>
 *
 * <p>注意：区内森林面积为 0 时，{@code region_value} 为 NULL，不是 0。</p>
 *
 * <p>联合主键 (assessment_year, region_code)，MyBatis-Plus 的单主键 API（如 {@code selectById} /
 * {@code updateById} / {@code deleteById}）不可用，请使用 Wrapper 条件操作。</p>
 */
@Data
@Accessors(chain = true)
@TableName("vegetation_sensitivity_summary")
public class VegetationSensitivitySummary {

    /**
     * 评价年份 2018—2025；2025 沿用 2024 年 LC_Type1 森林底表
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
     * 区县指标（无量纲，值域 [0.8,1.0]）= veg_sum_km2 ÷ forest_area_km2，即森林面积加权敏感度；
     * 区内森林面积为 0 时为 NULL
     */
    @TableField("region_value")
    private Double regionValue;

    /**
     * 敏感度落在 [0.80,0.84) 的网格在区内真实交叠面积合计 km²（页面等级名：低）
     */
    @TableField("bin_area_km2_1")
    private Double binAreaKm21;

    /**
     * 落在 [0.84,0.88) 的面积合计 km²（页面等级名：较低）
     */
    @TableField("bin_area_km2_2")
    private Double binAreaKm22;

    /**
     * 落在 [0.88,0.92) 的面积合计 km²（页面等级名：较高）
     */
    @TableField("bin_area_km2_3")
    private Double binAreaKm23;

    /**
     * 落在 [0.92,0.96) 的面积合计 km²（页面等级名：高）
     */
    @TableField("bin_area_km2_4")
    private Double binAreaKm24;

    /**
     * 落在 [0.96,1.00]（含上界）的面积合计 km²（页面等级名：极高）
     */
    @TableField("bin_area_km2_5")
    private Double binAreaKm25;

    /**
     * 区内森林面积 A_forest km²（源 source_totals.forest_area_km2），region_value 的分母，
     * 与 forest_fraction_summary.region_value 同源同口径
     */
    @TableField("forest_area_km2")
    private Double forestAreaKm2;

    /**
     * 森林类型 1 常绿针叶林面积 km²（源 source_totals.type1_area_km2，敏感度取值 1.0）
     */
    @TableField("type1_area_km2")
    private Double type1AreaKm2;

    /**
     * 森林类型 2 常绿阔叶林面积 km²（源 source_totals.type2_area_km2，敏感度取值 0.8）
     */
    @TableField("type2_area_km2")
    private Double type2AreaKm2;

    /**
     * 森林类型 3 落叶针叶林面积 km²（源 source_totals.type3_area_km2，敏感度取值 1.0）
     */
    @TableField("type3_area_km2")
    private Double type3AreaKm2;

    /**
     * 森林类型 4 落叶阔叶林面积 km²（源 source_totals.type4_area_km2，敏感度取值 0.8）
     */
    @TableField("type4_area_km2")
    private Double type4AreaKm2;

    /**
     * 森林类型 5 混交林面积 km²（源 source_totals.type5_area_km2，敏感度取值 0.9）
     */
    @TableField("type5_area_km2")
    private Double type5AreaKm2;

    /**
     * 指标平均值
     */
    @TableField("mean")
    private Double mean;
}
