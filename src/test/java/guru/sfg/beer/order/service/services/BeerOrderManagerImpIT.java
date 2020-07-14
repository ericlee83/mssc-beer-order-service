package guru.sfg.beer.order.service.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.jenspiegsa.wiremockextension.WireMockExtension;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.springframework.brewery.model.beer.BeerDto;
import com.springframework.brewery.model.beer.BeerStyleEnum;
import com.springframework.brewery.model.events.OrderStatusEnum;
import guru.sfg.beer.order.service.domain.BeerOrder;
import guru.sfg.beer.order.service.domain.BeerOrderLine;
import guru.sfg.beer.order.service.domain.Customer;
import guru.sfg.beer.order.service.repositories.BeerOrderRepository;
import guru.sfg.beer.order.service.repositories.CustomerRepository;
import guru.sfg.beer.order.service.services.beer.BeerServiceRestTemplateImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static com.github.jenspiegsa.wiremockextension.ManagedWireMockServer.with;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(WireMockExtension.class)
@SpringBootTest
public class BeerOrderManagerImpIT {

    @Autowired
    WireMockServer wireMockServer;

    @Autowired
    BeerOrderManager beerOrderManager;

    @Autowired
    BeerOrderRepository beerOrderRepository;

    @Autowired
    CustomerRepository customerRepository;

    @Autowired
    ObjectMapper objectMapper;

    Customer testCustomer;

    UUID beerId = UUID.randomUUID();

    @TestConfiguration
    static class RestTemplateBuilderProvider{

        @Bean(destroyMethod = "stop")
        public WireMockServer wireMockServer(){
            WireMockServer wireMockServer = with(wireMockConfig().port(8083));
            wireMockServer.start();
            return wireMockServer;
        }
    }

    @BeforeEach
    void setUp() {
        testCustomer = customerRepository.save(Customer.builder()
                .customerName("Test Customer").build());
    }

    @Test
    void testNewToAllocated() throws JsonProcessingException, InterruptedException {
        BeerDto beerDto = BeerDto.builder().id(beerId).upc("0083783375213").beerStyle(BeerStyleEnum.PALE_ALE).build();
        wireMockServer.stubFor(get(BeerServiceRestTemplateImpl.BEER_UPC_PATH+"0083783375213")
                .willReturn(okJson(objectMapper.writeValueAsString(beerDto))));
        wireMockServer.stubFor(get(BeerServiceRestTemplateImpl.BEER_UPC_PATH+"0631234300019")
                .willReturn(okJson(objectMapper.writeValueAsString(beerDto))));
        wireMockServer.stubFor(get(BeerServiceRestTemplateImpl.BEER_UPC_PATH+"0631234200036")
                .willReturn(okJson(objectMapper.writeValueAsString(beerDto))));

        BeerOrder beerOrder = createBeerOrder();
        BeerOrder savedBeerOrder = beerOrderManager.newBeerOrder(beerOrder);

        await().untilAsserted(()->{
            BeerOrder foundBeerOrder = beerOrderRepository.findById(beerOrder.getId()).get();

            assertEquals(OrderStatusEnum.ALLOCATED,foundBeerOrder.getOrderStatus());
        });

        await().untilAsserted(()->{
            BeerOrder foundBeerOrder = beerOrderRepository.findById(beerOrder.getId()).get();
            BeerOrderLine line = foundBeerOrder.getBeerOrderLines().iterator().next();
            assertEquals(line.getOrderQuantity(),line.getQuantityAllocated());
        });

        BeerOrder savedBeerOrder2 = beerOrderRepository.findById(savedBeerOrder.getId()).get();
        assertNotNull(savedBeerOrder2);
        assertEquals(OrderStatusEnum.ALLOCATED,savedBeerOrder2.getOrderStatus());
        savedBeerOrder2.getBeerOrderLines().forEach(line->{
            assertEquals(line.getOrderQuantity(),line.getQuantityAllocated());
        });
    }

    @Test
    void testNewToPickedUp() throws JsonProcessingException {
        BeerDto beerDto = BeerDto.builder().id(beerId).upc("0083783375213").beerStyle(BeerStyleEnum.PALE_ALE).build();
        wireMockServer.stubFor(get(BeerServiceRestTemplateImpl.BEER_UPC_PATH+"0083783375213")
                .willReturn(okJson(objectMapper.writeValueAsString(beerDto))));
        wireMockServer.stubFor(get(BeerServiceRestTemplateImpl.BEER_UPC_PATH+"0631234300019")
                .willReturn(okJson(objectMapper.writeValueAsString(beerDto))));
        wireMockServer.stubFor(get(BeerServiceRestTemplateImpl.BEER_UPC_PATH+"0631234200036")
                .willReturn(okJson(objectMapper.writeValueAsString(beerDto))));

        BeerOrder beerOrder = createBeerOrder();
        BeerOrder savedBeerOrder = beerOrderManager.newBeerOrder(beerOrder);

        await().untilAsserted(()->{
            BeerOrder foundBeerOrder = beerOrderRepository.findById(beerOrder.getId()).get();

            assertEquals(OrderStatusEnum.ALLOCATED,foundBeerOrder.getOrderStatus());
        });

        beerOrderManager.beerOrderPickedUp(savedBeerOrder.getId());

        await().untilAsserted(()->{
            BeerOrder foundBeerOrder = beerOrderRepository.findById(beerOrder.getId()).get();

            assertEquals(OrderStatusEnum.PICKED_UP,foundBeerOrder.getOrderStatus());
        });
        BeerOrder pickedUpOrder = beerOrderRepository.findById(savedBeerOrder.getId()).get();
        assertNotNull(pickedUpOrder);
        assertEquals(OrderStatusEnum.PICKED_UP,pickedUpOrder.getOrderStatus());
    }

    public BeerOrder createBeerOrder(){
        BeerOrder beerOrder = BeerOrder.builder()
                .id(beerId)
                .customer(testCustomer)
                .build();
        Set<BeerOrderLine> lines = new HashSet<>();
        lines.add(BeerOrderLine.builder()
                                .beerId(beerId)
                                .upc("0083783375213")
                                .orderQuantity(1)
                                .beerOrder(beerOrder)
                                .build());
        beerOrder.setBeerOrderLines(lines);
        return beerOrder;
    }
}
