package cn.gov.cma.hl.dxal.msp.lightningfire.constant;

import lombok.Getter;

/**
 * 接口枚举参数，label 为中文展示名称，枚举名为接口取值。
 *
 * <p>取值到名称的映射同时用于 springdoc 参数说明（见 LightningEnvironmentController），
 * 前端下拉框可直接读取 /ltg-env/region/options、/ltg-env/key-element/checkboxes。</p>
 */
public class Option {

    /**
     * 雷击火关键要素（checkbox）
     */
    @Getter
    public enum KeyElementOption {
        precipitation("降水"),
        temperature("气温"),
        windSpeed("风速"),
        relativeHumidity("相对湿度"),
        soilMoisture("土壤水"),
        fuelMoisture("可燃物含水率"),
        wetnessIndex("湿润指数"),
        comprehensiveRisk("综合风险"),
        ;

        private final String label;

        KeyElementOption(String label) {
            this.label = label;
        }
    }

    /**
     * 分析区域，all 表示大兴安岭地区（全区域）
     */
    @Getter
    public enum RegionOption {
        all("大兴安岭地区"),
        hzh("呼中区"),
        xlin("新林区"),
        slin("松岭区"),
        jgdc("加格达奇区"),
        mh("漠河市"),
        th("塔河县"),
        hm("呼玛县");

        private final String label;

        RegionOption(String label) {
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
