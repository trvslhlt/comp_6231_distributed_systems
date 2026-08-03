package vehiclerental.totalprice.web;

import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import vehiclerental.common.dto.ErrorResponse;

import java.lang.reflect.Method;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

class ApiExceptionHandlerTest {

    private final ApiExceptionHandler handler = new ApiExceptionHandler();

    @Test
    void upstreamNotFoundMapsTo404WithNotFoundErrorCode() {
        ResponseEntity<ErrorResponse> response =
                handler.handleNotFound(new UpstreamNotFoundException("Spaceship", "summer"));
        ErrorResponse body = Objects.requireNonNull(response.getBody());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(body.error()).isEqualTo("not_found");
        assertThat(body.message()).contains("Spaceship").contains("summer");
    }

    @Test
    void invalidDaysMapsTo400WithBadRequestErrorCode() {
        ResponseEntity<ErrorResponse> response = handler.handleBadRequest(new InvalidDaysException(0));
        ErrorResponse body = Objects.requireNonNull(response.getBody());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(body.error()).isEqualTo("bad_request");
        assertThat(body.message()).contains("0");
    }

    @Test
    void upstreamBadRequestAlsoMapsTo400() {
        ResponseEntity<ErrorResponse> response =
                handler.handleBadRequest(new UpstreamBadRequestException("SUV", "monsoon"));
        ErrorResponse body = Objects.requireNonNull(response.getBody());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(body.message()).contains("SUV").contains("monsoon");
    }

    @Test
    void missingRequestParameterAlsoMapsTo400() {
        ResponseEntity<ErrorResponse> response =
                handler.handleBadRequest(new MissingServletRequestParameterException("days", "int"));
        ErrorResponse body = Objects.requireNonNull(response.getBody());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(body.error()).isEqualTo("bad_request");
    }

    @Test
    void nonNumericDaysMapsTo400WithoutLeakingUpstreamDetails() throws NoSuchMethodException {
        Method getTotal = TotalPriceController.class.getMethod("getTotal", String.class, String.class, int.class);
        MethodParameter daysParam = new MethodParameter(getTotal, 2);
        MethodArgumentTypeMismatchException e = new MethodArgumentTypeMismatchException(
                "abc", int.class, "days", daysParam, new NumberFormatException("For input string: \"abc\""));

        ResponseEntity<ErrorResponse> response = handler.handleTypeMismatch(e);
        ErrorResponse body = Objects.requireNonNull(response.getBody());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(body.error()).isEqualTo("bad_request");
        assertThat(body.message()).contains("days").contains("abc");
    }
}
