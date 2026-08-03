package vehiclerental.totalprice.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vehiclerental.common.dto.SeasonPriceResponse;
import vehiclerental.common.dto.TotalPriceResponse;
import vehiclerental.totalprice.client.SeasonPriceClient;
import vehiclerental.common.config.InstanceIdentity;

import java.math.BigDecimal;

/**
 * Controller for calculating the total price of renting a vehicle based on its type, season, and number of days.
 * The data is provided by the season price API.
 */
@RestController
public class TotalPriceController {

    private final SeasonPriceClient seasonPriceClient;
    private final InstanceIdentity instanceIdentity;

    public TotalPriceController(SeasonPriceClient seasonPriceClient, InstanceIdentity instanceIdentity) {
        this.seasonPriceClient = seasonPriceClient;
        this.instanceIdentity = instanceIdentity;
    }

    @GetMapping("/total")
    public TotalPriceResponse getTotal(
        @RequestParam String vehicleType, 
        @RequestParam String season, 
        @RequestParam int days
    ) {
        if (days < 1) {
            throw new InvalidDaysException(days);
        }

        SeasonPriceResponse seasonPrice = seasonPriceClient.getPrice(vehicleType, season);
        BigDecimal total = seasonPrice.data().pricePerDay().multiply(BigDecimal.valueOf(days));

        return new TotalPriceResponse(
                new TotalPriceResponse.Data(
                        seasonPrice.data().vehicleType(),
                        seasonPrice.data().season(),
                        days,
                        seasonPrice.data().pricePerDay(),
                        total
                ),
                new TotalPriceResponse.Debug(
                        instanceIdentity.getInstanceId(),
                        instanceIdentity.getPort(),
                        seasonPrice.debug().servedByInstanceId()
                )
        );
    }
}
