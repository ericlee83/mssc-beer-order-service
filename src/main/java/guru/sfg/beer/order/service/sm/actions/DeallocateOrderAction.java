package guru.sfg.beer.order.service.sm.actions;

import com.springframework.brewery.model.events.DeallocateOrderRequest;
import com.springframework.brewery.model.events.OrderEventEnum;
import com.springframework.brewery.model.events.OrderStatusEnum;
import guru.sfg.beer.order.service.domain.BeerOrder;
import guru.sfg.beer.order.service.repositories.BeerOrderRepository;
import guru.sfg.beer.order.service.web.mappers.BeerOrderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

import static guru.sfg.beer.order.service.config.JmsConfig.DEALLOCATE_ORDER;
import static guru.sfg.beer.order.service.services.BeerOrderManagerImpl.ORDER_ID_HEADER;

@Component
@Slf4j
@RequiredArgsConstructor
public class DeallocateOrderAction implements Action<OrderStatusEnum, OrderEventEnum> {
    private final BeerOrderRepository beerOrderRepository;
    private final BeerOrderMapper beerOrderMapper;
    private final JmsTemplate jmsTemplate;

    @Override
    public void execute(StateContext<OrderStatusEnum, OrderEventEnum> stateContext) {
        String beerOrderId = (String)stateContext.getMessage().getHeaders().get(ORDER_ID_HEADER);
        Optional<BeerOrder> beerOrderOptional = beerOrderRepository.findById(UUID.fromString(beerOrderId));

        beerOrderOptional.ifPresentOrElse(beerOrder -> {
            jmsTemplate.convertAndSend(DEALLOCATE_ORDER,
                    DeallocateOrderRequest.builder().beerOrder(beerOrderMapper.beerOrderToDto(beerOrder)).build());
            log.debug("Sent deallocate order request to queue for order id "+beerOrderId);
        },()-> log.error("Beer Order not found!"));
    }
}
