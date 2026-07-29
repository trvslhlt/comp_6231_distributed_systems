package vehiclerental.totalprice.web;

public class UpstreamNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public UpstreamNotFoundException(String vehicleType, String season) {
        super("No price found for vehicleType='%s' season='%s'".formatted(vehicleType, season));
    }
}
