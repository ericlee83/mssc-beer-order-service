package guru.sfg.beer.order.service.services.beer;

import com.springframework.brewery.model.beer.BeerDto;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

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
