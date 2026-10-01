package cn.gov.cma.hl.dxal.msp.lightningfire.service;

import cn.gov.cma.hl.dxal.msp.lightningfire.constant.Option;
import cn.gov.cma.hl.dxal.msp.lightningfire.constant.RegionCode;
import cn.gov.cma.hl.dxal.msp.lightningfire.dto.lightningfeature.CorrelationCoefficientDTO;
import cn.gov.cma.hl.dxal.msp.lightningfire.dto.lightningfeature.LeadingFactorDTO;
import cn.gov.cma.hl.dxal.msp.lightningfire.dto.lightningfeature.LightningCharacteristicsDTO;
import cn.gov.cma.hl.dxal.msp.lightningfire.dto.lightningfeature.LightningFireDTO;
import cn.gov.cma.hl.dxal.msp.lightningfire.dto.lightningfeature.LightningFireSummaryDTO;
import cn.gov.cma.hl.dxal.msp.lightningfire.entity.CorrelationCoefficientStatistics;
import cn.gov.cma.hl.dxal.msp.lightningfire.entity.ForestFireStatistics;
import cn.gov.cma.hl.dxal.msp.lightningfire.entity.LeadingFactorStatistics;
import cn.gov.cma.hl.dxal.msp.lightningfire.entity.LightningRegionStatistics;
import cn.gov.cma.hl.dxal.msp.lightningfire.mapper.CorrelationCoefficientStatisticsMapper;
import cn.gov.cma.hl.dxal.msp.lightningfire.mapper.ForestFireStatisticsMapper;
import cn.gov.cma.hl.dxal.msp.lightningfire.mapper.LeadingFactorStatisticsMapper;
import cn.gov.cma.hl.dxal.msp.lightningfire.mapper.LightningRegionStatisticsMapper;
import cn.gov.cma.hl.dxal.msp.lightningfire.vo.CorrelationCoefficientVO;
import cn.gov.cma.hl.dxal.msp.lightningfire.vo.LeadingFactorVO;
import cn.gov.cma.hl.dxal.msp.lightningfire.vo.LightningFireTimeAxisVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LightningFeatureService {

    /**
     * 条形图 x 轴因子顺序与名称，与各统计表的建表列顺序一致。
     */
    private static final String[] FACTOR_LABELS =
            {"相对湿度", "温度", "降水", "地面高程", "可燃物含水率", "坡度", "风速"};

    /**
     * 地闪特征兜底值：聚合查询恒返回一行，仅在查询结果意外为空时使用。
     */
    private static final LightningCharacteristicsDTO EMPTY_CHARACTERISTICS =
            new LightningCharacteristicsDTO(0, 0F, 0F, 0F);

    /**
     * 雷击火统计兜底值：聚合查询恒返回一行，仅在查询结果意外为空时使用。
     */
    private static final LightningFireSummaryDTO EMPTY_FIRE_SUMMARY = new LightningFireSummaryDTO(0L, 0D, 0D);

    private final LightningRegionStatisticsMapper lightningRegionStatisticsMapper;

    private final CorrelationCoefficientStatisticsMapper correlationCoefficientStatisticsMapper;

    private final LeadingFactorStatisticsMapper leadingFactorStatisticsMapper;

    private final ForestFireStatisticsMapper forestFireStatisticsMapper;

    /**
     * 地闪特征：区间内（可按区域过滤）的地闪次数、正闪比例、地闪密度与地闪强度。
     *
     * <p>比例 / 密度 / 强度按各地区地闪记录数加权平均，已由 SQL 完成聚合与四舍五入；
     * 无数据时各项均记 0，不返回 null。</p>
     */
    public LightningCharacteristicsDTO lightningCharacteristics(LocalDate since, LocalDate until, RegionCode regionCode) {
        LightningCharacteristicsDTO characteristics = lightningRegionStatisticsMapper
                .selectLightningSummaryByCondition(buildRegionQuery(since, until, regionCode));
        return characteristics == null ? EMPTY_CHARACTERISTICS : characteristics;
    }

    /**
     * 雷击火时间轴：区间内按粒度展开的完整时间轴 + 区间内的雷击火点位。
     *
     * <p>timeAxis 恒按 since ~ until 展开（含无火日期），保证前端 X 轴连续；
     * series 为区间内（可按区域过滤）的雷击火记录，按发现时间升序，无记录时为空数组。</p>
     */
    public LightningFireTimeAxisVO lightningFireTimeAxis(LocalDate since, LocalDate until,
                                                        RegionCode regionCode,
                                                        Option.GranularityOption granularity) {
        List<LightningFireDTO> series = forestFireStatisticsMapper
                .selectLightningFireSeries(buildLightningFireQuery(since, until, regionCode));
        String[] timeAxis = buildTimeAxis(since, until, granularity).toArray(String[]::new);
        return new LightningFireTimeAxisVO(timeAxis, series.toArray(LightningFireDTO[]::new));
    }

    /**
     * 雷击火统计：区间内（可按区域过滤）的事件次数、过火面积合计与平均过火面积。
     *
     * <p>聚合已由 SQL 完成，无记录时三项均为 0；过火面积单位 hm²，与源档案一致。
     * 点位密度需要区域面积，属展示口径，由统计概览按区域面积常量（RegionArea）另行计算。</p>
     */
    public LightningFireSummaryDTO lightningFireSummary(LocalDate since, LocalDate until, RegionCode regionCode) {
        LightningFireSummaryDTO summary = forestFireStatisticsMapper
                .selectLightningFireSummary(buildLightningFireQuery(since, until, regionCode));
        return summary == null ? EMPTY_FIRE_SUMMARY : summary;
    }

    /**
     * 相关系数条形图：区间内各环境因子相关系数的均值。
     *
     * <p>相关系数已由上游计算完成并按日存储，这里只做区间聚合，不参与任何相关计算。
     * 无数据、或整列为 NULL 时按 0 处理；x 轴因子名称恒返回，保证空数据时仍能得到一条全 0 的条形图。</p>
     */
    public CorrelationCoefficientVO correlationCoefficientBarChart(LocalDate since, LocalDate until) {
        CorrelationCoefficientDTO coefficient = correlationCoefficientStatisticsMapper
                .selectAverageCorrelationCoefficient(buildDateRangeQuery(since, until, CorrelationCoefficientStatistics::getDate));
        Bars bars = barsByAbsoluteValueDesc(correlationValues(coefficient));
        return new CorrelationCoefficientVO(bars.xAxis(), bars.yAxis());
    }

    /**
     * 主导因子条形图：区间内各环境因子主导权重的均值。
     *
     * <p>权重已由上游计算完成并按日存储，这里只做区间聚合。
     * 无数据、或整列为 NULL 时按 0 处理；x 轴因子名称恒返回，保证空数据时仍能得到一条全 0 的条形图。</p>
     */
    public LeadingFactorVO leadingFactorBarChart(LocalDate since, LocalDate until) {
        LeadingFactorDTO leadingFactor = leadingFactorStatisticsMapper
                .selectAverageLeadingFactor(buildDateRangeQuery(since, until, LeadingFactorStatistics::getDate));
        Bars bars = barsByAbsoluteValueDesc(leadingFactorValues(leadingFactor));
        return new LeadingFactorVO(bars.xAxis(), bars.yAxis());
    }

    /**
     * 条形图数据：x 轴因子名称与 y 轴数值，顺序一一对应。
     */
    private record Bars(String[] xAxis, Double[] yAxis) {
    }

    /**
     * 按数值绝对值降序排列：最显著的因子排在最前。
     * <p>List.sort 为稳定排序，绝对值相同（含全 0）时保持建表列顺序。</p>
     */
    private Bars barsByAbsoluteValueDesc(List<Double> values) {
        List<Integer> order = new ArrayList<>(FACTOR_LABELS.length);
        for (int i = 0; i < FACTOR_LABELS.length; i++) {
            order.add(i);
        }
        order.sort(Comparator.comparingDouble((Integer i) -> Math.abs(values.get(i))).reversed());

        String[] xAxis = new String[order.size()];
        Double[] yAxis = new Double[order.size()];
        for (int i = 0; i < order.size(); i++) {
            int index = order.get(i);
            xAxis[i] = FACTOR_LABELS[index];
            yAxis[i] = values.get(index);
        }
        return new Bars(xAxis, yAxis);
    }

    /**
     * 按 x 轴因子顺序取相关系数。
     */
    private List<Double> correlationValues(CorrelationCoefficientDTO coefficient) {
        if (coefficient == null) {
            return zeroFactorValues();
        }
        return factorValues(coefficient.relativeHumidity(), coefficient.temperature(), coefficient.rain(),
                coefficient.dem(), coefficient.fmc(), coefficient.slope(), coefficient.windSpeed());
    }

    /**
     * 按 x 轴因子顺序取主导因子权重。
     */
    private List<Double> leadingFactorValues(LeadingFactorDTO leadingFactor) {
        if (leadingFactor == null) {
            return zeroFactorValues();
        }
        return factorValues(leadingFactor.relativeHumidity(), leadingFactor.temperature(), leadingFactor.rain(),
                leadingFactor.dem(), leadingFactor.fmc(), leadingFactor.slope(), leadingFactor.windSpeed());
    }

    /**
     * 按 x 轴因子顺序整理数值，null（无数据 / 整列为 NULL）记 0。
     */
    private List<Double> factorValues(Double... values) {
        List<Double> filled = new ArrayList<>(values.length);
        for (Double value : values) {
            filled.add(value == null ? 0D : value);
        }
        return filled;
    }

    /**
     * 无数据时的全 0 因子序列，长度与 x 轴因子数一致。
     */
    private List<Double> zeroFactorValues() {
        return Collections.nCopies(FACTOR_LABELS.length, 0D);
    }

    /**
     * 按日期区间构造查询条件，since / until 为空时不加对应边界。
     */
    private <T> LambdaQueryWrapper<T> buildDateRangeQuery(LocalDate since, LocalDate until, SFunction<T, LocalDate> dateColumn) {
        LambdaQueryWrapper<T> q = Wrappers.lambdaQuery();
        if (since != null) {
            q.ge(dateColumn, since);
        }
        if (until != null) {
            q.le(dateColumn, until);
        }
        return q;
    }

    /**
     * 地闪统计查询条件：日期区间 + 可选区域。
     */
    private LambdaQueryWrapper<LightningRegionStatistics> buildRegionQuery(LocalDate since, LocalDate until, RegionCode regionCode) {
        LambdaQueryWrapper<LightningRegionStatistics> q =
                buildDateRangeQuery(since, until, LightningRegionStatistics::getStatDate);
        applyRegion(q, regionCode, LightningRegionStatistics::getRegionName);
        return q;
    }

    /**
     * 雷击火查询条件：发现时间区间 + 可选区域。
     *
     * <p>until 为闭区间端点，取「小于次日零点」，既包含一整天又不依赖时间部分的精度。</p>
     */
    private LambdaQueryWrapper<ForestFireStatistics> buildLightningFireQuery(LocalDate since, LocalDate until, RegionCode region) {
        LambdaQueryWrapper<ForestFireStatistics> q = Wrappers.lambdaQuery();
        if (since != null) {
            q.ge(ForestFireStatistics::getDiscoveredAt, since.atStartOfDay());
        }
        if (until != null) {
            q.lt(ForestFireStatistics::getDiscoveredAt, until.plusDays(1).atStartOfDay());
        }
        applyRegion(q, region, ForestFireStatistics::getRegion);
        return q;
    }

    /**
     * 区域条件：把 6 位区划码表达的选区落到「地区名」列上。
     *
     * <p>地闪统计表（region_name）与雷击火记录表（region）都按地区名称存区域，没有区划码列，
     * 因此取 {@link RegionCode#selectable(Integer)} 给出的选区内县区名做 IN 过滤：全省（230000）
     * 为省内全部县区、地市（如 232700）为所辖县区、县区（如 232701）为该县区本身。
     * 直接按地市名过滤会漏掉县级记录（两表都只有县区行），按码段过滤又依赖列内容格式，
     * 故统一由枚举给出「选区内有哪些区划」这一条规则。</p>
     */
    private <T> void applyRegion(LambdaQueryWrapper<T> q, RegionCode regionCode, SFunction<T, String> regionColumn) {
        if (regionCode == null) {
            return;
        }
        List<String> regionNames = RegionCode.selectable(regionCode.getCode()).stream()
                .map(RegionCode::getLabel)
                .toList();
        if (!regionNames.isEmpty()) {
            q.in(regionColumn, regionNames);
        }
    }

    /**
     * 将 since ~ until 按粒度展开为完整时间轴，如 2024-06-01 / 2024-06 / 2024。
     *
     * <p>区间端点先归一到粒度起点（月取当月 1 号、年取当年 1 月 1 号）；
     * since / until 为空或 since 晚于 until 时无法确定完整轴，返回空数组。</p>
     */
    private List<String> buildTimeAxis(LocalDate since, LocalDate until, Option.GranularityOption granularity) {
        if (since == null || until == null) {
            return List.of();
        }

        LocalDate start = truncate(since, granularity);
        LocalDate end = truncate(until, granularity);
        if (start.isAfter(end)) {
            return List.of();
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(periodFormat(granularity));
        List<String> axis = new ArrayList<>();
        for (LocalDate cursor = start; !cursor.isAfter(end); cursor = next(cursor, granularity)) {
            axis.add(cursor.format(formatter));
        }
        return axis;
    }

    /**
     * 时间标签格式：天 yyyy-MM-dd，月 yyyy-MM，年 yyyy。
     */
    private String periodFormat(Option.GranularityOption granularity) {
        return switch (effective(granularity)) {
            case MONTH -> "yyyy-MM";
            case YEAR -> "yyyy";
            default -> "yyyy-MM-dd";
        };
    }

    /**
     * 按粒度取区间起点：天取当天，月取当月 1 号，年取当年 1 月 1 号。
     */
    private LocalDate truncate(LocalDate date, Option.GranularityOption granularity) {
        return switch (effective(granularity)) {
            case MONTH -> date.withDayOfMonth(1);
            case YEAR -> date.withDayOfYear(1);
            default -> date;
        };
    }

    /**
     * 按粒度前进一步：天 +1 天，月 +1 月，年 +1 年。
     */
    private LocalDate next(LocalDate date, Option.GranularityOption granularity) {
        return switch (effective(granularity)) {
            case MONTH -> date.plusMonths(1);
            case YEAR -> date.plusYears(1);
            default -> date.plusDays(1);
        };
    }

    /**
     * 粒度缺省值：未指定时按天。
     */
    private Option.GranularityOption effective(Option.GranularityOption granularity) {
        return granularity == null ? Option.GranularityOption.DAY : granularity;
    }
}
