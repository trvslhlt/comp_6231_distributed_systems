package vehiclerental.seasonprice.domain;

import java.io.Serializable;
import java.util.Objects;

public class PriceId implements Serializable {

    private String vehicleType;
    private String season;

    public PriceId() {
    }

    public PriceId(String vehicleType, String season) {
        this.vehicleType = vehicleType;
        this.season = season;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PriceId priceId)) return false;
        return Objects.equals(vehicleType, priceId.vehicleType) && Objects.equals(season, priceId.season);
    }

    @Override
    public int hashCode() {
        return Objects.hash(vehicleType, season);
    }
}
