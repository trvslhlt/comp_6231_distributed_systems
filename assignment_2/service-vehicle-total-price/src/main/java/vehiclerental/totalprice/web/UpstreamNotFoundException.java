package vehiclerental.totalprice.web;

/**
 * Exception thrown when the upstream service does not return a price for the given vehicle type and season.
 */
public class UpstreamNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public UpstreamNotFoundException(String vehicleType, String season) {
        super("No price found for vehicleType='%s' season='%s'".formatted(vehicleType, season));
    }
}
