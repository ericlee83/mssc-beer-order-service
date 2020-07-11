package guru.sfg.beer.order.service.services.beer;

import guru.sfg.beer.order.service.web.model.beer.BeerDto;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@ConfigurationProperties(prefix = "sfg.brewery",ignoreUnknownFields = false)
public class BeerServiceRestTemplateImpl implements BeerService {

    private final String BEER_UPC_PATH = "/api/v1/beerupc/";
    private final String BEER_PATH = "/api/v1/beer/";

    private final RestTemplate restTemplate;

    private String beerHost;

    public BeerServiceRestTemplateImpl(RestTemplateBuilder restTemplateBuilder) {
        restTemplate = restTemplateBuilder.build();
    }


    @Override
    public Optional<BeerDto> findBeerById(UUID id) {
        return Optional.of(restTemplate.getForObject(beerHost+BEER_PATH+id,BeerDto.class));
    }

    @Override
    public Optional<BeerDto> findBeerByUpc(String upc) {
        return Optional.of(restTemplate.getForObject(beerHost+BEER_UPC_PATH+upc,BeerDto.class));
    }

    public void setBeerHost(String beerHost) {
        this.beerHost = beerHost;
    }
}
