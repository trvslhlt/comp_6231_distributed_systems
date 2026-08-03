package vehiclerental.totalprice.web;

import org.junit.jupiter.api.Test;
import vehiclerental.common.dto.SeasonPriceResponse;
import vehiclerental.common.dto.TotalPriceResponse;
import vehiclerental.totalprice.client.SeasonPriceClient;
import vehiclerental.common.config.InstanceIdentity;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TotalPriceControllerTest {

    // Real instances, not mocks: InstanceIdentity is a plain value object, and SeasonPriceClient
    // is a concrete class — mocking it needs Mockito's inline mock maker, which doesn't yet
    // support this project's build JDK (see the note in SeasonPriceClientTest). A hand-written
    // subclass overriding getPrice() sidesteps that entirely since it's just the plain Java
    // compiler, not bytecode generation at test time.
    private final InstanceIdentity instanceIdentity = new InstanceIdentity("total-price-1", 8080);

    @Test
    void multipliesUpstreamPricePerDayByDays() {
        SeasonPriceClient client = stubClient(new SeasonPriceResponse(
                new SeasonPriceResponse.Data("SUV", "summer", new BigDecimal("70.00")),
                new SeasonPriceResponse.Debug("season-price-2", 8081)
        ));
        TotalPriceController controller = new TotalPriceController(client, instanceIdentity);

        TotalPriceResponse response = controller.getTotal("SUV", "summer", 3);

        assertThat(response.data().totalPrice()).isEqualByComparingTo("210.00");
        assertThat(response.data().days()).isEqualTo(3);
        assertThat(response.data().pricePerDay()).isEqualByComparingTo("70.00");
        assertThat(response.debug().servedByInstanceId()).isEqualTo("total-price-1");
        assertThat(response.debug().servedByPort()).isEqualTo(8080);
        assertThat(response.debug().upstreamInstanceId()).isEqualTo("season-price-2");
    }

    @Test
    void zeroDaysThrowsInvalidDaysExceptionWithoutCallingUpstream() {
        SeasonPriceClient client = stubClient(new AssertionError("should not have called upstream"));
        TotalPriceController controller = new TotalPriceController(client, instanceIdentity);

        assertThatThrownBy(() -> controller.getTotal("SUV", "summer", 0))
                .isInstanceOf(InvalidDaysException.class)
                .hasMessageContaining("0");
    }

    @Test
    void negativeDaysThrowsInvalidDaysException() {
        SeasonPriceClient client = stubClient(new AssertionError("should not have called upstream"));
        TotalPriceController controller = new TotalPriceController(client, instanceIdentity);

        assertThatThrownBy(() -> controller.getTotal("SUV", "summer", -5))
                .isInstanceOf(InvalidDaysException.class);
    }

    @Test
    void upstreamNotFoundPropagatesToCaller() {
        SeasonPriceClient client = stubClientThrowing(new UpstreamNotFoundException("Spaceship", "summer"));
        TotalPriceController controller = new TotalPriceController(client, instanceIdentity);

        assertThatThrownBy(() -> controller.getTotal("Spaceship", "summer", 2))
                .isInstanceOf(UpstreamNotFoundException.class);
    }

    private static SeasonPriceClient stubClient(SeasonPriceResponse toReturn) {
        return new SeasonPriceClient(null) {
            @Override
            public SeasonPriceResponse getPrice(String vehicleType, String season) {
                return toReturn;
            }
        };
    }

    private static SeasonPriceClient stubClient(AssertionError ifCalled) {
        return new SeasonPriceClient(null) {
            @Override
            public SeasonPriceResponse getPrice(String vehicleType, String season) {
                throw ifCalled;
            }
        };
    }

    private static SeasonPriceClient stubClientThrowing(RuntimeException toThrow) {
        return new SeasonPriceClient(null) {
            @Override
            public SeasonPriceResponse getPrice(String vehicleType, String season) {
                throw toThrow;
            }
        };
    }
}
