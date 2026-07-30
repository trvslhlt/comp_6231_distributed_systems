package vehiclerental.common.dto;

import java.math.BigDecimal;

/** {@code data} is the data relevant to the client; {@code debug} is info about which
 * service instances handled the request. Useful for observing load
 * balancing but not part of the API contract. */
public record SeasonPriceResponse(Data data, Debug debug) {

    public record Data(
        String vehicleType, 
        String season, 
        BigDecimal pricePerDay
    ) {}

    public record Debug(
        String servedByInstanceId, 
        int servedByPort
    ) {}
}
