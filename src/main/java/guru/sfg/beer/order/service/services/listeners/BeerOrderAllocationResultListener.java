package guru.sfg.beer.order.service.services.listeners;

import com.springframework.brewery.model.events.AllocateOrderResult;
import guru.sfg.beer.order.service.services.BeerOrderManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import static guru.sfg.beer.order.service.config.JmsConfig.ALLOCATE_ORDER_RESPONSE;

@Component
@RequiredArgsConstructor
@Slf4j
public class BeerOrderAllocationResultListener {

    private final BeerOrderManager beerOrderManager;

    @JmsListener(destination = ALLOCATE_ORDER_RESPONSE)
    public void listen(AllocateOrderResult result){
        if(!result.getAllocationError() && !result.getPendingInventory()){
            beerOrderManager.beerOrderAllocationPassed(result.getBeerOrder());
        }else if(!result.getAllocationError() && result.getPendingInventory()){
            beerOrderManager.beerOrderAllocationPendingInventory(result.getBeerOrder());
        }else if(result.getAllocationError()){
            beerOrderManager.beerOrderAllocationFailed(result.getBeerOrder());
        }
    }
}
