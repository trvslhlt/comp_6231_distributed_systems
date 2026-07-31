package vehiclerental.seasonprice.web;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vehiclerental.common.dto.SeasonPriceResponse;
import vehiclerental.seasonprice.config.InstanceIdentity;
import vehiclerental.seasonprice.domain.PriceEntity;
import vehiclerental.seasonprice.domain.PriceRepository;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SeasonPriceControllerTest {

    // Real instance, not a mock: InstanceIdentity is a plain value object with a public
    // constructor, and mocking concrete classes needs Mockito's inline/bytebuddy mock maker,
    // which at this Spring Boot version doesn't yet support newer JDKs (fails with "Java 25 is
    // not supported by the current version of Byte Buddy" on this project's build JDK).
    // PriceRepository is an interface, so mocking it doesn't hit that limitation.
    private final InstanceIdentity instanceIdentity = new InstanceIdentity("season-price-1", 8081);

    @Mock
    private PriceRepository priceRepository;

    @Test
    void returnsPriceForKnownVehicleTypeAndSeason() {
        PriceEntity price = priceEntity("SUV", "summer", "70.00");
        when(priceRepository.findByVehicleTypeIgnoreCaseAndSeason("SUV", "summer"))
                .thenReturn(Optional.of(price));

        SeasonPriceController controller = new SeasonPriceController(priceRepository, instanceIdentity);
        SeasonPriceResponse response = controller.getPrice("SUV", "Summer");

        assertThat(response.data().vehicleType()).isEqualTo("SUV");
        assertThat(response.data().season()).isEqualTo("summer");
        assertThat(response.data().pricePerDay()).isEqualByComparingTo("70.00");
        assertThat(response.debug().servedByInstanceId()).isEqualTo("season-price-1");
        assertThat(response.debug().servedByPort()).isEqualTo(8081);
    }

    @Test
    void seasonIsNormalizedToLowercaseBeforeLookup() {
        when(priceRepository.findByVehicleTypeIgnoreCaseAndSeason(eq("SUV"), eq("summer")))
                .thenReturn(Optional.of(priceEntity("SUV", "summer", "70.00")));

        SeasonPriceController controller = new SeasonPriceController(priceRepository, instanceIdentity);
        controller.getPrice("SUV", "  SUMMER  ");

        verify(priceRepository).findByVehicleTypeIgnoreCaseAndSeason("SUV", "summer");
    }

    @Test
    void unknownSeasonThrowsInvalidSeasonExceptionWithoutQueryingRepository() {
        SeasonPriceController controller = new SeasonPriceController(priceRepository, instanceIdentity);

        assertThatThrownBy(() -> controller.getPrice("SUV", "monsoon"))
                .isInstanceOf(InvalidSeasonException.class)
                .hasMessageContaining("monsoon");

        verify(priceRepository, never())
                .findByVehicleTypeIgnoreCaseAndSeason(any(), any());
    }

    @Test
    void missingPriceThrowsPriceNotFoundException() {
        when(priceRepository.findByVehicleTypeIgnoreCaseAndSeason("Spaceship", "summer"))
                .thenReturn(Optional.empty());

        SeasonPriceController controller = new SeasonPriceController(priceRepository, instanceIdentity);

        assertThatThrownBy(() -> controller.getPrice("Spaceship", "summer"))
                .isInstanceOf(PriceNotFoundException.class)
                .hasMessageContaining("Spaceship")
                .hasMessageContaining("summer");
    }

    // PriceEntity has no public constructor or setters (it's a JPA-managed read model), so
    // tests build one via reflection rather than adding test-only production API surface.
    private static PriceEntity priceEntity(String vehicleType, String season, String pricePerDay) {
        try {
            var constructor = PriceEntity.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            PriceEntity entity = constructor.newInstance();
            setField(entity, "vehicleType", vehicleType);
            setField(entity, "season", season);
            setField(entity, "pricePerDay", new BigDecimal(pricePerDay));
            return entity;
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    private static void setField(Object target, String fieldName, Object value) {
        try {
            var field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}
