package vehiclerental.totalprice.web;

public class UpstreamNotFoundException extends RuntimeException {

    public UpstreamNotFoundException(String vehicleType, String season) {
        super("No price found for vehicleType='%s' season='%s'".formatted(vehicleType, season));
    }
}
