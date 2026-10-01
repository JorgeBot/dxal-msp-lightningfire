package cn.gov.cma.hl.dxal.msp.lightningfire.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 承载体脆弱性·道路接近条件
 *
 * <p>对应表 road_access_summary，粒度：年份 × 地区（全业务区/地市/县市区），
 * 存放区县指标与接近条件区间面积。数据覆盖 2018—2024；2025 年源包缺文件
 * （index.json 声明 missing_source「缺少该年度源数据，不跨年替代」）。</p>
 *
 * <p>指标 = clip(Σ(a_250m × 到道路距离) ÷ A_unit ÷ u_distance, 0, 1)，
 * 归一化参考上界 u_distance = 8.311621982886752 km（index.json 的
 * indicators.road_access.reference，与道路暴露度共用同一份静态道路参考）。</p>
 *
 * <p>联合主键 (assessment_year, region_code)，MyBatis-Plus 的单主键 API（如 {@code selectById} /
 * {@code updateById} / {@code deleteById}）不可用，请使用 Wrapper 条件操作。</p>
 */
@Data
@Accessors(chain = true)
@TableName("road_access_summary")
public class RoadAccessSummary {

    /**
     * 评价年份 2018—2024；2025 年源包缺文件，不跨年替代
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
     * 地区名称（源 regions[].region_name，「全业务区」入库时统一记为「全省」）
     */
    @TableField("region_name")
    private String regionName;

    /**
     * province=全业务区（全省）；city=地市；county=县市区
     */
    @TableField("region_level")
    private String regionLevel;

    /**
     * 区县指标（无量纲 0—1）= 源 regions[].mean，即区内有效格按真实交叠面积加权的道路接近条件均值，
     * 未乘 100
     */
    @TableField("region_value")
    private Double regionValue;

    /**
     * 道路接近条件落在 [0.0,0.2) 的网格在区内真实交叠面积合计 km²（页面等级名：低）
     */
    @TableField("bin_area_km2_1")
    private Double binAreaKm21;

    /**
     * 落在 [0.2,0.4) 的面积合计 km²（页面等级名：较低）
     */
    @TableField("bin_area_km2_2")
    private Double binAreaKm22;

    /**
     * 落在 [0.4,0.6) 的面积合计 km²（页面等级名：较高）
     */
    @TableField("bin_area_km2_3")
    private Double binAreaKm23;

    /**
     * 落在 [0.6,0.8) 的面积合计 km²（页面等级名：高）
     */
    @TableField("bin_area_km2_4")
    private Double binAreaKm24;

    /**
     * 落在 [0.8,1.0]（含上界）的面积合计 km²（页面等级名：极高）；五档之和即指标区总面积
     */
    @TableField("bin_area_km2_5")
    private Double binAreaKm25;
}
