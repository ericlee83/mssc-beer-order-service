package guru.sfg.beer.order.service.services.beer;

import com.springframework.brewery.model.beer.BeerDto;

import java.util.Optional;
import java.util.UUID;

public interface BeerService {

    Optional<BeerDto> findBeerById(UUID id);
    Optional<BeerDto> findBeerByUpc(String upc);
}
