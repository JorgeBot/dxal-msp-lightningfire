package cn.gov.cma.hl.dxal.msp.lightningfire.vo;

import java.util.List;

public record KeyElementSummaryVO(List<Article> articles, TimeDimChart timeDimChart, RegionDimChart regionDimChart) {

    public record Article(String label, String value, String unit) {
    }

    public record TimeDimChart(String[] xAxis, Integer[] yAxis, Integer maxAt, Integer maxValue) {
    }

    public record RegionDimChart(String[] xAxis, Integer[] yAxis) {
    }
}
