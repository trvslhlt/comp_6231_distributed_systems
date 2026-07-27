package vehiclerental.seasonprice.web;

import java.util.Set;

public class InvalidSeasonException extends RuntimeException {

    public InvalidSeasonException(String season, Set<String> validSeasons) {
        super("Invalid season '%s', expected one of %s".formatted(season, validSeasons));
    }
}
