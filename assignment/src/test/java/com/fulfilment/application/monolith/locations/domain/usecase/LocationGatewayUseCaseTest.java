package com.fulfilment.application.monolith.locations.domain.usecase;

import com.fulfilment.application.monolith.locations.adapters.database.LocationRepository;
import com.fulfilment.application.monolith.locations.domain.models.Location;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@QuarkusTest
class LocationGatewayUseCaseTest {

  @InjectMock
  private LocationRepository locationRepository;

  @Inject
  private LocationGatewayUseCase locationGatewayUseCase;

  @ParameterizedTest
  @CsvSource({
      "AAAAAAAAAAAA-000",
      "AAA-000",
  })
  void resolveByIdentifier_WhenIdentifierIsValid_ThenDoCall(String identifier) {
    var location = new Location("AAA-000", 1, 1);
    when(locationRepository.resolveByIdentifier(any())).thenReturn(location);

    var result = locationGatewayUseCase.resolveByIdentifier(identifier);

    assertThat(result).isSameAs(location);
  }

  @ParameterizedTest
  @CsvSource({
      "AAAAAAAAAAAAA-000",
      "AAAAAAAAAAAA-0000",
      "123-AAA",
      "123-000",
      "AAA-AAA",
      "111-111",
      "AAA-00",
      "AA-000",
  })
  void resolveByIdentifier_WhenIdentifierIsNotValid_ThenThrow(String identifier) {
    assertThatThrownBy(() -> locationGatewayUseCase.resolveByIdentifier(identifier))
        .isInstanceOf(WebApplicationException.class)
        .hasMessage("Location should be in form PLACE-number, example: 'ZWOLLE-001'");

    verifyNoInteractions(locationRepository);
  }
}