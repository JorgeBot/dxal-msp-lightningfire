package cn.gov.cma.hl.dxal.msp.lightningfire.service;

import cn.gov.cma.hl.dxal.msp.lightningfire.constant.DynamicThreshold;
import cn.gov.cma.hl.dxal.msp.lightningfire.constant.FixedThreshold;
import cn.gov.cma.hl.dxal.msp.lightningfire.constant.Option;
import cn.gov.cma.hl.dxal.msp.lightningfire.constant.RegionCode;
import cn.gov.cma.hl.dxal.msp.lightningfire.entity.DynamicConditionDistributionDaily;
import cn.gov.cma.hl.dxal.msp.lightningfire.mapper.DynamicConditionDistributionDailyMapper;
import cn.gov.cma.hl.dxal.msp.lightningfire.vo.DynamicThresholdCandidateVO;
import cn.gov.cma.hl.dxal.msp.lightningfire.vo.DynamicThresholdCountyDistributionVO;
import cn.gov.cma.hl.dxal.msp.lightningfire.vo.DynamicThresholdCountyVO;
import cn.gov.cma.hl.dxal.msp.lightningfire.vo.DynamicThresholdVO;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 功能三「动态阈值」：逐年候选阈值序列，以及所选区域应用当年候选后的逐日结果。
 *
 * <p>数据分两部分，互不混用：</p>
 * <ul>
 *   <li><b>年度候选（尺子）</b>：2019—2025 年每个因子一条候选线（阈值、固定基线、差值、置信区间、
 *       训练天数、检验与复核建议），提取为常量 {@link DynamicThreshold}
 *       （来源 功能三_动态阈值/结果/年度候选与漂移.csv），当期固定基线见 {@link FixedThreshold}，
 *       接口直接查表，不重新训练；</li>
 *   <li><b>逐日结果（量出来的）</b>：县级逐日动态条件分布表 dynamic_condition_distribution_daily，
 *       每行一个县一天的两列百分比：{@code {因子}_hit_pct}（符合当年候选条件）与
 *       {@code {因子}_changed_pct}（与旧固定基线条件判定不同），本类只做读取与汇总。</li>
 * </ul>
 *
 * <p>口径与约定：</p>
 * <ul>
 *   <li>日期决定用哪一年的候选线（日期 2024-08-20 → year=2024），逐日表存的就是应用该线后的结果，
 *       接口不重新逐格判定；</li>
 *   <li>多县汇总按 {@code valid_grid_count} 加权：Σ(县百分比 × 可判断格网数) ÷ Σ可判断格网数，
 *       等价于开发字段说明的「符合数（或变化数）之和 ÷ 可判断数之和」，不是各县百分比简单平均；</li>
 *   <li>无数据不补 0：该日没有判定结果（5—9 月范围之外、没有含林格网、或该年只有候选线没有逐日结果）
 *       时百分比为 null，与「0%（有数据但没有格网符合）」可区分；</li>
 *   <li>判断变化不是准确率提高，也不是「符合旧条件」的同义词：新条件 0% 而变化 100% 表示
 *       原先全部符合、现在全部不符合，两者一般不互补；整体历史的 flipRate 不能用于当前县地图。</li>
 * </ul>
 *
 * <p>区县范围统一走 {@link RegionCode#selectable(Integer)}：选县市区返回该县本身、
 * 选地市返回所辖县区、选 230000 返回全省 125 个县级单元（表中没有地市与省级汇总行）。
 * 动态阈值只有九个单因子，没有组合环境条件（{@code combined} 由接口层拒绝）。</p>
 */
@Service
@RequiredArgsConstructor
public class DynamicThresholdService {

    private static final String LEVEL_PROVINCE = "province";

    private static final String LEVEL_CITY = "city";

    private static final String LEVEL_COUNTY = "county";

    /**
     * 动态条件分布表的百分比列本身就是 0—100 的数值，单位固定为「%」，接口不再做换算。
     */
    private static final String UNIT_PERCENT = "%";

    /**
     * 符合新条件百分比降序（无数据的排最后，同值按区划码升序）：地图与县级横条按高低展示。
     */
    private static final Comparator<DynamicThresholdCountyVO> BY_HIT_DESC =
            Comparator.comparing(DynamicThresholdCountyVO::hitPercent,
                            Comparator.nullsLast(Comparator.reverseOrder()))
                    .thenComparing(DynamicThresholdCountyVO::code);

    private final DynamicConditionDistributionDailyMapper dynamicConditionDistributionDailyMapper;

    /**
     * 「当前的固定阈值」与固定阈值模块共用同一条组装逻辑：动态阈值的基线列就是它。
     */
    private final FixedThresholdService fixedThresholdService;

    /**
     * 动态阈值：所选因子 2019—2025 年的逐年候选与当前的固定阈值。
     *
     * @param factor 环境因子（九个单因子之一，不含 combined）
     * @return 年度候选序列与当前固定阈值
     */
    public DynamicThresholdVO annualCandidates(Option.EnvironmentalFactorOption factor) {
        DynamicThreshold.Rule rule = DynamicThreshold.ruleOf(factor.name());
        if (rule == null) {
            throw new IllegalStateException("动态阈值表缺少因子：" + factor.name());
        }
        DynamicThresholdCandidateVO[] candidates = rule.candidates().stream()
                .map(this::candidate)
                .toArray(DynamicThresholdCandidateVO[]::new);
        return new DynamicThresholdVO(rule.factor(), rule.name(), rule.unit(), rule.baselineOperator(),
                symbol(rule.baselineOperator()), rule.window(),
                fixedThresholdService.rule(factor), candidates);
    }

    /**
     * 所选区域各区县在所选日期的动态阈值结果：符合新条件百分比与判定改变百分比，另给区域加权合计。
     *
     * <p>区域合计对两列分别按可判断格网数加权；区域内一个有效数据都没有时两个合计都为 null。
     * 范围内的县区全部返回（无结果的百分比为 null、按降序排在最后）。</p>
     *
     * @param date   所选日期（逐日动态表覆盖 2019-05-01—2024-09-30 的 5—9 月）
     * @param region 所选行政区（全省 / 地市 / 县市区）
     * @param factor 环境因子（九个单因子之一，不含 combined）
     * @return 各区县结果与区域合计
     */
    public DynamicThresholdCountyDistributionVO countyDistribution(LocalDate date, RegionCode region,
                                                                  Option.EnvironmentalFactorOption factor) {
        List<DynamicConditionDistributionDaily> rows = rows(date, region);
        Map<Integer, Percents> byCounty = new LinkedHashMap<>();
        // 区域合计：符合新条件与判定改变分别加权，分母各自只为该列有值的可判断格网数
        double hitSum = 0D;
        long hitGrids = 0L;
        double changedSum = 0D;
        long changedGrids = 0L;
        Integer assessmentYear = null;
        for (DynamicConditionDistributionDaily row : rows) {
            Integer regionCode = row.getRegionCode();
            Integer validGridCount = row.getValidGridCount();
            if (regionCode == null || validGridCount == null || validGridCount <= 0) {
                continue;
            }
            Percents percents = percents(row, factor);
            byCounty.put(regionCode, percents);
            if (percents.hit() != null) {
                hitSum += percents.hit() * validGridCount;
                hitGrids += validGridCount;
            }
            if (percents.changed() != null) {
                changedSum += percents.changed() * validGridCount;
                changedGrids += validGridCount;
            }
            if (assessmentYear == null && row.getAssessmentYear() != null) {
                assessmentYear = row.getAssessmentYear().intValue();
            }
        }
        List<DynamicThresholdCountyVO> counties = new ArrayList<>();
        for (RegionCode county : RegionCode.selectable(region.getCode())) {
            Percents percents = byCounty.get(county.getCode());
            counties.add(new DynamicThresholdCountyVO(county.getCode(), county.getLabel(),
                    percents == null ? null : percents.hit(),
                    percents == null ? null : percents.changed()));
        }
        counties.sort(BY_HIT_DESC);
        return new DynamicThresholdCountyDistributionVO(region.getCode(), region.getLabel(), levelOf(region),
                date, assessmentYear == null ? date.getYear() : assessmentYear,
                factor.name(), factor.getLabel(), UNIT_PERCENT,
                hitGrids == 0L ? null : hitSum / hitGrids,
                changedGrids == 0L ? null : changedSum / changedGrids,
                counties.toArray(DynamicThresholdCountyVO[]::new));
    }

    /**
     * 按「日期 + regionCode 范围内的县区」读取逐日动态分布行。
     *
     * <p>只取可判断格网数大于 0 的行：该列为 0 时两列百分比恒为 NULL，读了也不参与任何汇总。
     * 表内没有地市与省级汇总行，地区范围只能用县级码集合表达。</p>
     */
    private List<DynamicConditionDistributionDaily> rows(LocalDate date, RegionCode region) {
        List<Integer> regionCodes = RegionCode.selectable(region.getCode()).stream()
                .map(RegionCode::getCode)
                .toList();
        if (regionCodes.isEmpty()) {
            return List.of();
        }
        return dynamicConditionDistributionDailyMapper.selectList(
                Wrappers.<DynamicConditionDistributionDaily>lambdaQuery()
                        .eq(DynamicConditionDistributionDaily::getAssessmentDate, date)
                        .in(DynamicConditionDistributionDaily::getRegionCode, regionCodes)
                        .gt(DynamicConditionDistributionDaily::getValidGridCount, 0)
                        .orderByAsc(DynamicConditionDistributionDaily::getRegionCode));
    }

    /**
     * 一行里某因子的两个百分比：符合新条件与判定改变。
     */
    private record Percents(Double hit, Double changed) {
    }

    /**
     * 因子 → 该行的 [符合新条件比例, 判定改变比例]（与动态条件分布表的两列一一对应）。
     *
     * <p>表内一个因子两列，取哪两列由因子决定，故用一次 switch 显式对应；
     * 与 {@link Option.EnvironmentalFactorOption} 的取值保持同步。动态阈值没有组合环境条件，
     * 该取值由接口层拒绝，这里再兜一层防止误用。</p>
     */
    private Percents percents(DynamicConditionDistributionDaily row, Option.EnvironmentalFactorOption factor) {
        return switch (factor) {
            case lightning_count -> new Percents(row.getLightningCountHitPct(), row.getLightningCountChangedPct());
            case mean_abs_intensity ->
                    new Percents(row.getMeanAbsIntensityHitPct(), row.getMeanAbsIntensityChangedPct());
            case temperature -> new Percents(row.getTemperatureHitPct(), row.getTemperatureChangedPct());
            case wind_speed -> new Percents(row.getWindSpeedHitPct(), row.getWindSpeedChangedPct());
            case relative_humidity ->
                    new Percents(row.getRelativeHumidityHitPct(), row.getRelativeHumidityChangedPct());
            case rain -> new Percents(row.getRainHitPct(), row.getRainChangedPct());
            case fmc -> new Percents(row.getFmcHitPct(), row.getFmcChangedPct());
            case dem -> new Percents(row.getDemHitPct(), row.getDemChangedPct());
            case slope -> new Percents(row.getSlopeHitPct(), row.getSlopeChangedPct());
            case combined -> throw new IllegalStateException("动态阈值没有组合环境条件：" + factor);
        };
    }

    /**
     * 常量里的年度候选行转接口 VO：缺少训练天数由计划天数减可用天数得到。
     */
    private DynamicThresholdCandidateVO candidate(DynamicThreshold.Candidate candidate) {
        return new DynamicThresholdCandidateVO((int) candidate.year(), candidate.operator(),
                symbol(candidate.operator()), candidate.threshold(), candidate.baseline(), candidate.delta(),
                candidate.ciLow(), candidate.ciHigh(), candidate.flipRate(), candidate.positive(),
                candidate.availableDays(), candidate.expectedDays(),
                candidate.expectedDays() - candidate.availableDays(), (int) candidate.trainStart(),
                (int) candidate.trainEnd(), candidate.evaluationJ(), candidate.directionChanged(),
                DynamicThreshold.STATUS_CANDIDATE_NOT_ACTIVE, candidate.adoptionStatus(), candidate.adoptionLabel());
    }

    /**
     * 比较方向取值（ge / gt / le）转展示符号；未知取值返回 null。
     */
    private String symbol(String operator) {
        FixedThreshold.Direction direction = FixedThreshold.Direction.fromCode(operator);
        return direction == null ? null : direction.getSymbol();
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
