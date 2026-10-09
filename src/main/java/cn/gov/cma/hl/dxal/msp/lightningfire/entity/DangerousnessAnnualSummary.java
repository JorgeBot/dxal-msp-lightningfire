package cn.gov.cma.hl.dxal.msp.lightningfire.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 危险性 H 年度汇总
 *
 * <p>对应表 dangerousness_annual_summary，粒度：年份 × 地区（全省 / 地市 / 县市区），一行一个地区
 * 的年度 H 摘要；来源为《2018-2024年H年度均值与区域分档占比.xlsx》的区域统计 sheet
 * （987 行源数据去重后 973 行 = 139 个地区 × 7 年）。</p>
 *
 * <p>与逐日评分表 {@link DangerousnessStatistics} 的分工：本表是 H 的年度摘要（0—1 归一化指数 +
 * 五档占比），逐日表是 0—100 的评分。综合风险评估与区划模块的区域评分统计取逐日表的 score，
 * 分项指数里的 H 取本表的 {@code mean}，两者不是同一量纲，不能互相替代。</p>
 *
 * <p>业务约束（源数据说明）：</p>
 * <ul>
 *   <li>{@code mean} 为该区年度 H 平均值（0—1，不是百分数）；按有效格 H 均值再按有效格面积加权；
 *       无有效格时为 NULL（实测 139 行中每年只有 101—111 行有值），不补 0，2020 年只有 34 天观测。</li>
 *   <li>{@code read_days} 为实际读取日数（2018=359、2019=359、2020=34、2021=361、2022=363、
 *       2023=365、2024=366），2020 年不能与其他年份直接比较。</li>
 *   <li>{@code bin_ratio_1}—{@code bin_ratio_5} 为各档格点数占比（0—1），五档之和为 1
 *       （源数据存在四舍五入偏差）；分档为 0.2 等宽，末档含上界 1。</li>
 *   <li>本表按 region_code 取数（与逐日表只有地区名不同），region_level 取 province / city / county。</li>
 * </ul>
 *
 * <p>本表未声明主键，MyBatis-Plus 的单主键 API（如 {@code selectById} / {@code updateById}）不可用，
 * 请使用 Wrapper 条件操作。</p>
 */
@Data
@Accessors(chain = true)
@TableName("dangerousness_annual_summary")
public class DangerousnessAnnualSummary {

    /**
     * 评价年份 2018—2024
     */
    @TableField("assessment_year")
    private Short assessmentYear;

    /**
     * 行政区划代码（6 位，GB/T 2260）：230000=全省；230100、230200…232700=地市；230102 等=县市区
     */
    @TableField("region_code")
    private Integer regionCode;

    /**
     * 地区名称（全业务区在 schema 内统一为「全省」）
     */
    @TableField("region_name")
    private String regionName;

    /**
     * 区域等级：province=全省（源「省级」）；city=地市（源「地级」）；county=县市区（源「区县」）
     */
    @TableField("region_level")
    private String regionLevel;

    /**
     * 所属地级市（源 Excel「所属地级市」）；省级行无此值，为 NULL
     */
    @TableField("city")
    private String cityName;

    /**
     * 年度有效格点数；为 0 表示该区没有有效格，H 均值与五档占比全为 NULL
     */
    @TableField("valid_grid_count")
    private Integer validGridCount;

    /**
     * 实际读取日数（2020 年为 34，与其他年份不可直接比较）
     */
    @TableField("read_days")
    private Short readDays;

    /**
     * 年度 H 平均值（0—1，不是百分数）；无有效格时为 NULL，不补 0
     */
    @TableField("mean")
    private Double mean;

    /**
     * 分档占比：H 落在 [0,0.2) 的格点数占比（0—1）
     */
    @TableField("bin_ratio_1")
    private Double binRatio1;

    /**
     * 分档占比：H 落在 [0.2,0.4) 的格点数占比（0—1）
     */
    @TableField("bin_ratio_2")
    private Double binRatio2;

    /**
     * 分档占比：H 落在 [0.4,0.6) 的格点数占比（0—1）
     */
    @TableField("bin_ratio_3")
    private Double binRatio3;

    /**
     * 分档占比：H 落在 [0.6,0.8) 的格点数占比（0—1）
     */
    @TableField("bin_ratio_4")
    private Double binRatio4;

    /**
     * 分档占比：H 落在 [0.8,1.0]（含上界）的格点数占比（0—1）；五档之和为 1（源数据有四舍五入偏差）
     */
    @TableField("bin_ratio_5")
    private Double binRatio5;
}
