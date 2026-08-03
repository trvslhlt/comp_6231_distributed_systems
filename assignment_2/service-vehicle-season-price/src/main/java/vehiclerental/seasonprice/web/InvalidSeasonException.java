package vehiclerental.seasonprice.web;

import java.util.Set;

/**
 * Exception thrown when an invalid season is provided.
 */
public class InvalidSeasonException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public InvalidSeasonException(String season, Set<String> validSeasons) {
        super("Invalid season '%s', expected one of %s".formatted(season, validSeasons));
    }
}
