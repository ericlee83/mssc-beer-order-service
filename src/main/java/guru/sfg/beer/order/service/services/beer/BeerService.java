package guru.sfg.beer.order.service.services.beer;

import guru.sfg.beer.order.service.web.model.BeerOrderDto;
import guru.sfg.beer.order.service.web.model.beer.BeerDto;

import java.util.Optional;
import java.util.UUID;

public interface BeerService {

    Optional<BeerDto> findBeerById(UUID id);
    Optional<BeerDto> findBeerByUpc(String upc);
}
