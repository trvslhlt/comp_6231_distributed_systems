package vehiclerental.totalprice.web;

public class InvalidDaysException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public InvalidDaysException(int days) {
        super("Invalid days=%d, must be at least 1".formatted(days));
    }
}
