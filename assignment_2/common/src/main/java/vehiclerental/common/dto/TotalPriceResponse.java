package vehiclerental.common.dto;

import java.math.BigDecimal;

public record TotalPriceResponse(
        String vehicleType,
        String season,
        int days,
        BigDecimal pricePerDay,
        BigDecimal totalPrice,
        String servedByInstanceId,
        int servedByPort,
        String upstreamInstanceId
) {
}
