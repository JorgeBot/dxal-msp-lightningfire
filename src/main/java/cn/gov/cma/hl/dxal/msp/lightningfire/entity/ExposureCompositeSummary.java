package cn.gov.cma.hl.dxal.msp.lightningfire.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 承载体暴露度·综合暴露度 E
 *
 * <p>对应表 exposure_composite_summary，粒度：年份 × 地区（全业务区/地市/县市区）× AHP 方案，
 * 存放指标对比值、比例面积与分项贡献。{@code E = wF*f + wS*s + wD*d}，线性可区域聚合；
 * 数据覆盖 2018—2024 共 7 年，2025 年无（缺道路暴露密度）。</p>
 *
 * <p>业务约束（源数据说明）：</p>
 * <ul>
 *   <li>{@code region_code} 为行政区划码（integer，6 位 GB/T 2260）：230000=全业务区（全省）；
 *       230100 等 4 位地市码已后补 00；230102 等=县市区。</li>
 *   <li>{@code region_value} 为选区面积加权均值，取值 0—1，非百分数（源 display_scale=1，不乘 100）；
 *       其镜像列 {@code mean} 与之同源同值，出图统一取 {@code region_value}。</li>
 *   <li>{@code coverage} 为有效面积覆盖率；页面区县对比条要求 &ge; 0.99999，实测有 44 个区不达标。</li>
 *   <li>{@code bin_area_km2_5} 实测 7 年 × 125 县区恒为 0。</li>
 *   <li>{@code source_file} 为源栅格文件名，同一年、同一方案的 139 行共用同一个值；
 *       栅格本体在 {@code 交接包根目录/{评价年份}/{source_file}}，是页面取 E 栅格的唯一依据。</li>
 *   <li>三列分项贡献之和与 {@code region_value} 只是近似相等（源表注释亦为「≈」）：6811 行实测偏差
 *       中位数 0.00023、95 分位 0.0047、最大 0.0559（2019 年向阳区 s1/s3/s5），不能相互反推；
 *       贡献率与 AHP 权重列当前表内不存储，由上游或前端计算。</li>
 * </ul>
 *
 * <p>联合主键 (assessment_year, region_code, scheme_id)，MyBatis-Plus 的单主键 API（如
 * {@code selectById} / {@code updateById} / {@code deleteById}）不可用，请使用 Wrapper 条件操作。</p>
 */
@Data
@Accessors(chain = true)
@TableName("exposure_composite_summary")
public class ExposureCompositeSummary {

    /**
     * 评价年份 2018—2024（2025 无 E：缺 road_exposure）
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
     * AHP 方案号 0—6：0=AHP基准矩阵；1=第1/2项=1；2=第1/2项=3；3=第1/3项=1；
     * 4=第1/3项=3；5=第2/3项=0.5；6=第2/3项=2
     */
    @TableField("scheme_id")
    private Short schemeId;

    /**
     * 源栅格文件名（Excel B 列原值，如 E_s0_02064e59b488.tif）；同目录同名 .json 为其统计文件。
     *
     * <p>方案号 0—6 由文件名 {@code /^E_s(\d+)_/} 取出，与 scheme_id 一致；同一年、同一方案下
     * 139 个行政区共用同一个 source_file（方案不同文件名不同）。栅格文件的完整路径为
     * {@code 交接包根目录/{评价年份}/{source_file}}，页面取栅格时按此拼接，不手写文件名哈希。</p>
     */
    @TableField("source_file")
    private String sourceFile;

    /**
     * 指标对比：E 的选区面积加权均值，0—1（源 display_scale=1，不乘 100，不是百分数）
     */
    @TableField("region_value")
    private Double regionValue;

    /**
     * 指标平均值（源 regions[].mean）：E 主栅格按选区真实交叠面积加权的均值，0—1，NA 不当零。
     *
     * <p>与 region_value 同源同值（实测 6811 行逐行相等、无一处不等），两列都保留以对齐源数据口径；
     * 页面出图统一取 region_value，本列仅作映射，不参与计算。</p>
     */
    @TableField("mean")
    private Double mean;

    /**
     * 选区总面积 km²
     */
    @TableField("region_area_km2")
    private Double regionAreaKm2;

    /**
     * E 有效交叠面积 km²（三项共同有效），等于五档面积之和，是占比分母
     */
    @TableField("valid_area_km2")
    private Double validAreaKm2;

    /**
     * 有效面积覆盖率 = valid_area_km2/region_area_km2；页面区县对比条要求 &ge; 0.99999，本指标有 44 个区不达标
     */
    @TableField("coverage")
    private Double coverage;

    /**
     * 比例面积：E 落在 [0.0,0.2) 的网格在区内真实交叠面积合计 km²
     */
    @TableField("bin_area_km2_1")
    private Double binAreaKm21;

    /**
     * 比例面积：E 落在 [0.2,0.4) 的面积合计 km²
     */
    @TableField("bin_area_km2_2")
    private Double binAreaKm22;

    /**
     * 比例面积：E 落在 [0.4,0.6) 的面积合计 km²
     */
    @TableField("bin_area_km2_3")
    private Double binAreaKm23;

    /**
     * 比例面积：E 落在 [0.6,0.8) 的面积合计 km²
     */
    @TableField("bin_area_km2_4")
    private Double binAreaKm24;

    /**
     * 比例面积：E 落在 [0.8,1.0]（含上界）的面积合计 km²；实测 7 年 × 125 县区恒为 0
     */
    @TableField("bin_area_km2_5")
    private Double binAreaKm25;

    /**
     * 分项贡献（森林）：wF × E掩膜内森林分项归一化面积加权均值；三列之和 = region_value
     */
    @TableField("contrib_forest")
    private Double contribForest;

    /**
     * 分项贡献（居民地）：wS × E掩膜内居民地分项归一化面积加权均值
     */
    @TableField("contrib_settlement")
    private Double contribSettlement;

    /**
     * 分项贡献（道路）：wD × E掩膜内道路分项归一化面积加权均值
     */
    @TableField("contrib_road")
    private Double contribRoad;
}
