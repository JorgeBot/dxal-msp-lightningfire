package cn.gov.cma.hl.dxal.msp.lightningfire.service;

import cn.gov.cma.hl.dxal.msp.lightningfire.constant.RegionCode;
import cn.gov.cma.hl.dxal.msp.lightningfire.entity.DangerousnessStatistics;
import cn.gov.cma.hl.dxal.msp.lightningfire.mapper.DangerousnessStatisticsMapper;
import cn.gov.cma.hl.dxal.msp.lightningfire.vo.EnvironmentalThresholdChartVO;
import cn.gov.cma.hl.dxal.msp.lightningfire.vo.EnvironmentalThresholdCountyVO;
import cn.gov.cma.hl.dxal.msp.lightningfire.vo.EnvironmentalThresholdDailyVO;
import cn.gov.cma.hl.dxal.msp.lightningfire.vo.EnvironmentalThresholdTrendPointVO;
import cn.gov.cma.hl.dxal.msp.lightningfire.vo.EnvironmentalThresholdTrendVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

/**
 * 雷击火环境影响阈值：所选区域的危险性分数折线图（逐日）与当日区县分数快照。
 *
 * <p>数据源为危险性统计表 dangerousness_statistics 的 score 列（0—100 百分制，数值越高危险性越高），
 * 不读栅格、不做 AHP 合成。该表粒度是「日期 × 区县 × 地级市」：region 列只放区县名
 * （如漠河市、加格达奇区），city 列放地市名（全省 13 个地市），表内没有省级行，也没有行政区划代码，
 * 因此本模块按 {@link RegionCode} 的地区名匹配，与综合风险评估与区划模块（{@link RiskAssessmentService}）
 * 取的是同一张表、同一套行政区口径，只是把「评价年份」换成了「日期区间」与「单日」。</p>
 *
 * <p>区域分数的汇总口径与综合风险评估与区划一致：先按区县算均值，再按行政区等权取均值。</p>
 * <ul>
 *   <li>县市区：按「所属地市 + 区县名」定位该区县的记录；</li>
 *   <li>地市：取所辖县区，先按区县算当日均值，再取各县区均值的等权均值；</li>
 *   <li>全省：由县区均值汇总出各地市均值，再取各地市均值的等权均值；</li>
 *   <li>表中没有面积列与逐格数据，故一律不做面积加权。</li>
 * </ul>
 *
 * <p>缺数约定：没有记录的日期 / 没有记录的县区都照常返回、分数为 null，不补 0，
 * 避免把缺测读成低风险；只有「当日一条记录都没有」时才连「有效区县个数」一起归零。
 * 表内 city 列不在区划枚举内的记录无法定位行政区，直接跳过（枚举覆盖全省 13 个地市，
 * 正常数据不会出现）。</p>
 */
@Service
@RequiredArgsConstructor
public class EnvironmentalImpactThresholdService {

    private static final String LEVEL_PROVINCE = "province";

    private static final String LEVEL_CITY = "city";

    private static final String LEVEL_COUNTY = "county";

    /**
     * 分数单位：危险性评分是 0—100 百分制，与综合风险评估与区划的「分」同一口径。
     */
    private static final String UNIT_SCORE = "分";

    /**
     * 当日分数前五柱状图展示的区县数量。
     */
    private static final int TOP_SIZE = 5;

    private static final String TOP_CHART_TITLE = "当日区县分数对比（前 5）";

    /**
     * 分数升序（无记录的排最后）：用于取当日最小值，分数相同时取名称靠前者，结果稳定。
     */
    private static final Comparator<EnvironmentalThresholdCountyVO> BY_SCORE_ASC =
            Comparator.comparing(EnvironmentalThresholdCountyVO::score,
                            Comparator.nullsLast(Comparator.naturalOrder()))
                    .thenComparing(EnvironmentalThresholdCountyVO::name);

    /**
     * 分数降序（无记录的排最后）：用于「每个县的分数」与前五柱状图的展示顺序，
     * 分数相同时同样取名称升序，保证同一份数据每次返回的顺序一致。
     */
    private static final Comparator<EnvironmentalThresholdCountyVO> BY_SCORE_DESC =
            Comparator.comparing(EnvironmentalThresholdCountyVO::score,
                            Comparator.nullsLast(Comparator.reverseOrder()))
                    .thenComparing(EnvironmentalThresholdCountyVO::name);

    private final DangerousnessStatisticsMapper dangerousnessStatisticsMapper;

    /**
     * 分数折线图：所选区域在 since ~ until 内逐日的危险性分数。
     *
     * <p>x 轴恒按 since ~ until 逐日展开（含无记录的日期），保证前端折线连续：
     * 无记录的日期同样占位、分数为 null，不补 0，也不跨日替代。逐日分数按行政区层级汇总，
     * 口径见类注释。区间内一条记录都没有时，返回一段全 null 的日期轴（接口仍返回 200），
     * 不用 0 或空数组冒充无数据。</p>
     *
     * @param since  统计起始日期（含）
     * @param until  统计结束日期（含），需不早于 since（由接口层校验）
     * @param region 所选行政区（全省 / 地市 / 县市区）
     * @return 折线图数据，points 长度即 since ~ until 的日历天数
     */
    public EnvironmentalThresholdTrendVO scoreTrend(LocalDate since, LocalDate until, RegionCode region) {
        NavigableMap<LocalDate, Double> dailyScore = selectedDailyScore(since, until, region);
        List<EnvironmentalThresholdTrendPointVO> points = new ArrayList<>();
        for (LocalDate cursor = since; !cursor.isAfter(until); cursor = cursor.plusDays(1)) {
            points.add(new EnvironmentalThresholdTrendPointVO(cursor, dailyScore.get(cursor)));
        }
        return new EnvironmentalThresholdTrendVO(region.getCode(), region.getLabel(), levelOf(region),
                since, until, UNIT_SCORE, points.toArray(EnvironmentalThresholdTrendPointVO[]::new));
    }

    /**
     * 当日区县分数快照：有效区县个数、区县分数的最小 / 最大值、分数前五柱状图与每个县的分数。
     *
     * <p>统计范围是 regionCode 范围内的全部县区（选择地市取所辖县区、选择县市区取该县区本身、
     * 选择 230000 取全省全部县区）；当日没有记录的县区照常返回、分数为 null，
     * 因此「有效区县个数」是其中分数非 null 的县区个数，而不是数组长度。</p>
     *
     * <p>该日一条记录都没有时返回一份「空快照」：有效区县个数 0、最小 / 最大分数与对应县区名为 null、
     * 柱状图 items 为空数组、各县分数全为 null，接口返回 200——「当天没数据」本身就是该页面要展示的状态。</p>
     *
     * @param date   统计日期（必填，数据源只覆盖 2018-01-01—2024-12-31）
     * @param region 所选行政区（全省 / 地市 / 县市区）
     * @return 当日快照
     */
    public EnvironmentalThresholdDailyVO dailyScore(LocalDate date, RegionCode region) {
        // 当日各「地市 + 区县」的分数均值；键是区县，值域里只有数据里真实出现的县区
        Map<String, CountyScore> scored = new LinkedHashMap<>();
        accumulate(regionQuery(region).eq(DangerousnessStatistics::getDate, date))
                .forEach((ignored, byCity) -> byCity.forEach((cityCode, counties) ->
                        counties.forEach((regionName, sum) -> scored.put(countyKey(cityCode, regionName),
                                new CountyScore(regionName, sum.mean())))));
        // 范围内的全部县区都占位：数据里没有的县区分数为 null，而不是从结果里消失
        List<EnvironmentalThresholdCountyVO> counties = new ArrayList<>();
        for (RegionCode county : RegionCode.selectable(region.getCode())) {
            CountyScore scoredCounty = scored.remove(countyKey(county.getCode() / 100 * 100, county.getLabel()));
            counties.add(new EnvironmentalThresholdCountyVO(county.getCode(), county.getLabel(),
                    scoredCounty == null ? null : scoredCounty.score()));
        }
        // 数据里出现、区划枚举未覆盖的县区名（同名跨地市时按键区分过，不会与枚举内的县区混淆）
        scored.values().forEach(county ->
                counties.add(new EnvironmentalThresholdCountyVO(null, county.name(), county.score())));
        counties.sort(BY_SCORE_DESC);

        List<EnvironmentalThresholdCountyVO> valid = counties.stream()
                .filter(county -> county.score() != null)
                .toList();
        EnvironmentalThresholdCountyVO min = valid.stream().min(BY_SCORE_ASC).orElse(null);
        EnvironmentalThresholdCountyVO max = valid.stream().min(BY_SCORE_DESC).orElse(null);
        EnvironmentalThresholdCountyVO[] top = valid.stream()
                .limit(TOP_SIZE)
                .toArray(EnvironmentalThresholdCountyVO[]::new);
        return new EnvironmentalThresholdDailyVO(date, region.getCode(), region.getLabel(), levelOf(region),
                counties.size(), valid.size(),
                min == null ? null : min.score(), min == null ? null : min.name(),
                max == null ? null : max.score(), max == null ? null : max.name(),
                new EnvironmentalThresholdChartVO(TOP_CHART_TITLE, UNIT_SCORE, top),
                counties.toArray(EnvironmentalThresholdCountyVO[]::new));
    }

    /**
     * 所选区域逐日的分数：只含有记录的日期，没有记录的日期不出现在结果里（由调用方决定是否占位）。
     */
    private NavigableMap<LocalDate, Double> selectedDailyScore(LocalDate since, LocalDate until, RegionCode region) {
        LambdaQueryWrapper<DangerousnessStatistics> query = regionQuery(region)
                .ge(DangerousnessStatistics::getDate, since)
                .le(DangerousnessStatistics::getDate, until)
                .orderByAsc(DangerousnessStatistics::getDate)
                .orderByAsc(DangerousnessStatistics::getCityName)
                .orderByAsc(DangerousnessStatistics::getRegionName);
        NavigableMap<LocalDate, Double> dailyScore = new TreeMap<>();
        accumulate(query).forEach((date, byCity) -> {
            Double score = dayScore(byCity, region);
            if (score != null) {
                dailyScore.put(date, score);
            }
        });
        return dailyScore;
    }

    /**
     * 某日所选区域的分数：全省取各地市（各地市先按区县取均值）的等权均值，
     * 地市取所辖县区均值的等权均值，县市区取其自身均值；当日没有记录时返回 null。
     */
    private Double dayScore(Map<Integer, Map<String, ScoreSum>> byCity, RegionCode region) {
        if (region == RegionCode.R230000) {
            return meanOf(byCity.values().stream().map(this::countyMean).toList());
        }
        Integer cityCode = cityCodeOf(region);
        Map<String, ScoreSum> counties = cityCode == null ? null : byCity.get(cityCode);
        if (counties == null) {
            return null;
        }
        if (region.isCounty()) {
            ScoreSum county = counties.get(region.getLabel());
            return county == null ? null : county.mean();
        }
        return countyMean(counties);
    }

    /**
     * 地市当日分数：所辖各区县（先按区县算均值）的等权均值。
     */
    private Double countyMean(Map<String, ScoreSum> counties) {
        return meanOf(counties.values().stream().map(ScoreSum::mean).toList());
    }

    /**
     * 一组数值的等权均值，忽略 null；全部为 null（没有记录）时返回 null。
     */
    private Double meanOf(List<Double> values) {
        double sum = 0D;
        int count = 0;
        for (Double value : values) {
            if (value != null) {
                sum += value;
                count++;
            }
        }
        return count == 0 ? null : sum / count;
    }

    /**
     * 本模块的取数条件：只取日期、区县名、地市名与分数四列，并按所选行政区限定范围。
     *
     * <p>县市区按「所属地市 + 区县名」定位（区划枚举里同名区县分属不同地市，只按名字会串档）；
     * 地市按 city 列定位；全省不加地区条件（省级行不存在，全部地市行由各地县区行汇总）。
     * 三列地区名与分数都为 NULL 的行没有意义，直接排除。</p>
     */
    private LambdaQueryWrapper<DangerousnessStatistics> regionQuery(RegionCode region) {
        LambdaQueryWrapper<DangerousnessStatistics> query = Wrappers.<DangerousnessStatistics>lambdaQuery()
                .select(DangerousnessStatistics::getDate, DangerousnessStatistics::getRegionName,
                        DangerousnessStatistics::getCityName, DangerousnessStatistics::getScore)
                .isNotNull(DangerousnessStatistics::getRegionName)
                .isNotNull(DangerousnessStatistics::getCityName)
                .isNotNull(DangerousnessStatistics::getScore);
        if (region != RegionCode.R230000) {
            String cityName = cityNameOf(region);
            if (cityName != null) {
                query.eq(DangerousnessStatistics::getCityName, cityName);
            }
        }
        if (region.isCounty()) {
            query.eq(DangerousnessStatistics::getRegionName, region.getLabel());
        }
        return query;
    }

    /**
     * 按「日期 → 地市 → 区县」三级累加分数：同一天同一区县若有多行则合并为一条均值，
     * 上层再按「先区县、后地市」的次序取均值，与综合风险评估与区划的汇总口径一致。
     *
     * <p>聚合在内存中完成（与 {@link RiskAssessmentService} 相同），city 列不在区划枚举内的记录
     * 无法定位行政区，跳过。</p>
     */
    private Map<LocalDate, Map<Integer, Map<String, ScoreSum>>> accumulate(
            LambdaQueryWrapper<DangerousnessStatistics> query) {
        Map<String, Integer> cityCodes = cityCodes();
        Map<LocalDate, Map<Integer, Map<String, ScoreSum>>> accumulated = new TreeMap<>();
        for (DangerousnessStatistics row : dangerousnessStatisticsMapper.selectList(query)) {
            BigDecimal score = row.getScore();
            String regionName = row.getRegionName();
            LocalDate date = row.getDate();
            Integer cityCode = row.getCityName() == null ? null : cityCodes.get(row.getCityName());
            if (score == null || regionName == null || date == null || cityCode == null) {
                continue;
            }
            accumulated.computeIfAbsent(date, ignored -> new LinkedHashMap<>())
                    .computeIfAbsent(cityCode, ignored -> new LinkedHashMap<>())
                    .merge(regionName, new ScoreSum(score.doubleValue(), 1L), ScoreSum::plus);
        }
        return accumulated;
    }

    /**
     * 地市名到 6 位地市码的对照表；不含省级码（表内没有省级行）。
     */
    private Map<String, Integer> cityCodes() {
        Map<String, Integer> codes = new LinkedHashMap<>();
        for (RegionCode region : RegionCode.values()) {
            if (region != RegionCode.R230000 && !region.isCounty()) {
                codes.put(region.getLabel(), region.getCode());
            }
        }
        return codes;
    }

    /**
     * 行政区所属地市的 6 位码：县市区取其区划码前 4 位对应的地市，地市与全省取自身；
     * 区划枚举内没有对应地市时返回 null。
     */
    private Integer cityCodeOf(RegionCode region) {
        if (!region.isCounty()) {
            return region == RegionCode.R230000 ? null : region.getCode();
        }
        RegionCode city = RegionCode.fromCode(region.getCode() / 100 * 100);
        return city == null ? null : city.getCode();
    }

    /**
     * 行政区所属地市名：县市区取区划码前 4 位对应的地市名，地市取自身名；省级没有地市名。
     */
    private String cityNameOf(RegionCode region) {
        if (!region.isCounty()) {
            return region == RegionCode.R230000 ? null : region.getLabel();
        }
        RegionCode city = RegionCode.fromCode(region.getCode() / 100 * 100);
        return city == null ? null : city.getLabel();
    }

    /**
     * 行政区级别：全省、地市、县市区。
     */
    private String levelOf(RegionCode region) {
        if (region == RegionCode.R230000) {
            return LEVEL_PROVINCE;
        }
        return region.isCounty() ? LEVEL_COUNTY : LEVEL_CITY;
    }

    /**
     * 区县行的索引键：地市码 + 区县名，避免同名区县跨地市串档（如鹤岗市东山区与佳木斯市东风区）。
     */
    private String countyKey(int cityCode, String regionName) {
        return "R" + cityCode + " " + regionName;
    }

    /**
     * 同一天同一区县（同一地市）的分数累加值。
     */
    private record ScoreSum(double sum, long count) {

        /**
         * 合并同一区县的多行记录。
         */
        ScoreSum plus(ScoreSum other) {
            return new ScoreSum(sum + other.sum, count + other.count);
        }

        /**
         * 该区县当日分数均值。
         */
        double mean() {
            return sum / count;
        }
    }

    /**
     * 当日某个区县的分数。
     */
    private record CountyScore(String name, double score) {
    }
}
