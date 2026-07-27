package vehiclerental.totalprice.web;

public class InvalidDaysException extends RuntimeException {

    public InvalidDaysException(int days) {
        super("Invalid days=%d, must be at least 1".formatted(days));
    }
}
