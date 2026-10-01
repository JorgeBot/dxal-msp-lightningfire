package cn.gov.cma.hl.dxal.msp.lightningfire.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 承载体暴露度·道路暴露密度
 *
 * <p>对应表 road_exposure_summary，粒度：年份 × 地区（全业务区/地市/县市区），
 * 存放区县指标、道路暴露密度区间面积、道路长度。数据覆盖 2018—2024（源包无 2025 年文件）。</p>
 *
 * <p>联合主键 (assessment_year, region_code)，MyBatis-Plus 的单主键 API（如 {@code selectById} /
 * {@code updateById} / {@code deleteById}）不可用，请使用 Wrapper 条件操作。</p>
 */
@Data
@Accessors(chain = true)
@TableName("road_exposure_summary")
public class RoadExposureSummary {

    /**
     * 评价年份 2018—2024（源包无 2025 年文件）
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
     * 区县指标 km/km² = 区内道路长度 ÷ 该区评价范围内总面积（未归一化、未乘 100）
     */
    @TableField("region_value")
    private Double regionValue;

    /**
     * 归一化道路暴露密度落在 [0.0,0.2) 的网格在区内真实交叠面积合计 km²（对应道路密度 [0, 0.34412) km/km²）
     */
    @TableField("bin_area_km2_1")
    private Double binAreaKm21;

    /**
     * 归一化 [0.2,0.4) 的面积合计 km²（对应密度 [0.34412, 0.68824)）
     */
    @TableField("bin_area_km2_2")
    private Double binAreaKm22;

    /**
     * 归一化 [0.4,0.6) 的面积合计 km²（对应密度 [0.68824, 1.03236)）
     */
    @TableField("bin_area_km2_3")
    private Double binAreaKm23;

    /**
     * 归一化 [0.6,0.8) 的面积合计 km²（对应密度 [1.03236, 1.37648)）
     */
    @TableField("bin_area_km2_4")
    private Double binAreaKm24;

    /**
     * 归一化 [0.8,1.0]（含上界）的面积合计 km²（对应密度 [1.37648, 1.72060] km/km²）
     */
    @TableField("bin_area_km2_5")
    private Double binAreaKm25;

    /**
     * 区内道路长度合计 km（源 source_totals.road_length_km，按网格交叠裁切后求和）
     */
    @TableField("road_length_km")
    private Double roadLengthKm;

    /**
     * 指标平均值（源 regions[].mean）：区内有效格按真实交叠面积加权的归一化道路暴露密度均值，0—1，未乘 100；
     * 与 region_value（= 道路长度÷区面积，km/km²）口径不同
     */
    @TableField("mean")
    private Double mean;
}
