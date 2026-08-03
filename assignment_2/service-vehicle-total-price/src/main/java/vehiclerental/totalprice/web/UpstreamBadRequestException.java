package vehiclerental.totalprice.web;

/**
 * Exception thrown when the upstream service returns a bad request response.
 */
public class UpstreamBadRequestException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public UpstreamBadRequestException(String vehicleType, String season) {
        super("Invalid vehicleType='%s' or season='%s'".formatted(vehicleType, season));
    }
}
