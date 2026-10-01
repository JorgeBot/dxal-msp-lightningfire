package cn.gov.cma.hl.dxal.msp.lightningfire.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 交接包总索引 index.json 的产品定位：按评价年份、指标、方案找到对应的产品记录。
 *
 * <p>暴露度与脆弱性的统计接口原先经本组件取产品来源年份；现在两个接口的 sourceYear 直接取请求的评价年份，
 * 已不再调用本组件。类与缓存逻辑保留备用（如需按 index.json 定位产品、标注来源年份时可直接复用）。</p>
 *
 * <p>方案号只有综合暴露度 E、综合脆弱性 V 的产品记录才带（schemeId 传 null 表示不按方案过滤）；
 * 数据来源年份取自产品记录的 source_year，E、V 只声明了逐输入的 source_years，故返回 null。</p>
 *
 * <p>索引文件缺失或解析失败时返回 null，由调用方按「无该产品」处理，不抛异常中断接口。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IndicatorProductIndex {

    private final ObjectMapper objectMapper;

    /**
     * 交接包根目录：index.json 所在目录。
     */
    @Value("${file-root.risk-assessment}")
    private String indicatorDataRoot;

    /**
     * 总索引解析结果缓存，按 index.json 最后修改时间失效。
     */
    private volatile IndexCache indexCache;

    /**
     * 按年份、指标定位产品记录；schemeId 非空时再按方案号匹配。
     *
     * @param assessmentYear 评价年份
     * @param fieldId        指标标识（index.json 的 indicator，如 forest_fraction、E）
     * @param schemeId       AHP 方案号；null 表示该指标不分方案
     * @return 产品记录；总索引缺失或无匹配记录时返回 null
     */
    public JsonNode product(short assessmentYear, String fieldId, Short schemeId) {
        JsonNode products = products();
        if (products == null) {
            return null;
        }
        for (JsonNode product : products) {
            if (product.path("year").asInt() != assessmentYear) {
                continue;
            }
            if (!fieldId.equals(product.path("indicator").asText())) {
                continue;
            }
            if (schemeId != null && product.path("scheme_id").asInt() != schemeId) {
                continue;
            }
            return product;
        }
        return null;
    }

    /**
     * 数据来源年份：取自产品记录（如森林覆盖暴露度 2025 年沿用 2024 年源、坡度困难度各年共用 2025 年底表）。
     * 总索引缺失、无对应产品或未标注来源年份时返回 null。
     */
    public Short sourceYear(short assessmentYear, String fieldId, Short schemeId) {
        JsonNode product = product(assessmentYear, fieldId, schemeId);
        if (product == null || !product.path("source_year").canConvertToInt()) {
            return null;
        }
        return (short) product.path("source_year").asInt();
    }

    /**
     * index.json 的 products 数组，按文件最后修改时间缓存（索引约 520 KB，避免每次请求重新解析）。
     */
    public JsonNode products() {
        Path indexFile = Path.of(indicatorDataRoot).toAbsolutePath().normalize().resolve("index.json");
        try {
            if (!Files.isRegularFile(indexFile)) {
                return null;
            }
            long lastModified = Files.getLastModifiedTime(indexFile).toMillis();
            IndexCache cached = indexCache;
            if (cached != null && cached.lastModified() == lastModified) {
                return cached.products();
            }
            JsonNode products = objectMapper.readTree(indexFile.toFile()).path("products");
            if (!products.isArray()) {
                return null;
            }
            indexCache = new IndexCache(lastModified, products);
            return products;
        } catch (IOException e) {
            log.warn("读取总索引失败，数据来源年份按评价年份返回 null：{}", indexFile, e);
            return null;
        }
    }

    /**
     * 总索引缓存条目。
     */
    private record IndexCache(long lastModified, JsonNode products) {
    }
}
