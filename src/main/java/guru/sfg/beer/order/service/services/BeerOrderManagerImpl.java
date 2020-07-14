package guru.sfg.beer.order.service.services;

import com.springframework.brewery.model.BeerOrderDto;
import com.springframework.brewery.model.events.OrderEventEnum;
import com.springframework.brewery.model.events.OrderStatusEnum;
import guru.sfg.beer.order.service.domain.BeerOrder;
import guru.sfg.beer.order.service.repositories.BeerOrderRepository;
import guru.sfg.beer.order.service.sm.BeerOrderStateChangeIntercepter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.config.StateMachineFactory;
import org.springframework.statemachine.support.DefaultStateMachineContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
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

    @Transactional
    @Override
    public void processValidationResult(UUID beerOrderId, Boolean isValid) {
        log.debug("Process Validation Result for beerOrderId: " + beerOrderId + " Valid? " + isValid);
        Optional<BeerOrder> beerOrderOp = beerOrderRepository.findById(beerOrderId);
        beerOrderOp.ifPresentOrElse(beerOrder -> {
            if(isValid){
                sendBeerOrderEvent(beerOrder,OrderEventEnum.VALIDATION_PASSED);
                BeerOrder validatedOrder = beerOrderRepository.findById(beerOrderId).get();
                sendBeerOrderEvent(validatedOrder,OrderEventEnum.ALLOCATE_ORDER);
            }else{
                sendBeerOrderEvent(beerOrder,OrderEventEnum.VALIDATION_FAILED);
            }
        },()-> log.error("Order not found. Id: "+beerOrderId));

    }

    @Override
    public void beerOrderAllocationPassed(BeerOrderDto beerOrderDto) {
        Optional<BeerOrder> beerOrderOptional = beerOrderRepository.findById(beerOrderDto.getId());
        beerOrderOptional.ifPresentOrElse(beerOrder -> {
            sendBeerOrderEvent(beerOrder,OrderEventEnum.ALLOCATION_SUCCESS);
            updateAllocatedQty(beerOrderDto);
        },()->log.error("Order not found [allocation pass] id: "+beerOrderDto.getId()));

    }

    private void updateAllocatedQty(BeerOrderDto beerOrderDto) {
        Optional<BeerOrder> allocatedOrderOptional = beerOrderRepository.findById(beerOrderDto.getId());
        allocatedOrderOptional.ifPresentOrElse(allocatedOrder->{
            allocatedOrder.getBeerOrderLines().forEach(orderLine->{
                beerOrderDto.getBeerOrderLines().forEach(beerOrderLineDto -> {
                    if(orderLine.getId().equals(beerOrderDto.getId())){
                        orderLine.setQuantityAllocated(beerOrderLineDto.getQuantityAllocated());
                    }
                });
            });
            beerOrderRepository.saveAndFlush(allocatedOrder);
        },()->log.error("Order not found [update qty] id: "+beerOrderDto.getId()));

    }

    @Override
    public void beerOrderAllocationPendingInventory(BeerOrderDto beerOrderDto) {
        Optional<BeerOrder> beerOrderOptional = beerOrderRepository.findById(beerOrderDto.getId());
        beerOrderOptional.ifPresentOrElse(beerOrder -> {
            sendBeerOrderEvent(beerOrder,OrderEventEnum.ALLOCATION_NO_INVENTORY);
            updateAllocatedQty(beerOrderDto);
        },()->log.error("Order not found [pending inventory] id: "+beerOrderDto.getId()));

    }

    @Override
    public void beerOrderAllocationFailed(BeerOrderDto beerOrderDto) {
        Optional<BeerOrder> beerOrderOptional = beerOrderRepository.findById(beerOrderDto.getId());
        beerOrderOptional.ifPresentOrElse(beerOrder -> {
            sendBeerOrderEvent(beerOrder,OrderEventEnum.ALLOCATION_FAILED);
        },()->log.error("Order not found [allocation fail] id: "+beerOrderDto.getId()));

    }

    @Override
    public void beerOrderPickedUp(UUID id) {
        Optional<BeerOrder> beerOrderOptional = beerOrderRepository.findById(id);
        beerOrderOptional.ifPresentOrElse(beerOrder -> {
            sendBeerOrderEvent(beerOrder,OrderEventEnum.ORDER_PICKED_UP);
        },()->log.error("Order not found [allocation fail] id: "+id));
    }

    private void sendBeerOrderEvent(BeerOrder beerOrder, OrderEventEnum eventEnum){
        StateMachine<OrderStatusEnum,OrderEventEnum> sm = build(beerOrder);
        Message msg = MessageBuilder
                .withPayload(eventEnum)
                .setHeader(ORDER_ID_HEADER,beerOrder.getId().toString())
                .build();

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
