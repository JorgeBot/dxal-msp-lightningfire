package cn.gov.cma.hl.dxal.msp.lightningfire.constant;

import lombok.Getter;

/**
 * 接口枚举参数，label 为中文展示名称，枚举名为接口取值。
 *
 * <p>取值到名称的映射同时用于 springdoc 参数说明（见 LightningEnvironmentController、RiskAssessmentController），
 * 前端下拉框可直接读取 /common/region-code、/ltg-env/key-element/checkboxes、
 * /risk-assessment/exposure/indicator/options、/risk-assessment/vulnerability/indicator/options。</p>
 */
public class Option {

    /**
     * 雷击火关键要素（checkbox）
     */
    @Getter
    public enum KeyElementOption {
        lightningCharacteristics("闪电特征"),
        lightingFire("雷击火"),
        thunderstorm("雷暴"),
        temperature("气温"),
        windSpeed("风速"),
        precipitation("降水"),
        relativeHumidity("相对湿度"),
        wetnessIndex("湿润指数"),
        ;

        private final String label;

        KeyElementOption(String label) {
            this.label = label;
        }
    }

    /**
     * 承载体暴露度评估指标
     *
     * <p>fieldId 为交接包中的指标标识：定位统计汇总表与 {年度}/{fieldId}.tif 栅格文件；
     * 综合暴露度取 E，其栅格文件名含方案哈希，取自 exposure_composite_summary.source_file，
     * 不按 {fieldId}.tif 拼接（fieldId 仅用于 index.json 产品记录匹配）。</p>
     *
     * <p>meanDisplayScale / meanDisplayUnit 为「指标均值对比柱状图」的展示口径：
     * 森林覆盖暴露度、居民地面占比的 mean 为 0—1，显示时乘 100 记 %；道路暴露度密度、
     * 综合暴露度的均值为无量纲指数，直接显示。</p>
     */
    @Getter
    public enum ExposureIndicatorOption {
        forestFraction("森林覆盖暴露度", "forest_fraction", 100, "%"),
        settlementFraction("居民地面占比", "settlement_fraction", 100, "%"),
        roadExposure("道路暴露度密度", "road_exposure", 1, "无量纲"),
        compositeExposure("综合暴露度", "E", 1, "无量纲"),
        ;

        private final String label;

        private final String fieldId;

        private final int meanDisplayScale;

        private final String meanDisplayUnit;

        ExposureIndicatorOption(String label, String fieldId, int meanDisplayScale, String meanDisplayUnit) {
            this.label = label;
            this.fieldId = fieldId;
            this.meanDisplayScale = meanDisplayScale;
            this.meanDisplayUnit = meanDisplayUnit;
        }
    }


    @Getter
    public enum ExposureAHPOption {

        L0(0, "AHP基准矩阵"),
        L1(1, "AHP对照：森林 / 居民地项=1"),
        L2(2, "AHP对照：森林 / 居民地项=3"),
        L3(3, "AHP对照：森林 / 道路项=1"),
        L4(4, "AHP对照：森林 / 道路项=3"),
        L5(5, "AHP对照：居民地 / 道路项=0.5"),
        L6(6, "AHP对照：居民地 / 道路项=2"),
        ;
        private final Integer id;
        private final String label;

        ExposureAHPOption(Integer id, String label) {
            this.id = id;
            this.label = label;
        }
    }

    /**
     * 承载体脆弱性评估指标
     *
     * <p>fieldId 为交接包中的指标标识：定位统计汇总表、{年度}/{fieldId}.tif 栅格文件与
     * index.json 的产品记录（取数据来源年份）；综合脆弱性取 V，其栅格文件名含方案哈希，
     * 取自 vulnerability_composite_summary.source_file，不按 {fieldId}.tif 拼接。
     * 权重方案与暴露度共用同一套 AHP 方案号，但取 V_weights
     * （四项：植被燃烧敏感度、可燃物负荷指数、坡度困难度、道路接近条件）。</p>
     */
    @Getter
    public enum VulnerabilityIndicatorOption {
        vegetationSensitivity("植被燃烧敏感度", "vegetation_sensitivity"),
        fuelLoadIndex("可燃物负荷指数", "fuel_load_index"),
        slopeDifficulty("坡度困难度", "slope_difficulty"),
        roadAccess("道路接近条件", "road_access"),
        compositeVulnerability("综合脆弱性", "V"),
        ;

        private final String label;

        private final String fieldId;

        VulnerabilityIndicatorOption(String label, String fieldId) {
            this.label = label;
            this.fieldId = fieldId;
        }
    }

    /**
     * 承载体脆弱性·综合脆弱性 V 的 AHP 方案
     *
     * <p>方案号与 ExposureAHPOption 相同（0—6），标签取 index.json weight_schemes 的原生表述
     * 「第1/2项」形式：V 的四个输入由同一套 3×3 判断矩阵折算（见 indicators.V.formula），
     * 故不沿用暴露度的森林 / 居民地 / 道路说法。</p>
     */
    @Getter
    public enum VulnerabilityAHPOption {

        L0(0, "AHP基准矩阵"),
        L1(1, "AHP对照：第1/2项=1"),
        L2(2, "AHP对照：第1/2项=3"),
        L3(3, "AHP对照：第1/3项=1"),
        L4(4, "AHP对照：第1/3项=3"),
        L5(5, "AHP对照：第2/3项=0.5"),
        L6(6, "AHP对照：第2/3项=2"),
        ;
        private final Integer id;
        private final String label;

        VulnerabilityAHPOption(Integer id, String label) {
            this.id = id;
            this.label = label;
        }
    }

    /**
     * 时间轴粒度
     */
    @Getter
    public enum GranularityOption {
        DAY("天"),
        MONTH("月"),
        YEAR("年"),
        ;

        private final String label;

        GranularityOption(String label) {
            this.label = label;
        }
    }
}
