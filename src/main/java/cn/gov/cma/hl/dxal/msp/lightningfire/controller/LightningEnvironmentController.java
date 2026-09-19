package cn.gov.cma.hl.dxal.msp.lightningfire.controller;

import cn.gov.cma.hl.dxal.msp.lightningfire.constant.SelectOption;
import cn.gov.cma.hl.dxal.msp.lightningfire.dto.ResponseDTO;
import cn.gov.cma.hl.dxal.msp.lightningfire.dto.lightningfeature.GridDTO;
import cn.gov.cma.hl.dxal.msp.lightningfire.dto.lightningfeature.TimeDimChartDTO;
import cn.gov.cma.hl.dxal.msp.lightningfire.dto.request.KeyElementSummaryDTO;
import cn.gov.cma.hl.dxal.msp.lightningfire.service.LightningFeatureService;
import cn.gov.cma.hl.dxal.msp.lightningfire.vo.CorrelationCoefficientVO;
import cn.gov.cma.hl.dxal.msp.lightningfire.vo.KeyElementSummaryVO;
import cn.gov.cma.hl.dxal.msp.lightningfire.vo.LeadingFactorVO;
import cn.gov.cma.hl.dxal.msp.lightningfire.vo.SelectOptionVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

/**
 * 雷击火环境
 */
@RestController
@Tag(name = "雷击火环境")
@RequestMapping("/ltg-env")
@RequiredArgsConstructor
public class LightningEnvironmentController {

    private final LightningFeatureService lightningFeatureService;

    @Operation(summary = "雷击火关键要素", description = "select 中的 option 元素")
    @GetMapping("/key-element/options")
    public ResponseDTO<SelectOptionVO[]> getKeyElementOptions() {
        SelectOptionVO[] optionVOS = Arrays.stream(SelectOption.KeyElementOption.values())
                .map(e -> new SelectOptionVO(e.getLabel(), e.name()))
                .toArray(SelectOptionVO[]::new);
        return ResponseDTO.success(optionVOS);
    }


    @Operation(summary = "分析区域", description = "select 中的 option 元素")
    @GetMapping("/region/options")
    public ResponseDTO<SelectOptionVO[]> getRegionOptions() {
        SelectOptionVO[] optionVOS = Arrays.stream(SelectOption.RegionOption.values())
                .map(e -> new SelectOptionVO(e.getLabel(), e.name()))
                .toArray(SelectOptionVO[]::new);
        return ResponseDTO.success(optionVOS);
    }

    @Operation(summary = "地闪特征统计概览")
    @PostMapping("/key-element-summary")
    public ResponseDTO<KeyElementSummaryVO> getKeyElementSummary(@RequestBody KeyElementSummaryDTO payload) {
        GridDTO grid = lightningFeatureService.lightningFeatureGrid(payload.since(), payload.until(), payload.region());
        List<KeyElementSummaryVO.Article> articles = List.of(
                new KeyElementSummaryVO.Article("地闪次数", String.valueOf(grid.recordCount()), "次"),
                new KeyElementSummaryVO.Article("地闪密度", String.valueOf(grid.avgDensity()), "次/km²"),
                new KeyElementSummaryVO.Article("地闪强度", String.valueOf(grid.avgAbsIntensity()), "强度值"),
                new KeyElementSummaryVO.Article("正负极性", String.valueOf(grid.avgPositiveRatioPer()), "%正闪")
        );

        TimeDimChartDTO timeDim = lightningFeatureService.timeDimensionChart(payload.since(), payload.until(), payload.region(), payload.granularity());

        KeyElementSummaryVO.TimeDimChart timeDimChart = new KeyElementSummaryVO.TimeDimChart(
                timeDim.series().toArray(String[]::new),
                timeDim.count().toArray(Integer[]::new),
                timeDim.maxAt(),
                timeDim.maxValue());

        return ResponseDTO.success(new KeyElementSummaryVO(articles, timeDimChart));
    }

    @Operation(summary = "相关系数条形图", description = "区间内各环境因子相关系数的均值，无数据记 0，正负使用不同的颜色")
    @GetMapping("/bar-chart/correlation-coefficient")
    public ResponseDTO<CorrelationCoefficientVO> getCorrelationCoefficientBarChart(@RequestParam("since") LocalDate since, @RequestParam("until") LocalDate until) {
        return ResponseDTO.success(lightningFeatureService.correlationCoefficientBarChart(since, until));
    }

    @Operation(summary = "主导因子条形图", description = "区间内各环境主导因子相关系数的均值，无数据记 0")
    @GetMapping("/bar-chart/leading-factor")
    public ResponseDTO<LeadingFactorVO> getLeadingFactorBarChart(@RequestParam("since") LocalDate since, @RequestParam("until") LocalDate until) {
        return ResponseDTO.success(lightningFeatureService.leadingFactorBarChart(since, until));
    }


}
