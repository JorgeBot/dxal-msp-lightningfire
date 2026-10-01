package cn.gov.cma.hl.dxal.msp.lightningfire.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 承载体脆弱性·坡度困难度
 *
 * <p>对应表 slope_difficulty_summary，粒度：年份 × 地区（全业务区/地市/县市区），
 * 存放区县指标、困难度区间面积、平均坡度与坡度有效面积。</p>
 *
 * <p>注意：8 个年度均取自同一份 2025 年静态坡度底表（temporal_rule=shared_static_2018_2025），
 * 年度间数值完全相同，并非逐年观测，做时间序列分析时不可当作真实年际变化。</p>
 *
 * <p>联合主键 (assessment_year, region_code)，MyBatis-Plus 的单主键 API（如 {@code selectById} /
 * {@code updateById} / {@code deleteById}）不可用，请使用 Wrapper 条件操作。</p>
 */
@Data
@Accessors(chain = true)
@TableName("slope_difficulty_summary")
public class SlopeDifficultySummary {

    /**
     * 评价年份 2018—2025；各年均为同一份 2025 年坡度底表（temporal_rule=shared_static_2018_2025），
     * 年度间数值完全相同，非逐年观测
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
     * 区县指标（无量纲 0—1）= 源 mean，即 Σ(有效网格坡度困难度×交叠面积) ÷ Σ有效交叠面积；
     * 上界常数 u_slope = 5.51884126663208 度
     */
    @TableField("region_value")
    private Double regionValue;

    /**
     * 坡度困难度落在 [0.0,0.2) 的网格在区内真实交叠面积合计 km²（页面等级名：低）
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
     * 落在 [0.8,1.0]（含上界）的面积合计 km²（页面等级名：极高）
     */
    @TableField("bin_area_km2_5")
    private Double binAreaKm25;

    /**
     * 区内坡度有效部分面积加权平均坡度（度）= slope_sum ÷ slope_valid_area_km2；未做 u_slope 归一化、未截断
     */
    @TableField("slope_mean_degree")
    private Double slopeMeanDegree;

    /**
     * 区内坡度有效（源像元有效且 0 &le; 坡度 &le; 90）面积 km²（源 source_totals.slope_valid_area_km2）
     */
    @TableField("slope_valid_area_km2")
    private Double slopeValidAreaKm2;

    /**
     * 区内「面积 × 坡度」加权和 Σ a×slope_degrees（源 source_totals.slope_sum），量纲 km²·度
     */
    @TableField("slope_sum")
    private Double slopeSum;
}
