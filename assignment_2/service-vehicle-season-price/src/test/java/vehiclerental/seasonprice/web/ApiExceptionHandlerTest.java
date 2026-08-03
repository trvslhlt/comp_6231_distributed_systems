package vehiclerental.seasonprice.web;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import vehiclerental.common.dto.ErrorResponse;

import java.util.Objects;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ApiExceptionHandlerTest {

    private final ApiExceptionHandler handler = new ApiExceptionHandler();

    @Test
    void priceNotFoundMapsTo404WithNotFoundErrorCode() {
        ResponseEntity<ErrorResponse> response =
                handler.handleNotFound(new PriceNotFoundException("Spaceship", "summer"));
        ErrorResponse body = Objects.requireNonNull(response.getBody());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(body.error()).isEqualTo("not_found");
        assertThat(body.message()).contains("Spaceship").contains("summer");
    }

    @Test
    void invalidSeasonMapsTo400WithBadRequestErrorCode() {
        ResponseEntity<ErrorResponse> response =
                handler.handleBadRequest(new InvalidSeasonException("monsoon", Set.of("summer", "winter")));
        ErrorResponse body = Objects.requireNonNull(response.getBody());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(body.error()).isEqualTo("bad_request");
        assertThat(body.message()).contains("monsoon");
    }

    @Test
    void missingRequestParameterAlsoMapsTo400() {
        ResponseEntity<ErrorResponse> response =
                handler.handleBadRequest(new MissingServletRequestParameterException("season", "String"));
        ErrorResponse body = Objects.requireNonNull(response.getBody());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(body.error()).isEqualTo("bad_request");
    }
}
