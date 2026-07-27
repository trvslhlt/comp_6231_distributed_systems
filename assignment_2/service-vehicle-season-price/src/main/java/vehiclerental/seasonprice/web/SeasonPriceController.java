package vehiclerental.seasonprice.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vehiclerental.common.dto.SeasonPriceResponse;
import vehiclerental.seasonprice.config.InstanceIdentity;
import vehiclerental.seasonprice.domain.PriceEntity;
import vehiclerental.seasonprice.domain.PriceRepository;

import java.util.Set;

@RestController
public class SeasonPriceController {

    private static final Set<String> VALID_SEASONS = Set.of("spring", "summer", "fall", "winter");

    private final PriceRepository priceRepository;
    private final InstanceIdentity instanceIdentity;

    public SeasonPriceController(PriceRepository priceRepository, InstanceIdentity instanceIdentity) {
        this.priceRepository = priceRepository;
        this.instanceIdentity = instanceIdentity;
    }

    @GetMapping("/price")
    public SeasonPriceResponse getPrice(@RequestParam String vehicleType, @RequestParam String season) {
        String normalizedSeason = season.trim().toLowerCase();
        if (!VALID_SEASONS.contains(normalizedSeason)) {
            throw new InvalidSeasonException(season, VALID_SEASONS);
        }

        PriceEntity price = priceRepository.findByVehicleTypeIgnoreCaseAndSeason(vehicleType, normalizedSeason)
                .orElseThrow(() -> new PriceNotFoundException(vehicleType, season));

        return new SeasonPriceResponse(
                price.getVehicleType(),
                price.getSeason(),
                price.getPricePerDay(),
                instanceIdentity.getInstanceId(),
                instanceIdentity.getPort()
        );
    }
}
