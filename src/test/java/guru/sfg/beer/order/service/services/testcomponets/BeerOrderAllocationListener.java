package guru.sfg.beer.order.service.services.testcomponets;

import com.springframework.brewery.model.events.AllocateOrderRequest;
import com.springframework.brewery.model.events.AllocateOrderResult;
import guru.sfg.beer.order.service.config.JmsConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class BeerOrderAllocationListener {

    private final JmsTemplate jmsTemplate;

    @JmsListener(destination = JmsConfig.ALLOCATE_ORDER)
    public void listen(Message msg){

        AllocateOrderRequest request = (AllocateOrderRequest) msg.getPayload();
        request.getBeerOrder().getBeerOrderLines().forEach(beerOrderLineDto -> {
            beerOrderLineDto.setQuantityAllocated(beerOrderLineDto.getOrderQuantity());
        });
        jmsTemplate.convertAndSend(JmsConfig.ALLOCATE_ORDER_RESPONSE,
                AllocateOrderResult.builder().beerOrder(request.getBeerOrder())
        .allocationError(false).pendingInventory(false).build());
    }
}
