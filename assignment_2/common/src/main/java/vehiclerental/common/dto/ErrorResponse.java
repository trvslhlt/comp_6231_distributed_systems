package vehiclerental.common.dto;

/** {@code error} is the error code; {@code message} is the error message. */
public record ErrorResponse(
    String error, 
    String message
) {}
