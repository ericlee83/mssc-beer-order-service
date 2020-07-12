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
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;
import org.springframework.stereotype.Component;

import java.util.Random;
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
        String beerId = (String)stateContext.getMessage().getHeaders().get(ORDER_ID_HEADER);
        BeerOrder beerOrder = beerOrderRepository.getOne(UUID.fromString(beerId));

        jmsTemplate.convertAndSend(JmsConfig.VALIDATE_ORDER, ValidateOrderRequest.builder().beerOrder(beerOrderMapper.beerOrderToDto(beerOrder)).build());


        log.debug("Sent valication requst to queue for order id "+beerId);
        if(new Random().nextInt(10) < 8 ){
            stateContext.getStateMachine().sendEvent(MessageBuilder.withPayload(OrderEventEnum.VALIDATION_PASSED)
                    .setHeader(ORDER_ID_HEADER,stateContext.getMessageHeader(ORDER_ID_HEADER)).build());
        }else{
            stateContext.getStateMachine().sendEvent(MessageBuilder.withPayload(OrderEventEnum.VALIDATION_FAILED)
                    .setHeader(ORDER_ID_HEADER,stateContext.getMessageHeader(ORDER_ID_HEADER)).build());
        }
    }
}
