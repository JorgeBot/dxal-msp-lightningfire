package cn.gov.cma.hl.dxal.msp.lightningfire.constant;

import lombok.Getter;

public class SelectOption {

    @Getter
    public enum KeyElementOption {
        lightningFeature("地闪特征"),
        precipitation("降水"),
        temperature("气温"),
        windSpeed("风速"),
        relativeHumidity("相对湿度"),
        soilMoisture("土壤水"),
        fuelMoisture("可燃物含水率"),
        wetnessIndex("湿润指数"),
        lightningFire("雷击火"),
        comprehensiveRisk("综合风险"),
        ;

        private final String label;

        KeyElementOption(String label) {
            this.label = label;
        }
    }

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
