package vehiclerental.seasonprice.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import java.math.BigDecimal;

/**
 * Maps to the "prices" table, whose "season" column is a native Postgres ENUM (see
 * V1__init_schema.sql). The JDBC URL carries "stringtype=unspecified" so the pgjdbc driver
 * sends this field's value as an untyped string and Postgres coerces it to the enum column
 * itself, instead of requiring a Hibernate-side native enum type mapping.
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

    protected PriceEntity() {
    }

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
