package guru.sfg.beer.order.service.sm.actions;

import com.springframework.brewery.model.events.OrderEventEnum;
import com.springframework.brewery.model.events.OrderStatusEnum;
import com.springframework.brewery.model.events.ValidateOrderRequest;
import guru.sfg.beer.order.service.config.JmsConfig;
import guru.sfg.beer.order.service.domain.BeerOrder;
import guru.sfg.beer.order.service.repositories.BeerOrderRepository;
import guru.sfg.beer.order.service.web.mappers.BeerOrderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;
import org.springframework.stereotype.Component;

import java.util.UUID;

import static guru.sfg.beer.order.service.services.BeerOrderManagerImpl.ORDER_ID_HEADER;

@Component
@Slf4j
@RequiredArgsConstructor
public class OrderValidationAction implements Action<OrderStatusEnum, OrderEventEnum> {

    private final BeerOrderRepository beerOrderRepository;
    private final BeerOrderMapper beerOrderMapper;
    private final JmsTemplate jmsTemplate;

    @Override
    public void execute(StateContext<OrderStatusEnum, OrderEventEnum> stateContext) {
        String beerId = ((UUID)stateContext.getMessage().getHeaders().get(ORDER_ID_HEADER)).toString();
        BeerOrder beerOrder = beerOrderRepository.getOne(UUID.fromString(beerId));

        jmsTemplate.convertAndSend(JmsConfig.VALIDATE_ORDER, ValidateOrderRequest.builder().beerOrder(beerOrderMapper.beerOrderToDto(beerOrder)).build());


        log.debug("Sent validation request to queue for order id "+beerId);

    }
}
