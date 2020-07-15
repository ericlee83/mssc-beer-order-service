package guru.sfg.beer.order.service.sm.actions;

import com.springframework.brewery.model.events.OrderEventEnum;
import com.springframework.brewery.model.events.OrderStatusEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;
import org.springframework.stereotype.Component;

import static guru.sfg.beer.order.service.services.BeerOrderManagerImpl.ORDER_ID_HEADER;

@Component
@Slf4j
@RequiredArgsConstructor
public class ValidationFailureAction implements Action<OrderStatusEnum, OrderEventEnum> {

    @Override
    public void execute(StateContext<OrderStatusEnum, OrderEventEnum> stateContext) {
        String beerOrderId = (String)stateContext.getMessage().getHeaders().get(ORDER_ID_HEADER);
        log.error("Compensating transaction ... validation failed: "+ beerOrderId);
    }
}
