package cn.gov.cma.hl.dxal.msp.lightningfire.dto.request;

import cn.gov.cma.hl.dxal.msp.lightningfire.constant.SelectOption;

import java.time.LocalDate;

public record KeyElementSummaryDTO(LocalDate since, LocalDate until, SelectOption.RegionOption region,
                                   SelectOption.KeyElementOption keyElement,
                                   SelectOption.AnalysisMethodOption analysisMethod,
                                   SelectOption.GranularityOption granularity) {
}
