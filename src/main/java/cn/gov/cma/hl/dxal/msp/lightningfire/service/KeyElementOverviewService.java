package cn.gov.cma.hl.dxal.msp.lightningfire.service;

import cn.gov.cma.hl.dxal.msp.lightningfire.constant.Option;
import cn.gov.cma.hl.dxal.msp.lightningfire.constant.RegionArea;
import cn.gov.cma.hl.dxal.msp.lightningfire.constant.RegionCode;
import cn.gov.cma.hl.dxal.msp.lightningfire.constant.ThunderstormStation;
import cn.gov.cma.hl.dxal.msp.lightningfire.dto.lightningfeature.KeyElementStatisticDTO;
import cn.gov.cma.hl.dxal.msp.lightningfire.dto.lightningfeature.LightningCharacteristicsDTO;
import cn.gov.cma.hl.dxal.msp.lightningfire.dto.lightningfeature.LightningFireSummaryDTO;
import cn.gov.cma.hl.dxal.msp.lightningfire.entity.ThunderstormStatistics;
import cn.gov.cma.hl.dxal.msp.lightningfire.entity.WeatherObservation;
import cn.gov.cma.hl.dxal.msp.lightningfire.mapper.ThunderstormStatisticsMapper;
import cn.gov.cma.hl.dxal.msp.lightningfire.mapper.WeatherObservationMapper;
import cn.gov.cma.hl.dxal.msp.lightningfire.vo.ArticleVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 雷击火关键要素·统计概览：按所选要素返回它的 4 个统计格子。
 *
 * <p>闪电特征（4 格：地闪次数、地闪密度、地闪强度、正负极性）与下线前的「地闪特征」接口同源，
 * 直接取 {@link LightningFeatureService#lightningCharacteristics} 的聚合结果；气温、风速、降水、
 * 相对湿度各 4 格（平均、最大、最小、极差），湿润指数的第 4 格为距平。</p>
 *
 * <p>雷击火 4 格（事件次数、过火面积合计、平均过火面积、点位密度）取自 forest_fire_statistics，
 * 过火面积单位 hm²、与源档案一致；点位密度 = 事件次数 ÷ 选区内县市区面积之和，单位 起/万km²，
 * 分母来自 {@link RegionArea}，选区内只要有县市区没有面积口径就不计算（记 0）。</p>
 *
 * <p>雷暴返回 4 格，但只有两项统计：年平均雷暴天数与雷暴密度，后两格为 null。数据源 thunderstorm_statistics
 * 按站存年雷暴日数，站号与区划的对照见 {@link ThunderstormStation}；该表只有 1961—2013 年，
 * 这段年限即雷暴统计的时间口径、与页面的 since ~ until 无关，故取选区内全部年份的平均；
 * 雷暴密度 = 年平均雷暴天数 ÷ 选区内县市区面积之和（天/万km²）。选区内有区划没有雷暴站或没有面积口径时两格记 0。</p>
 *
 * <p>要素口径：气温、风速、降水、相对湿度、湿润指数的数据源是 weather_observations（区县 × 年，
 * 见 {@link WeatherObservation}）。页面选择的是日期区间，而该表只有年度值，故按自然年归一到 since ~ until
 * 覆盖的年份，在 regionCode 范围内的全部区县年记录上等权聚合——不按区县面积、也不按站点数加权
 * （表里没有这两列，且各县站点数差异已由入库口径处理）。闪电特征与雷击火按记录日期精确过滤，
 * 不受年度口径影响。区间内没有任何记录时各格记 0，与地闪特征、相关系数等接口的缺数约定一致，
 * 不在同一页面出现两种空值口径。</p>
 *
 * <p>湿润指数距平的基准取同一区划范围全部年份（2018—2025）的平均湿润指数，即
 * 距平 = 区间平均 − 多年平均，正值表示比多年平均更湿润；这是本表能给出的最长基准，
 * 不使用 1991—2020 常年值（数据不在本库内）。</p>
 *
 * <p>regionCode 为 6 位行政区划码，选区内的县区由 {@link RegionCode#selectable(Integer)} 给出：
 * 全省（230000）为省内全部县区、地市（如 232700）为所辖县区、县区（如 232701）为该县区本身。</p>
 */
@Service
@RequiredArgsConstructor
public class KeyElementOverviewService {

    private static final String UNIT_LIGHTNING_COUNT = "次";

    private static final String UNIT_LIGHTNING_DENSITY = "次/km²";

    private static final String UNIT_LIGHTNING_INTENSITY = "强度值";

    private static final String UNIT_LIGHTNING_POLARITY = "%正闪";

    private static final String UNIT_FIRE_COUNT = "起";

    private static final String UNIT_HECTARE = "hm²";

    private static final String UNIT_FIRE_DENSITY = "起/万km²";

    private static final String UNIT_THUNDERSTORM_DAYS = "天";

    private static final String UNIT_THUNDERSTORM_DENSITY = "天/万km²";

    private static final String UNIT_DIMENSIONLESS = "无量纲";

    private final LightningFeatureService lightningFeatureService;

    private final WeatherObservationMapper weatherObservationMapper;

    private final ThunderstormStatisticsMapper thunderstormStatisticsMapper;

    /**
     * 所选要素的统计概览格子，顺序固定：平均、最大、最小、第四格（闪电特征、雷击火、雷暴为各自的口径）。
     *
     * @param since      统计起始日期（含），只取到自然年；雷暴按 1961—2013 年口径，不使用该参数
     * @param until      统计结束日期（含），只取到自然年；雷暴按 1961—2013 年口径，不使用该参数
     * @param regionCode 分析区域，非 null 且必须是 {@link RegionCode} 内的区划码
     * @param element    所选要素，取值见 Option.KeyElementOption
     * @return 该要素的 4 个格子；区间或范围内没有记录时记 0；雷暴的第 3、4 格恒为 null
     */
    public ArticleVO[] overview(LocalDate since, LocalDate until, RegionCode regionCode,
                                Option.KeyElementOption element) {
        return switch (element) {
            case lightningCharacteristics -> lightningCells(since, until, regionCode).toArray(ArticleVO[]::new);
            case lightingFire -> lightningFireCells(since, until, regionCode).toArray(ArticleVO[]::new);
            // 雷暴表只有 1961—2013 年，与页面日期区间无关，故不传 since / until
            case thunderstorm -> thunderstormCells(regionCode).toArray(ArticleVO[]::new);
            // 气象要素：要素枚举名与 ElementCard 枚举名一致，可直接按名取卡片
            case temperature, windSpeed, precipitation, relativeHumidity, wetnessIndex ->
                    elementCells(since, until, regionCode, ElementCard.valueOf(element.name()))
                            .toArray(ArticleVO[]::new);
        };
    }

    /**
     * 闪电特征 4 格：地闪次数、地闪密度、地闪强度、正负极性。
     *
     * <p>数值原样转字符串：聚合已在 SQL 内四舍五入到 3 位小数
     * （见 LightningRegionStatisticsMapper.selectLightningSummaryByCondition），展示口径不在这里再改一遍。</p>
     */
    private List<ArticleVO> lightningCells(LocalDate since, LocalDate until, RegionCode regionCode) {
        LightningCharacteristicsDTO characteristics =
                lightningFeatureService.lightningCharacteristics(since, until, regionCode);
        return List.of(
                new ArticleVO("地闪次数", String.valueOf(characteristics.recordCount()), UNIT_LIGHTNING_COUNT),
                new ArticleVO("地闪密度", String.valueOf(characteristics.avgDensity()), UNIT_LIGHTNING_DENSITY),
                new ArticleVO("地闪强度", String.valueOf(characteristics.avgAbsIntensity()), UNIT_LIGHTNING_INTENSITY),
                new ArticleVO("正负极性", String.valueOf(characteristics.avgPositiveRatioPer()), UNIT_LIGHTNING_POLARITY));
    }

    /**
     * 雷击火 4 格：事件次数、过火面积合计、平均过火面积、点位密度。
     *
     * <p>前三格来自 ForestFireStatisticsMapper.selectLightningFireSummary（区间 + 区域过滤，
     * 过火面积单位 hm²，与源档案一致）；点位密度 = 事件次数 ÷ 区域内面积，单位 起/万km²，
     * 分母取选区内县市区面积之和（{@link RegionArea}）。选区内只要有一个县市区没有面积口径
     * （如全省 230000），密度就不计算、记 0，不拿部分县区面积当分母。</p>
     */
    private List<ArticleVO> lightningFireCells(LocalDate since, LocalDate until, RegionCode regionCode) {
        LightningFireSummaryDTO summary = lightningFeatureService.lightningFireSummary(since, until, regionCode);
        Integer areaKm2 = selectedAreaKm2(regionCode);
        Double density = areaKm2 == null || areaKm2 == 0
                ? null
                : summary.fireCount() * 10000D / areaKm2;
        return List.of(
                new ArticleVO("雷击火事件次数", String.valueOf(summary.fireCount()), UNIT_FIRE_COUNT),
                new ArticleVO("过火面积合计", format(summary.totalArea(), 2), UNIT_HECTARE),
                new ArticleVO("平均过火面积", format(summary.avgArea(), 2), UNIT_HECTARE),
                new ArticleVO("点位密度", format(density, 2), UNIT_FIRE_DENSITY));
    }

    /**
     * 选区内县市区的面积合计（km²）。
     *
     * <p>选区由 {@link RegionCode#selectable(Integer)} 给出，逐个取面积；任一县市区没有面积口径则返回 null
     * （宁可不给密度，也不用「部分县区面积 + 全部事件数」算出一个名不副实的密度）。</p>
     */
    private Integer selectedAreaKm2(RegionCode regionCode) {
        int total = 0;
        for (RegionCode region : RegionCode.selectable(regionCode.getCode())) {
            Integer areaKm2 = RegionArea.areaKm2(region);
            if (areaKm2 == null) {
                return null;
            }
            total += areaKm2;
        }
        return total;
    }

    /**
     * 雷暴 4 格：年平均雷暴天数、雷暴密度，后两格留空。
     *
     * <p>数据源 thunderstorm_statistics 按站存年雷暴日数，站号与区划的对照见 {@link ThunderstormStation}；
     * 表里只有 1961—2013 年，这段年限即雷暴统计的时间口径，与页面的 since ~ until 无关，
     * 故取选区内全部年份的平均年雷暴天数。雷暴密度 = 年平均雷暴天数 ÷ 选区内县市区面积之和，
     * 单位 天/万km²，分母与点位密度同用 {@link RegionArea}。选区内只要有县市区没有雷暴站或没有面积口径，
     * 两格就记 0，不拿部分站点、部分面积算全选区的值。该要素只有两项统计，后两格按页面 4 格布局返回 null。</p>
     */
    private List<ArticleVO> thunderstormCells(RegionCode regionCode) {
        List<String> stationCodes = selectedStationCodes(regionCode);
        Double avgDays = stationCodes == null
                ? null
                : thunderstormStatisticsMapper.selectAverageThunderstormDays(
                        Wrappers.<ThunderstormStatistics>lambdaQuery()
                                .in(ThunderstormStatistics::getStationCode, stationCodes));
        Integer areaKm2 = selectedAreaKm2(regionCode);
        Double density = avgDays == null || areaKm2 == null || areaKm2 == 0
                ? null
                : avgDays * 10000D / areaKm2;
        return List.of(
                new ArticleVO("年平均雷暴天数", format(avgDays, 1), UNIT_THUNDERSTORM_DAYS),
                new ArticleVO("雷暴密度", format(density, 2), UNIT_THUNDERSTORM_DENSITY),
                new ArticleVO(null, null, null),
                new ArticleVO(null, null, null));
    }

    /**
     * 选区内雷暴站的站号。
     *
     * <p>与面积一样，雷暴站只覆盖大兴安岭地区所辖 7 个县市区；选区内只要有一个区划没有对应站点就返回 null
     * （选全省时不能把大兴安岭 7 站的平均说成全省的雷暴天数）。</p>
     */
    private List<String> selectedStationCodes(RegionCode regionCode) {
        List<String> stationCodes = new ArrayList<>();
        for (RegionCode region : RegionCode.selectable(regionCode.getCode())) {
            String stationCode = ThunderstormStation.stationCode(region);
            if (stationCode == null) {
                return null;
            }
            stationCodes.add(stationCode);
        }
        return stationCodes;
    }

    /**
     * 所选气象要素的 4 格：区间内等权聚合的平均、最大、最小与第四格。
     *
     * <p>一次查询取回全部要素的聚合（见 WeatherObservationMapper.selectKeyElementStatistics），
     * 这里只取所选要素那一行；多年平均湿润指数只在要算距平时才查，不为其他要素多做一次聚合。
     * 结果缺行或聚合值为 NULL 时按无有效值处理（展示为 0）。</p>
     */
    private List<ArticleVO> elementCells(LocalDate since, LocalDate until, RegionCode regionCode, ElementCard card) {
        KeyElementStatisticDTO statistic = weatherObservationMapper
                .selectKeyElementStatistics(rangeQuery(since, until, regionCode)).stream()
                .filter(row -> row.element().equals(card.name()))
                .findFirst()
                .orElse(null);
        Double avg = statistic == null ? null : statistic.avgValue();
        Double max = statistic == null ? null : statistic.maxValue();
        Double min = statistic == null ? null : statistic.minValue();
        Double moistureIndexBaseline = card == ElementCard.wetnessIndex
                ? weatherObservationMapper.selectAverageMoistureIndex(regionQuery(regionCode))
                : null;

        return List.of(
                cell(card.getAvgLabel(), avg, card),
                cell(card.getMaxLabel(), max, card),
                cell(card.getMinLabel(), min, card),
                cell(card.getLastLabel(), lastValue(card, avg, max, min, moistureIndexBaseline), card));
    }

    /**
     * 第四格：气温等要素为最大 − 最小（极差），湿润指数为距平（区间平均 − 多年平均）。
     *
     * <p>参与相减的任一项为空（区间内该要素没有有效值，或范围内没有多年平均）时返回 null，
     * 由展示层按「无数据记 0」处理。</p>
     */
    private Double lastValue(ElementCard card, Double avg, Double max, Double min, Double moistureIndexBaseline) {
        if (card == ElementCard.wetnessIndex) {
            return avg == null || moistureIndexBaseline == null ? null : avg - moistureIndexBaseline;
        }
        return max == null || min == null ? null : max - min;
    }

    private ArticleVO cell(String label, Double value, ElementCard card) {
        return new ArticleVO(label, format(value, card.getScale()), card.getUnit());
    }

    /**
     * 展示值：按要素精度四舍五入（HALF_UP，不用 DecimalFormat 以免受默认区域设置影响），
     * 无有效值时记 0。
     */
    private String format(Double value, int scale) {
        return BigDecimal.valueOf(value == null ? 0D : value).setScale(scale, RoundingMode.HALF_UP).toPlainString();
    }

    /**
     * 要素统计条件：区划范围内、assessment_year 落在 since ~ until 覆盖的自然年。
     *
     * <p>数据源是年度值，日期区间按自然年取整：2024-06-01 ~ 2024-08-31 与 2024-01-01 ~ 2024-12-31 等价。</p>
     */
    private LambdaQueryWrapper<WeatherObservation> rangeQuery(LocalDate since, LocalDate until, RegionCode regionCode) {
        LambdaQueryWrapper<WeatherObservation> q = regionQuery(regionCode);
        if (since != null) {
            q.ge(WeatherObservation::getAssessmentYear, (short) since.getYear());
        }
        if (until != null) {
            q.le(WeatherObservation::getAssessmentYear, (short) until.getYear());
        }
        return q;
    }

    /**
     * 区划范围条件：region_code 落在选区内县区码集合。
     *
     * <p>用 {@link RegionCode#selectable(Integer)} 的县区集合表达「选区内有哪些区县」，而不是自己按码段判断：
     * 全省是省内全部县区、地市是所辖县区、县区是它本身，与区划下拉的同一套规则。</p>
     */
    private LambdaQueryWrapper<WeatherObservation> regionQuery(RegionCode regionCode) {
        LambdaQueryWrapper<WeatherObservation> q = Wrappers.lambdaQuery();
        q.in(WeatherObservation::getRegionCode, RegionCode.selectable(regionCode.getCode()).stream()
                .map(RegionCode::getCode)
                .toList());
        return q;
    }

    /**
     * 概览网格中的气象要素卡片：标签、单位与展示精度都收在这里，一个要素固定 4 格。
     *
     * <p>枚举名与 Mapper 返回的 element 标识一致（即 Option.KeyElementOption 的枚举名），
     * 于是「所选要素 → 卡片」不需要额外的映射表。第四格：气温、风速、降水、相对湿度为极差，
     * 湿润指数为距平。</p>
     */
    @Getter
    private enum ElementCard {

        temperature("平均气温", "最高气温", "最低气温", "气温极差", "℃", 1),
        windSpeed("平均风速", "最大风速", "最小风速", "风速极差", "m/s", 2),
        precipitation("平均降水", "最大降水", "最小降水", "降水极差", "mm", 1),
        relativeHumidity("平均相对湿度", "最高相对湿度", "最低相对湿度", "相对湿度极差", "%", 1),
        wetnessIndex("平均湿润指数", "最大湿润指数", "最小湿润指数", "湿润指数距平", UNIT_DIMENSIONLESS, 3),
        ;

        private final String avgLabel;

        private final String maxLabel;

        private final String minLabel;

        private final String lastLabel;

        private final String unit;

        /**
         * 展示小数位：气温 1 位、风速 2 位、降水 1 位、相对湿度 1 位、湿润指数 3 位。
         */
        private final int scale;

        ElementCard(String avgLabel, String maxLabel, String minLabel, String lastLabel, String unit, int scale) {
            this.avgLabel = avgLabel;
            this.maxLabel = maxLabel;
            this.minLabel = minLabel;
            this.lastLabel = lastLabel;
            this.unit = unit;
            this.scale = scale;
        }
    }
}
