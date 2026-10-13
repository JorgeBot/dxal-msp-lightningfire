package cn.gov.cma.hl.dxal.msp.lightningfire.service;

import cn.gov.cma.hl.dxal.msp.lightningfire.constant.FixedThreshold;
import cn.gov.cma.hl.dxal.msp.lightningfire.constant.Option;
import cn.gov.cma.hl.dxal.msp.lightningfire.constant.RegionCode;
import cn.gov.cma.hl.dxal.msp.lightningfire.entity.ConditionDistributionDaily;
import cn.gov.cma.hl.dxal.msp.lightningfire.mapper.ConditionDistributionDailyMapper;
import cn.gov.cma.hl.dxal.msp.lightningfire.vo.EnvironmentalThresholdConditionVO;
import cn.gov.cma.hl.dxal.msp.lightningfire.vo.EnvironmentalThresholdCountyDistributionVO;
import cn.gov.cma.hl.dxal.msp.lightningfire.vo.EnvironmentalThresholdCountyPercentVO;
import cn.gov.cma.hl.dxal.msp.lightningfire.vo.EnvironmentalThresholdMetricsVO;
import cn.gov.cma.hl.dxal.msp.lightningfire.vo.EnvironmentalThresholdPathVO;
import cn.gov.cma.hl.dxal.msp.lightningfire.vo.EnvironmentalThresholdPercentPointVO;
import cn.gov.cma.hl.dxal.msp.lightningfire.vo.EnvironmentalThresholdPercentTrendVO;
import cn.gov.cma.hl.dxal.msp.lightningfire.vo.EnvironmentalThresholdRuleVO;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

/**
 * 功能二「固定阈值」：固定条件界线（阈值汇总）、逐日条件百分比折线、所选区域各区县条件百分比。
 *
 * <p>数据分两部分，互不混用：</p>
 * <ul>
 *   <li><b>固定尺子</b>：九个单因子的方向、阈值、单位、95% 置信区间与 2024 年检验结果，
 *       以及组合环境条件的路径集合，提取为常量 {@link FixedThreshold}
 *       （来源 功能二_固定阈值/结果/阈值汇总.csv 与 固定阈值参数与检验.json），接口直接查表，不重新训练；</li>
 *   <li><b>量出来的结果</b>：县级逐日条件分布表 condition_distribution_daily，
 *       每行一个县一天的 {@code {因子}_hit_pct}（已经是 0—100 的百分比），
 *       本类只做读取与汇总，不做栅格判定。</li>
 * </ul>
 *
 * <p>汇总口径（多县、多日）：</p>
 * <ul>
 *   <li>区域当天百分比 = Σ(县比例 × 该县可判断格网数) ÷ Σ可判断格网数，
 *       等价于开发字段说明的「符合数之和 ÷ 可判断数之和」，不是各县百分比简单平均；
 *       表内没有命中数，只用百分比与可判断格网数就能还原这个口径；</li>
 *   <li>月 / 年粒度 = 该周期内<b>有效日</b>区域比例的均值（先按天算区域比例、再对有效日取均值），
 *       与开发字段说明「月年为有效日比例平均」一致；</li>
 *   <li>有效日 = 该因子比例非 NULL 且可判断格网数大于 0 的日期；比例列为 NULL 表示无数据
 *       （不在 5—9 月研究应用范围、没有含林格网或没有结果），不补 0、不跨日替代，
 *       折线对应时间点 percent 为 null（缺测断线）。</li>
 * </ul>
 *
 * <p>区县范围由 regionCode 决定，统一走
 * {@link RegionCode#selectable(Integer)}：选县市区返回该县本身、选地市返回所辖县区、
 * 选 230000 返回全省 125 个县级单元（表中没有地市与省级汇总行）。</p>
 */
@Service
@RequiredArgsConstructor
public class FixedThresholdService {

    private static final String LEVEL_PROVINCE = "province";

    private static final String LEVEL_CITY = "city";

    private static final String LEVEL_COUNTY = "county";

    /**
     * 条件分布表的比例列本身就是 0—100 的百分比，单位固定为「%」，接口不再做换算。
     */
    private static final String UNIT_PERCENT = "%";

    /**
     * 时间标签格式，与 /ltg-env 的时间轴口径一致。
     */
    private static final DateTimeFormatter DAY_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private static final DateTimeFormatter MONTH_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM");

    private static final DateTimeFormatter YEAR_FORMAT = DateTimeFormatter.ofPattern("yyyy");

    /**
     * 百分比降序（无数据的排最后，同值按区划码升序）：地图配色与县级横条按高低展示。
     */
    private static final Comparator<EnvironmentalThresholdCountyPercentVO> BY_PERCENT_DESC =
            Comparator.comparing(EnvironmentalThresholdCountyPercentVO::percent,
                            Comparator.nullsLast(Comparator.reverseOrder()))
                    .thenComparing(EnvironmentalThresholdCountyPercentVO::code);

    private final ConditionDistributionDailyMapper conditionDistributionDailyMapper;

    /**
     * 固定阈值规则：所选环境因子的方向、阈值、单位、置信区间与 2024 年检验结果。
     *
     * <p>九个单因子返回一条线（阈值 + 方向）；{@code combined} 没有单列阈值，
     * 返回组合环境条件的三条路径（路径内「且」、路径间「或」）与整体检验结果。</p>
     *
     * @param factor 环境因子（下拉取值，见 {@code /environmental-impact-threshold/factor/options}）
     * @return 该因子的固定条件与检验结果
     */
    public EnvironmentalThresholdRuleVO rule(Option.EnvironmentalFactorOption factor) {
        if (factor == Option.EnvironmentalFactorOption.combined) {
            return combinedRule(factor);
        }
        FixedThreshold.Rule rule = FixedThreshold.ruleOf(factor.name());
        if (rule == null) {
            throw new IllegalStateException("固定阈值表缺少因子：" + factor.name());
        }
        return new EnvironmentalThresholdRuleVO(factor.name(), factor.getLabel(), rule.status(),
                rule.direction().getCode(), rule.direction().getSymbol(), rule.threshold(), rule.unit(),
                rule.ciLower(), rule.ciUpper(), metrics(rule.metrics2024()), null);
    }

    /**
     * 组合环境条件：没有单列阈值，返回三条路径与整体检验结果。
     */
    private EnvironmentalThresholdRuleVO combinedRule(Option.EnvironmentalFactorOption factor) {
        EnvironmentalThresholdPathVO[] paths = FixedThreshold.COMBINED_PATHS.stream()
                .map(path -> new EnvironmentalThresholdPathVO(path.leaf(), path.conditions().stream()
                        .map(condition -> new EnvironmentalThresholdConditionVO(condition.factor(),
                                condition.factorName(), condition.direction().getCode(),
                                condition.direction().getSymbol(), condition.threshold(), condition.unit()))
                        .toArray(EnvironmentalThresholdConditionVO[]::new)))
                .toArray(EnvironmentalThresholdPathVO[]::new);
        return new EnvironmentalThresholdRuleVO(factor.name(), factor.getLabel(), FixedThreshold.COMBINED_STATUS,
                null, null, null, null, null, null, metrics(FixedThreshold.COMBINED_METRICS_2024), paths);
    }

    /**
     * 常量里的检验结果转接口 VO。
     */
    private EnvironmentalThresholdMetricsVO metrics(FixedThreshold.Metrics metrics) {
        return new EnvironmentalThresholdMetricsVO(metrics.tp(), metrics.fn(), metrics.fp(), metrics.tn(),
                metrics.pod(), metrics.far(), metrics.fpr(), metrics.csi(), metrics.j());
    }

    /**
     * 条件百分比折线图：所选区域、所选因子在 since ~ until 内逐时间点的达到条件百分比。
     *
     * <p>x 轴恒按 granularity 展开 since ~ until（含无数据的时间点），保证前端折线连续：
     * 天粒度一个日期一点；月 / 年粒度把周期内<b>有效日</b>的区域比例取均值（一天一个值、
     * 先加权汇总再对有效日平均）。没有有效日的周期 percent 为 null，不补 0。</p>
     *
     * @param since      统计起始日期（含）
     * @param until      统计结束日期（含），需不早于 since（由接口层校验）
     * @param region     所选行政区（全省 / 地市 / 县市区）
     * @param factor     环境因子
     * @param granularity 时间轴粒度，null 按天
     * @return 折线图数据
     */
    public EnvironmentalThresholdPercentTrendVO conditionTrend(LocalDate since, LocalDate until, RegionCode region,
                                                              Option.EnvironmentalFactorOption factor,
                                                              Option.GranularityOption granularity) {
        Option.GranularityOption effective = effective(granularity);
        NavigableMap<LocalDate, Double> daily = aggregate(conditionRows(since, until, region), factor).daily();
        // 先按粒度铺满时间轴（无数据的时间点占位），再把有数据的日期投进对应周期
        Map<String, List<Double>> buckets = new LinkedHashMap<>();
        LocalDate start = truncate(since, effective);
        LocalDate end = truncate(until, effective);
        for (LocalDate cursor = start; !cursor.isAfter(end); cursor = next(cursor, effective)) {
            buckets.put(periodLabel(cursor, effective), new ArrayList<>());
        }
        daily.forEach((date, percent) -> buckets.computeIfAbsent(periodLabel(date, effective),
                ignored -> new ArrayList<>()).add(percent));
        EnvironmentalThresholdPercentPointVO[] points = buckets.entrySet().stream()
                .map(bucket -> new EnvironmentalThresholdPercentPointVO(bucket.getKey(),
                        average(bucket.getValue()), (long) bucket.getValue().size()))
                .toArray(EnvironmentalThresholdPercentPointVO[]::new);
        return new EnvironmentalThresholdPercentTrendVO(region.getCode(), region.getLabel(), levelOf(region),
                factor.name(), factor.getLabel(), since, until, effective.name(), UNIT_PERCENT, points);
    }

    /**
     * 所选区域各区县的达到条件百分比（地图配色 / 县级横条 / 点击县弹窗）。
     *
     * <p>取值窗口由 granularity 决定：DAY（默认）取所选日期当天，与「地图显示所选单日」一致；
     * MONTH / YEAR 取该日期所在月 / 年内该县有效日比例的平均值（每县一个值，validDays 为参与平均的有效日数）。</p>
     *
     * <p>区域合计 regionPercent 与折线图同口径：DAY 为按可判断格网数加权的各县比例，
     * MONTH / YEAR 为周期内有效日区域比例的均值；区域内一个有效数据都没有时为 null。
     * 范围内的县区全部返回（无数据的 percent 为 null，按降序排在最后）。</p>
     *
     * @param date        所选日期
     * @param region      所选行政区（全省 / 地市 / 县市区）
     * @param factor      环境因子
     * @param granularity 取值粒度，null 按天（DAY）
     * @return 区县百分比与区域合计
     */
    public EnvironmentalThresholdCountyDistributionVO countyConditions(LocalDate date, RegionCode region,
                                                                      Option.EnvironmentalFactorOption factor,
                                                                      Option.GranularityOption granularity) {
        Option.GranularityOption effective = effective(granularity);
        LocalDate since = truncate(date, effective);
        LocalDate until = lastDayOfPeriod(date, effective);
        PercentAggregate aggregate = aggregate(conditionRows(since, until, region), factor);
        List<EnvironmentalThresholdCountyPercentVO> counties = new ArrayList<>();
        for (RegionCode county : RegionCode.selectable(region.getCode())) {
            List<Double> values = aggregate.countyValues().get(county.getCode());
            counties.add(new EnvironmentalThresholdCountyPercentVO(county.getCode(), county.getLabel(),
                    average(values), values == null ? 0L : (long) values.size()));
        }
        counties.sort(BY_PERCENT_DESC);
        List<Double> daily = new ArrayList<>(aggregate.daily().values());
        return new EnvironmentalThresholdCountyDistributionVO(region.getCode(), region.getLabel(), levelOf(region),
                date, effective.name(), factor.name(), factor.getLabel(), UNIT_PERCENT,
                average(daily), (long) daily.size(),
                counties.toArray(EnvironmentalThresholdCountyPercentVO[]::new));
    }

    /**
     * 按「日期区间 + regionCode 范围内的县区」读取条件分布行。
     *
     * <p>只取可判断格网数大于 0 的行：该列为 0 时比例列恒为 NULL，读了也不参与任何汇总。
     * 表内没有地市与省级汇总行，地区范围只能用县级码集合表达；
     * 县区集合统一走 {@link RegionCode#selectable(Integer)}，与其它模块口径一致。</p>
     */
    private List<ConditionDistributionDaily> conditionRows(LocalDate since, LocalDate until, RegionCode region) {
        List<Integer> regionCodes = RegionCode.selectable(region.getCode()).stream()
                .map(RegionCode::getCode)
                .toList();
        if (regionCodes.isEmpty()) {
            return List.of();
        }
        return conditionDistributionDailyMapper.selectList(Wrappers.<ConditionDistributionDaily>lambdaQuery()
                .ge(ConditionDistributionDaily::getAssessmentDate, since)
                .le(ConditionDistributionDaily::getAssessmentDate, until)
                .in(ConditionDistributionDaily::getRegionCode, regionCodes)
                .gt(ConditionDistributionDaily::getValidGridCount, 0)
                .orderByAsc(ConditionDistributionDaily::getAssessmentDate)
                .orderByAsc(ConditionDistributionDaily::getRegionCode));
    }

    /**
     * 条件分布行的聚合结果：每县在各有效日的比例，以及逐日区域加权比例。
     *
     * @param countyValues 区县码 → 周期内各有效日的比例（含重复日期时逐个入列，由调用方取均值）
     * @param daily        日期 → 区域比例（按可判断格网数加权），只含有有效数据的日期
     */
    private record PercentAggregate(Map<Integer, List<Double>> countyValues, NavigableMap<LocalDate, Double> daily) {
    }

    /**
     * 把条件分布行聚合成「每县有效日比例」与「逐日区域比例」。
     *
     * <p>区域比例按可判断格网数加权：Σ(比例 × 可判断格网数) ÷ Σ可判断格网数。
     * 比例列为 NULL（无数据）或可判断格网数不大于 0 的行直接跳过，不参与任何分母。</p>
     */
    private PercentAggregate aggregate(List<ConditionDistributionDaily> rows,
                                       Option.EnvironmentalFactorOption factor) {
        Map<Integer, List<Double>> countyValues = new LinkedHashMap<>();
        Map<LocalDate, double[]> weighted = new TreeMap<>();
        for (ConditionDistributionDaily row : rows) {
            Double percent = percent(row, factor);
            Integer validGridCount = row.getValidGridCount();
            LocalDate date = row.getAssessmentDate();
            Integer regionCode = row.getRegionCode();
            if (percent == null || validGridCount == null || validGridCount <= 0 || date == null
                    || regionCode == null) {
                continue;
            }
            countyValues.computeIfAbsent(regionCode, ignored -> new ArrayList<>()).add(percent);
            double[] sums = weighted.computeIfAbsent(date, ignored -> new double[2]);
            sums[0] += percent * validGridCount;
            sums[1] += validGridCount;
        }
        NavigableMap<LocalDate, Double> daily = new TreeMap<>();
        weighted.forEach((date, sums) -> daily.put(date, sums[0] / sums[1]));
        return new PercentAggregate(countyValues, daily);
    }

    /**
     * 因子 → 该行对应的比例值（与条件分布表的 {@code {因子}_hit_pct} 列一一对应）。
     *
     * <p>表内一列一个因子，行内取哪一列由因子决定，故用一次 switch 显式对应；
     * 与 {@link Option.EnvironmentalFactorOption} 的十个取值必须保持同步。</p>
     */
    private Double percent(ConditionDistributionDaily row, Option.EnvironmentalFactorOption factor) {
        return switch (factor) {
            case lightning_count -> row.getLightningCountHitPct();
            case mean_abs_intensity -> row.getMeanAbsIntensityHitPct();
            case temperature -> row.getTemperatureHitPct();
            case wind_speed -> row.getWindSpeedHitPct();
            case relative_humidity -> row.getRelativeHumidityHitPct();
            case rain -> row.getRainHitPct();
            case fmc -> row.getFmcHitPct();
            case dem -> row.getDemHitPct();
            case slope -> row.getSlopeHitPct();
            case combined -> row.getCombinedHitPct();
        };
    }

    /**
     * 一组比例的等权均值，忽略 null；为空（周期内没有有效日）时返回 null。
     */
    private Double average(List<Double> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        double sum = 0D;
        for (Double value : values) {
            sum += value;
        }
        return sum / values.size();
    }

    /**
     * 粒度缺省值：未指定时按天（与 /ltg-env 的时间轴口径一致）。
     */
    private Option.GranularityOption effective(Option.GranularityOption granularity) {
        return granularity == null ? Option.GranularityOption.DAY : granularity;
    }

    /**
     * 按粒度取区间起点：天取当天，月取当月 1 号，年取当年 1 月 1 号。
     */
    private LocalDate truncate(LocalDate date, Option.GranularityOption granularity) {
        return switch (granularity) {
            case MONTH -> date.withDayOfMonth(1);
            case YEAR -> date.withDayOfYear(1);
            default -> date;
        };
    }

    /**
     * 按粒度取区间终点（含）：天取当天，月取当月最后一天，年取当年最后一天。
     */
    private LocalDate lastDayOfPeriod(LocalDate date, Option.GranularityOption granularity) {
        return switch (granularity) {
            case MONTH -> date.with(TemporalAdjusters.lastDayOfMonth());
            case YEAR -> date.with(TemporalAdjusters.lastDayOfYear());
            default -> date;
        };
    }

    /**
     * 按粒度前进一步：天 +1 天，月 +1 月，年 +1 年。
     */
    private LocalDate next(LocalDate date, Option.GranularityOption granularity) {
        return switch (granularity) {
            case MONTH -> date.plusMonths(1);
            case YEAR -> date.plusYears(1);
            default -> date.plusDays(1);
        };
    }

    /**
     * 时间标签格式，与 /ltg-env 时间轴一致：天 yyyy-MM-dd、月 yyyy-MM、年 yyyy。
     */
    private String periodLabel(LocalDate date, Option.GranularityOption granularity) {
        return switch (granularity) {
            case MONTH -> date.format(MONTH_FORMAT);
            case YEAR -> date.format(YEAR_FORMAT);
            default -> date.format(DAY_FORMAT);
        };
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
}
