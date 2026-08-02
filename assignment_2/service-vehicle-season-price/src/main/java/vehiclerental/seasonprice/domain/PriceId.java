package vehiclerental.seasonprice.domain;

import java.io.Serializable;
import java.util.Objects;

/**
 * A unique identifier for a price entry based on vehicle type and season.
 */
public class PriceId implements Serializable {

    // serialVersionUID is used for version control in a Serializable class.
    private static final long serialVersionUID = 1L;

    private String vehicleType;
    private String season;

    public PriceId() {}

    public PriceId(String vehicleType, String season) {
        this.vehicleType = vehicleType;
        this.season = season;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PriceId priceId)) return false; // type check, case, and binding
        return Objects.equals(vehicleType, priceId.vehicleType) && Objects.equals(season, priceId.season);
    }

    @Override
    public int hashCode() {
        return Objects.hash(vehicleType, season);
    }
}
