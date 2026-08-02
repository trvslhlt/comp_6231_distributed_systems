package vehiclerental.totalprice.client;

import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import vehiclerental.common.dto.SeasonPriceResponse;
import vehiclerental.totalprice.web.UpstreamBadRequestException;
import vehiclerental.totalprice.web.UpstreamNotFoundException;

/**
 * Interacts with the season price API.
 */
@Component
public class SeasonPriceClient {

    private final RestClient restClient;

    public SeasonPriceClient(RestClient seasonPriceRestClient) {
        this.restClient = seasonPriceRestClient;
    }

    public SeasonPriceResponse getPrice(String vehicleType, String season) {
        try {
            return restClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/price")
                            .queryParam("vehicleType", vehicleType)
                            .queryParam("season", season)
                            .build())
                    .retrieve()
                    .body(SeasonPriceResponse.class);
        } catch (HttpClientErrorException.NotFound e) {
            throw new UpstreamNotFoundException(vehicleType, season);
        } catch (HttpClientErrorException.BadRequest e) {
            throw new UpstreamBadRequestException(vehicleType, season);
        }
    }
}
