package guru.sfg.beer.order.service.sm;

import com.springframework.brewery.model.events.OrderEventEnum;
import com.springframework.brewery.model.events.OrderStatusEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.action.Action;
import org.springframework.statemachine.config.EnableStateMachineFactory;
import org.springframework.statemachine.config.StateMachineConfigurerAdapter;
import org.springframework.statemachine.config.builders.StateMachineStateConfigurer;
import org.springframework.statemachine.config.builders.StateMachineTransitionConfigurer;

import java.util.EnumSet;

@Configuration
@EnableStateMachineFactory
@RequiredArgsConstructor
public class BeerOrderStateMachineConfig extends StateMachineConfigurerAdapter<OrderStatusEnum, OrderEventEnum> {

    private final Action<OrderStatusEnum, OrderEventEnum> orderValidationAction;
    private final Action<OrderStatusEnum, OrderEventEnum> allocateOrderAction;
    private final Action<OrderStatusEnum, OrderEventEnum> validationFailureAction;
    private final Action<OrderStatusEnum, OrderEventEnum> allocationFailureAction;
    private final Action<OrderStatusEnum, OrderEventEnum> deallocateOrderAction;
    @Override
    public void configure(StateMachineStateConfigurer<OrderStatusEnum, OrderEventEnum> states) throws Exception {
        states.withStates()
                .initial(OrderStatusEnum.NEW)
                .states(EnumSet.allOf(OrderStatusEnum.class))
                .end(OrderStatusEnum.PICKED_UP)
                .end(OrderStatusEnum.DELIVERED)
                .end(OrderStatusEnum.ALLOCATION_EXCEPTION)
                .end(OrderStatusEnum.DELIVERY_EXCEPTION)
                .end(OrderStatusEnum.VALIDATION_EXCEPTION)
                .end(OrderStatusEnum.CANCELED);
    }

    @Override
    public void configure(StateMachineTransitionConfigurer<OrderStatusEnum, OrderEventEnum> transitions) throws Exception {
        transitions.withExternal().source(OrderStatusEnum.NEW).target(OrderStatusEnum.VALIDATION_PENDING).event(OrderEventEnum.VALIDATE_ORDER)
                .action(orderValidationAction)
                .and()
                .withExternal().source(OrderStatusEnum.VALIDATION_PENDING).target(OrderStatusEnum.VALIDATED).event(OrderEventEnum.VALIDATION_PASSED)
                .and()
                .withExternal().source(OrderStatusEnum.VALIDATION_PENDING).target(OrderStatusEnum.VALIDATION_EXCEPTION).event(OrderEventEnum.VALIDATION_FAILED)
                .action(validationFailureAction)
                .and()
                .withExternal().source(OrderStatusEnum.VALIDATION_PENDING).target(OrderStatusEnum.CANCELED).event(OrderEventEnum.CANCEL_ORDER)
                .and()
                .withExternal().source(OrderStatusEnum.VALIDATED).target(OrderStatusEnum.ALLOCATION_PENDING).event(OrderEventEnum.ALLOCATE_ORDER)
                .action(allocateOrderAction)
                .and()
                .withExternal().source(OrderStatusEnum.VALIDATED).target(OrderStatusEnum.CANCELED).event(OrderEventEnum.CANCEL_ORDER)
                .and()
                .withExternal().source(OrderStatusEnum.ALLOCATION_PENDING).target(OrderStatusEnum.VALIDATED).event(OrderEventEnum.ALLOCATION_SUCCESS)
                .and()
                .withExternal().source(OrderStatusEnum.ALLOCATION_PENDING).target(OrderStatusEnum.VALIDATION_EXCEPTION).event(OrderEventEnum.ALLOCATION_FAILED)
                .action(allocationFailureAction)
                .and()
                .withExternal().source(OrderStatusEnum.ALLOCATION_PENDING).target(OrderStatusEnum.PENDING_INVENTORY).event(OrderEventEnum.ALLOCATION_NO_INVENTORY)
                .and()
                .withExternal().source(OrderStatusEnum.ALLOCATION_PENDING).target(OrderStatusEnum.CANCELED).event(OrderEventEnum.CANCEL_ORDER)
                .and()
                .withExternal().source(OrderStatusEnum.ALLOCATED).target(OrderStatusEnum.PICKED_UP).event(OrderEventEnum.ORDER_PICKED_UP)
                .and()
                .withExternal().source(OrderStatusEnum.ALLOCATED).target(OrderStatusEnum.CANCELED).event(OrderEventEnum.CANCEL_ORDER)
                .action(deallocateOrderAction);
    }
}
