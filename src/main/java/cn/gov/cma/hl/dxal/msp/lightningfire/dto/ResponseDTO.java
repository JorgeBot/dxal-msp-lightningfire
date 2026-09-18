package cn.gov.cma.hl.dxal.msp.lightningfire.dto;

public record ResponseDTO<T>(int code, String message, T data) {

    public static <T> ResponseDTO<T> success(T data) {
        return new ResponseDTO<>(200, null, data);
    }

    public static <T> ResponseDTO<T> failure(int code, String message) {
        return new ResponseDTO<>(code, message, null);
    }
}
