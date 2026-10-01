package cn.gov.cma.hl.dxal.msp.lightningfire.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 承载体脆弱性·可燃物负荷指数
 *
 * <p>对应表 fuel_load_index_summary，粒度：年份 × 地区（全业务区/地市/县市区），
 * 存放指标均值、归一化指标值、指数分档面积、可燃物载量与归一化参考。数据覆盖 2018—2022；
 * 2023—2025 源包缺文件（index.json 声明 missing_source「缺少该年度源数据，不跨年替代」）。</p>
 *
 * <p>联合主键 (assessment_year, region_code)，MyBatis-Plus 的单主键 API（如 {@code selectById} /
 * {@code updateById} / {@code deleteById}）不可用，请使用 Wrapper 条件操作。</p>
 */
@Data
@Accessors(chain = true)
@TableName("fuel_load_index_summary")
public class FuelLoadIndexSummary {

    /**
     * 年份 2018—2022；2023—2025 源包缺文件，index.json 声明 missing_source「缺少该年度源数据，不跨年替代」
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
     * 区域名称
     */
    @TableField("region_name")
    private String regionName;

    /**
     * 区域等级：province=全业务区（全省）；city=地市；county=县市区
     */
    @TableField("region_level")
    private String regionLevel;

    /**
     * 可燃物负荷指数落在 [0.0,0.2) 的网格在区内真实交叠面积合计 km²（页面等级名：低）
     */
    @TableField("bin_area_km2_1")
    private Double binAreaKm21;

    /**
     * 可燃物负荷指数落在 [0.2,0.4) 的面积合计 km²（页面等级名：较低）
     */
    @TableField("bin_area_km2_2")
    private Double binAreaKm22;

    /**
     * 可燃物负荷指数落在 [0.4,0.6) 的面积合计 km²（页面等级名：较高）
     */
    @TableField("bin_area_km2_3")
    private Double binAreaKm23;

    /**
     * 可燃物负荷指数落在 [0.6,0.8) 的面积合计 km²（页面等级名：高）
     */
    @TableField("bin_area_km2_4")
    private Double binAreaKm24;

    /**
     * 可燃物负荷指数落在 [0.8,1.0]（含上界）的面积合计 km²（页面等级名：极高）
     */
    @TableField("bin_area_km2_5")
    private Double binAreaKm25;

    /**
     * 可燃物载量：区内森林载量面积加权平均 t/ha（源 source_fuel_mean_t_ha）= Σa×load ÷ A_forest_valid；无载量地区为 NULL
     */
    @TableField("fuel_mean_t_ha")
    private Double fuelMeanTHa;

    /**
     * 归一化参考：固定参考上界 u_load = 25.501176834106445 t/ha（源 index.json reference.upper，2018 固定 P95），
     * 每行同值，不随地区与年份变化
     */
    @TableField("normalization_reference_t_ha")
    private Double normalizationReferenceTHa;

    /**
     * 指标值：归一化可燃物负荷指数（无量纲 0—1）= min(fuel_mean_t_ha ÷ normalization_reference_t_ha, 1)
     * （源 source_mean_normalized_diagnostic）；区内森林面积为 0 时为 NULL
     */
    @TableField("region_value")
    private Double regionValue;

    /**
     * 指标平均值（源 regions[].mean）：区内有效格按真实交叠面积加权的归一化可燃物负荷指数均值，0—1；
     * 无有效格（valid_area_km2 = 0）时为 NULL
     *
     * <p>页面「区县指标对比」柱状图取本列（见《页面数据读取对照说明》第 3、6 页）；
     * 与 region_value（源 source_mean_normalized_diagnostic）、fuel_mean_t_ha（源 source_fuel_mean_t_ha，t/ha）
     * 口径不同，三者实测互不相等，不能互相替代。</p>
     */
    @TableField("mean")
    private Double mean;
}
