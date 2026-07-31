package vehiclerental.common.dto;

import java.math.BigDecimal;

/** {@code data} is the data relevant to the client; {@code debug} is info 
 * about how the request was handled (not part of the API contract)
 * */
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
