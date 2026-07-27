package vehiclerental.seasonprice.web;

public class PriceNotFoundException extends RuntimeException {

    public PriceNotFoundException(String vehicleType, String season) {
        super("No price found for vehicleType='%s' season='%s'".formatted(vehicleType, season));
    }
}
