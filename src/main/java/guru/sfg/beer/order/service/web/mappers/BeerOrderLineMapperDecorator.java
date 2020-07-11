package guru.sfg.beer.order.service.web.mappers;

import guru.sfg.beer.order.service.domain.BeerOrderLine;
import guru.sfg.beer.order.service.services.beer.BeerService;
import guru.sfg.beer.order.service.web.model.BeerOrderLineDto;
import guru.sfg.beer.order.service.web.model.beer.BeerDto;
import org.springframework.beans.factory.annotation.Autowired;

import javax.swing.text.html.Option;
import java.util.Optional;

public abstract class BeerOrderLineMapperDecorator implements BeerOrderLineMapper {

    private BeerOrderLineMapper mapper;
    private BeerService beerService;
    @Override
    public BeerOrderLineDto beerOrderLineToDto(BeerOrderLine line) {
        BeerOrderLineDto dto = mapper.beerOrderLineToDto(line);
        Optional<BeerDto> beerDtoOptional = beerService.findBeerByUpc(line.getUpc());
        beerDtoOptional.ifPresent(beerDto -> {
            dto.setBeerId(beerDto.getId());
            dto.setBeerName(beerDto.getBeerName());
            dto.setBeerStyle(beerDto.getBeerStyle().name());
            dto.setPrice(beerDto.getPrice());
        });
        return dto;
    }

    @Override
    public BeerOrderLine dtoToBeerOrderLine(BeerOrderLineDto dto) {
        return mapper.dtoToBeerOrderLine(dto);
    }

    @Autowired
    public void setMapper(BeerOrderLineMapper mapper) {
        this.mapper = mapper;
    }

    @Autowired
    public void setBeerService(BeerService beerService) {
        this.beerService = beerService;
    }
}
