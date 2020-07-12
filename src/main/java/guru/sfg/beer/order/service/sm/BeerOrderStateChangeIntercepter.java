package guru.sfg.beer.order.service.sm;

import com.springframework.brewery.model.events.OrderEventEnum;
import com.springframework.brewery.model.events.OrderStatusEnum;
import guru.sfg.beer.order.service.domain.BeerOrder;
import guru.sfg.beer.order.service.repositories.BeerOrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.state.State;
import org.springframework.statemachine.support.StateMachineInterceptorAdapter;
import org.springframework.statemachine.transition.Transition;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

import static guru.sfg.beer.order.service.services.BeerOrderManagerImpl.ORDER_ID_HEADER;

@Slf4j
@Component
@RequiredArgsConstructor
public class BeerOrderStateChangeIntercepter extends StateMachineInterceptorAdapter<OrderStatusEnum, OrderEventEnum> {

    private final BeerOrderRepository beerOrderRepository;

    @Override
    public void preStateChange(State<OrderStatusEnum, OrderEventEnum> state, Message<OrderEventEnum> message, Transition<OrderStatusEnum, OrderEventEnum> transition, StateMachine<OrderStatusEnum, OrderEventEnum> stateMachine) {
        Optional.ofNullable(message)
                .flatMap(msg-> Optional.ofNullable((String) msg.getHeaders().getOrDefault(ORDER_ID_HEADER,"")))
                .ifPresent(orderId->{
                    log.debug("Saving state for order id: "+orderId+" Status: "+state.getId());
                    BeerOrder beerOrder = beerOrderRepository.getOne(UUID.fromString(orderId));
                    beerOrder.setOrderStatus(state.getId());
                    beerOrderRepository.saveAndFlush(beerOrder);
                });
    }
}
