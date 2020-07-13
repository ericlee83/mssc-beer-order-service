package guru.sfg.beer.order.service.services;

import com.springframework.brewery.model.BeerOrderDto;
import com.springframework.brewery.model.events.OrderEventEnum;
import com.springframework.brewery.model.events.OrderStatusEnum;
import guru.sfg.beer.order.service.domain.BeerOrder;
import guru.sfg.beer.order.service.repositories.BeerOrderRepository;
import guru.sfg.beer.order.service.sm.BeerOrderStateChangeIntercepter;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.config.StateMachineFactory;
import org.springframework.statemachine.support.DefaultStateMachineContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BeerOrderManagerImpl implements BeerOrderManager {

    private final StateMachineFactory<OrderStatusEnum, OrderEventEnum> factory;
    private final BeerOrderRepository beerOrderRepository;
    private final BeerOrderStateChangeIntercepter beerOrderStateChangeIntercepter;

    public static final String ORDER_ID_HEADER = "ORDER_ID_HEADER";

    @Transactional
    @Override
    public BeerOrder newBeerOrder(BeerOrder beerOrder) {
        beerOrder.setId(null);
        beerOrder.setOrderStatus(OrderStatusEnum.NEW);
        BeerOrder savedBeerOrder = beerOrderRepository.save(beerOrder);
        sendBeerOrderEvent(savedBeerOrder,OrderEventEnum.VALIDATE_ORDER);
        return savedBeerOrder;
    }

    @Override
    public void processValidationResult(UUID beerOrderId, Boolean isValid) {
        BeerOrder beerOrder = beerOrderRepository.getOne(beerOrderId);
        if(isValid){
            sendBeerOrderEvent(beerOrder,OrderEventEnum.VALIDATION_PASSED);
            BeerOrder validatedOrder = beerOrderRepository.getOne(beerOrderId);
            sendBeerOrderEvent(validatedOrder,OrderEventEnum.ALLOCATE_ORDER);
        }else{
            sendBeerOrderEvent(beerOrder,OrderEventEnum.VALIDATION_FAILED);
        }
    }

    @Override
    public void beerOrderAllocationPassed(BeerOrderDto beerOrderDto) {
        BeerOrder beerOrder = beerOrderRepository.getOne(beerOrderDto.getId());
        sendBeerOrderEvent(beerOrder,OrderEventEnum.ALLOCATION_SUCCESS);
        updateAllocatedQty(beerOrderDto,beerOrder);
    }

    private void updateAllocatedQty(BeerOrderDto beerOrderDto, BeerOrder beerOrder) {
        BeerOrder allocatedOrder = beerOrderRepository.getOne(beerOrderDto.getId());
        allocatedOrder.getBeerOrderLines().forEach(orderLine->{
            beerOrderDto.getBeerOrderLines().forEach(beerOrderLineDto -> {
                if(orderLine.getId().equals(beerOrderDto.getId())){
                    orderLine.setQuantityAllocated(beerOrderLineDto.getQuantityAllocated());
                }
            });
        });
        beerOrderRepository.saveAndFlush(beerOrder);
    }

    @Override
    public void beerOrderAllocationPendingInventory(BeerOrderDto beerOrderDto) {
        BeerOrder beerOrder = beerOrderRepository.getOne(beerOrderDto.getId());
        sendBeerOrderEvent(beerOrder,OrderEventEnum.ALLOCATION_NO_INVENTORY);
        updateAllocatedQty(beerOrderDto,beerOrder);
    }

    @Override
    public void beerOrderAllocationFailed(BeerOrderDto beerOrderDto) {
        BeerOrder beerOrder = beerOrderRepository.getOne(beerOrderDto.getId());
        sendBeerOrderEvent(beerOrder,OrderEventEnum.ALLOCATION_FAILED);
    }

    private void sendBeerOrderEvent(BeerOrder beerOrder, OrderEventEnum eventEnum){
        StateMachine<OrderStatusEnum,OrderEventEnum> sm = build(beerOrder);
        Message msg = MessageBuilder.withPayload(eventEnum).setHeader(ORDER_ID_HEADER,beerOrder.getId()).build();

        sm.sendEvent(msg);
    }

    private StateMachine<OrderStatusEnum,OrderEventEnum> build(BeerOrder beerOrder){
        StateMachine<OrderStatusEnum,OrderEventEnum> sm = factory.getStateMachine(beerOrder.getId());
        sm.stop();

        sm.getStateMachineAccessor().doWithAllRegions(sma->{
            sma.addStateMachineInterceptor(beerOrderStateChangeIntercepter);
            sma.resetStateMachine(new DefaultStateMachineContext<>(beerOrder.getOrderStatus(),null,null,null));
        });

        sm.start();
        return sm;
    }
}
