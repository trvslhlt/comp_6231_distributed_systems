package vehiclerental.totalprice.web;

/**
 * Exception thrown when the number of days is invalid (less than 1).
 */
public class InvalidDaysException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public InvalidDaysException(int days) {
        super("Invalid days=%d, must be at least 1".formatted(days));
    }
}
