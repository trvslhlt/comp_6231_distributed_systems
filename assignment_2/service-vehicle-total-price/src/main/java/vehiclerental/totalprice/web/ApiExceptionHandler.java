package vehiclerental.totalprice.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import vehiclerental.common.dto.ErrorResponse;

/**
 * Global exception handler for the API.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(UpstreamNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(UpstreamNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse("not_found", e.getMessage()));
    }

    @ExceptionHandler({InvalidDaysException.class, UpstreamBadRequestException.class, MissingServletRequestParameterException.class})
    public ResponseEntity<ErrorResponse> handleBadRequest(Exception e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse("bad_request", e.getMessage()));
    }
}
