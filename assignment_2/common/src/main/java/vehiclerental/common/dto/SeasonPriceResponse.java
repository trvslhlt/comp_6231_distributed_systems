package vehiclerental.common.dto;

import java.math.BigDecimal;

public record SeasonPriceResponse(
        String vehicleType,
        String season,
        BigDecimal pricePerDay,
        String servedByInstanceId,
        int servedByPort
) {
}
