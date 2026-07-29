package vehiclerental.totalprice.web;

public class UpstreamBadRequestException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public UpstreamBadRequestException(String message) {
        super(message);
    }
}
