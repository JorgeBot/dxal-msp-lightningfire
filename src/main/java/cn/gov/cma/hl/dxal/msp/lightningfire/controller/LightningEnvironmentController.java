package cn.gov.cma.hl.dxal.msp.lightningfire.controller;

import cn.gov.cma.hl.dxal.msp.lightningfire.constant.SelectOption;
import cn.gov.cma.hl.dxal.msp.lightningfire.dto.ResponseDTO;
import cn.gov.cma.hl.dxal.msp.lightningfire.dto.lightningfeature.GridDTO;
import cn.gov.cma.hl.dxal.msp.lightningfire.dto.lightningfeature.RegionDimChartDTO;
import cn.gov.cma.hl.dxal.msp.lightningfire.dto.lightningfeature.TimeDimChartDTO;
import cn.gov.cma.hl.dxal.msp.lightningfire.dto.request.KeyElementSummaryDTO;
import cn.gov.cma.hl.dxal.msp.lightningfire.service.LightningFeatureService;
import cn.gov.cma.hl.dxal.msp.lightningfire.vo.KeyElementSummaryVO;
import cn.gov.cma.hl.dxal.msp.lightningfire.vo.SelectOptionVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

    @Operation(summary = "分析方法", description = "select 中的 option 元素")
    @GetMapping("/analysis-method/options")
    public ResponseDTO<SelectOptionVO[]> getAnalysisMethodOptions() {
        SelectOptionVO[] optionVOS = Arrays.stream(SelectOption.AnalysisMethodOption.values())
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
        RegionDimChartDTO regionDim = lightningFeatureService.regionDimensionChart(payload.since(), payload.until(), payload.region());

        KeyElementSummaryVO.TimeDimChart timeDimChart = new KeyElementSummaryVO.TimeDimChart(
                timeDim.series().toArray(String[]::new),
                timeDim.count().toArray(Integer[]::new),
                timeDim.maxAt(),
                timeDim.maxValue());
        KeyElementSummaryVO.RegionDimChart regionDimChart = new KeyElementSummaryVO.RegionDimChart(
                regionDim.region().toArray(String[]::new),
                regionDim.count().toArray(Integer[]::new));

        return ResponseDTO.success(new KeyElementSummaryVO(articles, timeDimChart, regionDimChart));
    }

}
