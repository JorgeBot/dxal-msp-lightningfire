package cn.gov.cma.hl.dxal.msp.lightningfire.controller;


import cn.gov.cma.hl.dxal.msp.lightningfire.dto.ResponseDTO;
import cn.gov.cma.hl.dxal.msp.lightningfire.service.RegionCodeService;
import cn.gov.cma.hl.dxal.msp.lightningfire.vo.SelectOptionVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 公共接口：各页面共用的区划码选项。
 *
 * <p>本类只做 HTTP 出入参，区划集合、value 格式与校验规则都在
 * {@link RegionCodeService}，其它模块（下拉、按码过滤数据）直接复用该服务，不要另起一份。</p>
 */
@RestController
@Tag(name = "公共接口")
@RequestMapping("/common")
@RequiredArgsConstructor
@CrossOrigin("*")
public class CommonController {

    private final RegionCodeService regionCodeService;

    /**
     * 返回对应 code 下的县区，如果 code 为空则返回全省全部县区。
     *
     * @param code 区域编码，取值说明见 {@link RegionCodeService#CODE_DESC}
     */
    @Operation(summary = "区划码·县区选项 【OPTIONS】", description = """
            select 选项，value 为 6 位区划码字符串，可直接作为各接口 regionCode 参数取值；\
            code 为空或 230000 返回全省全部县区，code 为地市码返回该地市所辖县区，code 为县区码返回其本身""")
    @GetMapping("/region-code")
    public ResponseDTO<SelectOptionVO[]> getRegionCodeOptions(
            @Parameter(description = RegionCodeService.CODE_DESC, example = "230100")
            @RequestParam(value = "code", required = false) Integer code) {
        return ResponseDTO.success(regionCodeService.options(code));
    }
}
