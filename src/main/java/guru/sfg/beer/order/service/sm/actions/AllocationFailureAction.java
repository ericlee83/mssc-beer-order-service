package guru.sfg.beer.order.service.sm.actions;

import com.springframework.brewery.model.events.AllocationFailureEvent;
import com.springframework.brewery.model.events.OrderEventEnum;
import com.springframework.brewery.model.events.OrderStatusEnum;
import guru.sfg.beer.order.service.config.JmsConfig;
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
public class AllocationFailureAction implements Action<OrderStatusEnum, OrderEventEnum> {

    private final JmsTemplate jmsTemplate;

    @Override
    public void execute(StateContext<OrderStatusEnum, OrderEventEnum> stateContext) {
        String beerOrderId = (String) stateContext.getMessage().getHeaders().get(ORDER_ID_HEADER);

        jmsTemplate.convertAndSend(JmsConfig.ALLOCATE_FAILURE_QUEUE, AllocationFailureEvent.builder().orderId(UUID.fromString(beerOrderId)).build());

        log.debug("Sent allocation failure request to queue for order id " + beerOrderId);


    }
}
