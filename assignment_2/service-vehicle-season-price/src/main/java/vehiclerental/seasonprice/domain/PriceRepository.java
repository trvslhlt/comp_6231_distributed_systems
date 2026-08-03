package vehiclerental.seasonprice.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * Repository interface for price table queries.
 */
public interface PriceRepository extends JpaRepository<PriceEntity, PriceId> {

    @Query("SELECT p FROM PriceEntity p WHERE LOWER(p.vehicleType) = LOWER(:vehicleType) AND p.season = :season")
    Optional<PriceEntity> findByVehicleTypeIgnoreCaseAndSeason(
        @Param("vehicleType") String vehicleType, 
        @Param("season") String season
    );
}
