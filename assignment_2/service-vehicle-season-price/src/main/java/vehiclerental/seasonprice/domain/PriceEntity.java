package vehiclerental.seasonprice.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import java.math.BigDecimal;

/**
 * This class maps to the "prices" table in the database and represents a price entry.
 * It uses a composite primary key defined by the PriceId class.
 */
@Entity
@Table(name = "prices")
@IdClass(PriceId.class)
public class PriceEntity {

    @Id
    @Column(name = "vehicle_type")
    private String vehicleType;

    @Id
    @Column(name = "season", columnDefinition = "season")
    private String season;

    @Column(name = "price_per_day")
    private BigDecimal pricePerDay;

    protected PriceEntity() {}

    public String getVehicleType() {
        return vehicleType;
    }

    public String getSeason() {
        return season;
    }

    public BigDecimal getPricePerDay() {
        return pricePerDay;
    }
}
