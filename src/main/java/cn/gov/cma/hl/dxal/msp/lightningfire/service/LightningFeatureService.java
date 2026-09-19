package cn.gov.cma.hl.dxal.msp.lightningfire.service;

import cn.gov.cma.hl.dxal.msp.lightningfire.constant.SelectOption;
import cn.gov.cma.hl.dxal.msp.lightningfire.dto.lightningfeature.CorrelationCoefficientDTO;
import cn.gov.cma.hl.dxal.msp.lightningfire.dto.lightningfeature.GridDTO;
import cn.gov.cma.hl.dxal.msp.lightningfire.dto.lightningfeature.LeadingFactorDTO;
import cn.gov.cma.hl.dxal.msp.lightningfire.dto.lightningfeature.TimeDimChartDTO;
import cn.gov.cma.hl.dxal.msp.lightningfire.dto.lightningfeature.TimeSeriesDTO;
import cn.gov.cma.hl.dxal.msp.lightningfire.entity.CorrelationCoefficientStatistics;
import cn.gov.cma.hl.dxal.msp.lightningfire.entity.LeadingFactorStatistics;
import cn.gov.cma.hl.dxal.msp.lightningfire.entity.LightningRegionStatistics;
import cn.gov.cma.hl.dxal.msp.lightningfire.mapper.CorrelationCoefficientStatisticsMapper;
import cn.gov.cma.hl.dxal.msp.lightningfire.mapper.LeadingFactorStatisticsMapper;
import cn.gov.cma.hl.dxal.msp.lightningfire.mapper.LightningRegionStatisticsMapper;
import cn.gov.cma.hl.dxal.msp.lightningfire.vo.CorrelationCoefficientVO;
import cn.gov.cma.hl.dxal.msp.lightningfire.vo.LeadingFactorVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class LightningFeatureService {

    /**
     * 条形图 x 轴因子顺序与名称，与各统计表的建表列顺序一致。
     */
    private static final String[] FACTOR_LABELS =
            {"相对湿度", "温度", "降水", "地面高程", "可燃物含水率", "坡度", "风速"};

    private final LightningRegionStatisticsMapper lightningRegionStatisticsMapper;

    private final CorrelationCoefficientStatisticsMapper correlationCoefficientStatisticsMapper;

    private final LeadingFactorStatisticsMapper leadingFactorStatisticsMapper;

    public GridDTO lightningFeatureGrid(LocalDate since, LocalDate until, SelectOption.RegionOption region) {
        return lightningRegionStatisticsMapper.selectLightningSummaryByCondition(buildQuery(since, until, region));
    }

    public TimeDimChartDTO timeDimensionChart(LocalDate since, LocalDate until, SelectOption.RegionOption region, SelectOption.GranularityOption granularity) {
        List<TimeSeriesDTO> points = lightningRegionStatisticsMapper.selectTimeSeriesByCondition(
                buildQuery(since, until, region), resolvePeriodFormat(granularity));
        // 遍历完整的日期（since - until），缺失的时间段补 0
        points = fillMissingPeriods(points, since, until, granularity);

        List<String> series = new ArrayList<>(points.size());
        List<Integer> counts = new ArrayList<>(points.size());
        int maxAt = -1;
        int maxValue = Integer.MIN_VALUE;
        for (int i = 0; i < points.size(); i++) {
            TimeSeriesDTO point = points.get(i);
            int count = point.count() == null ? 0 : point.count();
            series.add(point.period());
            counts.add(count);
            if (count > maxValue) {
                maxValue = count;
                maxAt = i;
            }
        }
        boolean empty = points.isEmpty();
        return new TimeDimChartDTO(series, counts, empty ? null : maxAt, empty ? null : maxValue);
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
     * 按 x 轴因子顺序取相关系数，null（无数据/整列为 NULL）记 0。
     */
    private List<Double> correlationValues(CorrelationCoefficientDTO coefficient) {
        if (coefficient == null) {
            return List.of(0D, 0D, 0D, 0D, 0D, 0D, 0D);
        }
        return List.of(
                zeroIfNull(coefficient.relativeHumidity()),
                zeroIfNull(coefficient.temperature()),
                zeroIfNull(coefficient.rain()),
                zeroIfNull(coefficient.dem()),
                zeroIfNull(coefficient.fmc()),
                zeroIfNull(coefficient.slope()),
                zeroIfNull(coefficient.windSpeed()));
    }

    /**
     * 按 x 轴因子顺序取主导因子权重，null（无数据/整列为 NULL）记 0。
     */
    private List<Double> leadingFactorValues(LeadingFactorDTO leadingFactor) {
        if (leadingFactor == null) {
            return List.of(0D, 0D, 0D, 0D, 0D, 0D, 0D);
        }
        return List.of(
                zeroIfNull(leadingFactor.relativeHumidity()),
                zeroIfNull(leadingFactor.temperature()),
                zeroIfNull(leadingFactor.rain()),
                zeroIfNull(leadingFactor.dem()),
                zeroIfNull(leadingFactor.fmc()),
                zeroIfNull(leadingFactor.slope()),
                zeroIfNull(leadingFactor.windSpeed()));
    }

    private Double zeroIfNull(Double value) {
        return value == null ? 0D : value;
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

    private LambdaQueryWrapper<LightningRegionStatistics> buildQuery(LocalDate since, LocalDate until, SelectOption.RegionOption region) {
        LambdaQueryWrapper<LightningRegionStatistics> q = Wrappers.lambdaQuery();
        if (since != null) {
            q.ge(LightningRegionStatistics::getStatDate, since);
        }
        if (until != null) {
            q.le(LightningRegionStatistics::getStatDate, until);
        }
        if (region != null && region != SelectOption.RegionOption.all) {
            q.eq(LightningRegionStatistics::getRegion, region.getLabel());
        }
        return q;
    }


    private String resolvePeriodFormat(SelectOption.GranularityOption granularity) {
        return switch (granularity == null ? SelectOption.GranularityOption.DAY : granularity) {
            case MONTH -> "yyyy-MM";
            case YEAR -> "yyyy";
            default -> "yyyy-MM-dd";
        };
    }

    /**
     * 将查询结果按粒度展开为 since - until 的完整时间轴，缺失的时间段补 0。
     * <p>since / until 为空（或区间内无数据）时无法确定完整轴，原样返回查询结果。</p>
     */
    private List<TimeSeriesDTO> fillMissingPeriods(List<TimeSeriesDTO> points, LocalDate since, LocalDate until,
                                                  SelectOption.GranularityOption granularity) {
        if (since == null || until == null) {
            return points;
        }

        LocalDate start = truncate(since, granularity);
        LocalDate end = truncate(until, granularity);
        if (start.isAfter(end)) {
            return points;
        }

        Map<String, Integer> countsByPeriod = new LinkedHashMap<>();
        for (TimeSeriesDTO point : points) {
            countsByPeriod.put(point.period(), point.count() == null ? 0 : point.count());
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(resolvePeriodFormat(granularity));
        List<TimeSeriesDTO> filled = new ArrayList<>();
        LocalDate cursor = start;
        while (!cursor.isAfter(end)) {
            String period = cursor.format(formatter);
            filled.add(new TimeSeriesDTO(period, countsByPeriod.getOrDefault(period, 0)));
            cursor = next(cursor, granularity);
        }
        return filled;
    }

    /**
     * 按粒度取区间起点：天取当天，月取当月 1 号，年取当年 1 月 1 号。
     */
    private LocalDate truncate(LocalDate date, SelectOption.GranularityOption granularity) {
        return switch (granularity == null ? SelectOption.GranularityOption.DAY : granularity) {
            case MONTH -> date.withDayOfMonth(1);
            case YEAR -> date.withDayOfYear(1);
            default -> date;
        };
    }

    /**
     * 按粒度前进一步：天 +1 天，月 +1 月，年 +1 年。
     */
    private LocalDate next(LocalDate date, SelectOption.GranularityOption granularity) {
        return switch (granularity == null ? SelectOption.GranularityOption.DAY : granularity) {
            case MONTH -> date.plusMonths(1);
            case YEAR -> date.plusYears(1);
            default -> date.plusDays(1);
        };
    }
}
