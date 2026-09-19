package cn.gov.cma.hl.dxal.msp.lightningfire.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "统一响应体")
public record ResponseDTO<T>(
        @Schema(description = "状态码，200 表示成功", example = "200") int code,
        @Schema(description = "错误信息，成功时为 null") String message,
        @Schema(description = "业务数据") T data) {

    public static <T> ResponseDTO<T> success(T data) {
        return new ResponseDTO<>(200, null, data);
    }

    public static <T> ResponseDTO<T> failure(int code, String message) {
        return new ResponseDTO<>(code, message, null);
    }
}
