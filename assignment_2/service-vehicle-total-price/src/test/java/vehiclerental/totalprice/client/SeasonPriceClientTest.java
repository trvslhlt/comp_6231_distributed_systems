package vehiclerental.totalprice.client;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import vehiclerental.common.dto.SeasonPriceResponse;
import vehiclerental.totalprice.web.UpstreamBadRequestException;
import vehiclerental.totalprice.web.UpstreamNotFoundException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Exercises SeasonPriceClient against a real RestClient bound to a MockRestServiceServer,
 * rather than mocking the class itself — SeasonPriceClient is concrete, and Mockito's
 * inline mock maker (needed for concrete classes) doesn't yet support this project's build
 * JDK (see the note in SeasonPriceControllerTest). This also exercises the real HTTP-status
 * to domain-exception mapping, which is the actual logic worth testing here.
 */
class SeasonPriceClientTest {

    private MockRestServiceServer server;
    private SeasonPriceClient client;

    private void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://season-price");
        server = MockRestServiceServer.bindTo(builder).build();
        client = new SeasonPriceClient(builder.build());
    }

    @Test
    void returnsDeserializedResponseOnSuccess() {
        setUp();
        server.expect(requestTo("http://season-price/price?vehicleType=SUV&season=summer"))
                .andRespond(withSuccess("""
                        {"data":{"vehicleType":"SUV","season":"summer","pricePerDay":70.00},
                         "debug":{"servedByInstanceId":"season-price-1","servedByPort":8081}}
                        """, MediaType.APPLICATION_JSON));

        SeasonPriceResponse response = client.getPrice("SUV", "summer");

        assertThat(response.data().vehicleType()).isEqualTo("SUV");
        assertThat(response.data().pricePerDay()).isEqualByComparingTo("70.00");
        assertThat(response.debug().servedByInstanceId()).isEqualTo("season-price-1");
        server.verify();
    }

    @Test
    void mapsUpstream404ToUpstreamNotFoundException() {
        setUp();
        server.expect(requestTo("http://season-price/price?vehicleType=Spaceship&season=summer"))
                .andRespond(withStatus(org.springframework.http.HttpStatus.NOT_FOUND));

        assertThatThrownBy(() -> client.getPrice("Spaceship", "summer"))
                .isInstanceOf(UpstreamNotFoundException.class)
                .hasMessageContaining("Spaceship")
                .hasMessageContaining("summer");
    }

    @Test
    void mapsUpstream400ToUpstreamBadRequestException() {
        setUp();
        server.expect(requestTo("http://season-price/price?vehicleType=SUV&season=monsoon"))
                .andRespond(withStatus(org.springframework.http.HttpStatus.BAD_REQUEST)
                        .body("{\"error\":\"bad_request\",\"message\":\"Invalid season\"}")
                        .contentType(MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.getPrice("SUV", "monsoon"))
                .isInstanceOf(UpstreamBadRequestException.class)
                .hasMessageContaining("Invalid season");
    }
}
