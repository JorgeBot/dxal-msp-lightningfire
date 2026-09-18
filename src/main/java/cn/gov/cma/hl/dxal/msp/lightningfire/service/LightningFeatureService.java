package cn.gov.cma.hl.dxal.msp.lightningfire.service;

import cn.gov.cma.hl.dxal.msp.lightningfire.constant.SelectOption;
import cn.gov.cma.hl.dxal.msp.lightningfire.dto.lightningfeature.GridDTO;
import cn.gov.cma.hl.dxal.msp.lightningfire.dto.lightningfeature.RegionCountDTO;
import cn.gov.cma.hl.dxal.msp.lightningfire.dto.lightningfeature.RegionDimChartDTO;
import cn.gov.cma.hl.dxal.msp.lightningfire.dto.lightningfeature.TimeDimChartDTO;
import cn.gov.cma.hl.dxal.msp.lightningfire.dto.lightningfeature.TimeSeriesDTO;
import cn.gov.cma.hl.dxal.msp.lightningfire.entity.DailyRegionStatistics;
import cn.gov.cma.hl.dxal.msp.lightningfire.mapper.DailyRegionStatisticsMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LightningFeatureService {

    private final DailyRegionStatisticsMapper dailyRegionStatisticsMapper;

    public GridDTO lightningFeatureGrid(LocalDate since, LocalDate until, SelectOption.RegionOption region) {
        return dailyRegionStatisticsMapper.selectLightningSummaryByCondition(buildQuery(since, until, region));
    }

    public TimeDimChartDTO timeDimensionChart(LocalDate since, LocalDate until, SelectOption.RegionOption region, SelectOption.GranularityOption granularity) {
        List<TimeSeriesDTO> points = dailyRegionStatisticsMapper.selectTimeSeriesByCondition(
                buildQuery(since, until, region), resolvePeriodFormat(granularity));

        List<String> series = new ArrayList<>();
        List<Integer> counts = new ArrayList<>();
        int maxAt = -1;
        int maxValue = Integer.MIN_VALUE;
        for (int i = 0; i < points.size(); i++) {
            TimeSeriesDTO point = points.get(i);
            series.add(point.period());
            counts.add(point.count());
            if (point.count() > maxValue) {
                maxValue = point.count();
                maxAt = i;
            }
        }
        boolean empty = points.isEmpty();
        return new TimeDimChartDTO(series, counts, empty ? null : maxAt, empty ? null : maxValue);
    }

    public RegionDimChartDTO regionDimensionChart(LocalDate since, LocalDate until, SelectOption.RegionOption region) {
        List<RegionCountDTO> rows = dailyRegionStatisticsMapper.selectRegionCountByCondition(buildQuery(since, until, region));
        List<String> regions = new ArrayList<>();
        List<Integer> counts = new ArrayList<>();
        for (RegionCountDTO row : rows) {
            regions.add(resolveRegionLabel(row.region()));
            counts.add(row.count());
        }
        return new RegionDimChartDTO(regions, counts);
    }

    private LambdaQueryWrapper<DailyRegionStatistics> buildQuery(LocalDate since, LocalDate until, SelectOption.RegionOption region) {
        LambdaQueryWrapper<DailyRegionStatistics> q = Wrappers.lambdaQuery();
        if (since != null) {
            q.ge(DailyRegionStatistics::getStatDate, since);
        }
        if (until != null) {
            q.le(DailyRegionStatistics::getStatDate, until);
        }
        if (region != null && region != SelectOption.RegionOption.all) {
            q.eq(DailyRegionStatistics::getRegion, region.name());
        }
        return q;
    }

    private String resolvePeriodFormat(SelectOption.GranularityOption granularity) {
        return switch (granularity) {
            case MONTH -> "YYYY-MM";
            case YEAR -> "YYYY";
            default -> "YYYY-MM-DD";
        };
    }

    private String resolveRegionLabel(String code) {
        for (SelectOption.RegionOption option : SelectOption.RegionOption.values()) {
            if (option.name().equals(code)) {
                return option.getLabel();
            }
        }
        return code;
    }
}
