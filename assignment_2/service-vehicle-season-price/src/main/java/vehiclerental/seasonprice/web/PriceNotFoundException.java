package vehiclerental.seasonprice.web;

/**
 * Exception thrown when a price is not found for a given vehicle type and season.
 */
public class PriceNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public PriceNotFoundException(String vehicleType, String season) {
        super("No price found for vehicleType='%s' season='%s'".formatted(vehicleType, season));
    }
}
