package guru.sfg.beer.order.service.sm;

import com.springframework.brewery.model.events.OrderEventEnum;
import com.springframework.brewery.model.events.OrderStatusEnum;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.config.EnableStateMachineFactory;
import org.springframework.statemachine.config.StateMachineConfigurerAdapter;
import org.springframework.statemachine.config.builders.StateMachineStateConfigurer;
import org.springframework.statemachine.config.builders.StateMachineTransitionConfigurer;

import java.util.EnumSet;

@Configuration
@EnableStateMachineFactory
public class BeerOrderStateMachineConfig extends StateMachineConfigurerAdapter<OrderStatusEnum, OrderEventEnum> {
    @Override
    public void configure(StateMachineStateConfigurer<OrderStatusEnum, OrderEventEnum> states) throws Exception {
        states.withStates()
                .initial(OrderStatusEnum.NEW)
                .states(EnumSet.allOf(OrderStatusEnum.class))
                .end(OrderStatusEnum.PICKED_UP)
                .end(OrderStatusEnum.DELIVERED)
                .end(OrderStatusEnum.ALLOCATION_EXCEPTION)
                .end(OrderStatusEnum.DELIVERY_EXCEPTION)
                .end(OrderStatusEnum.VALIDATION_EXCEPTION);
    }

    @Override
    public void configure(StateMachineTransitionConfigurer<OrderStatusEnum, OrderEventEnum> transitions) throws Exception {
        transitions.withExternal().source(OrderStatusEnum.NEW).target(OrderStatusEnum.NEW).event(OrderEventEnum.VALIDATE_ORDER)
                .and()
                .withExternal().source(OrderStatusEnum.NEW).target(OrderStatusEnum.VALIDATED).event(OrderEventEnum.VALIDATION_PASSED)
                .and()
                .withExternal().source(OrderStatusEnum.NEW).target(OrderStatusEnum.VALIDATION_EXCEPTION).event(OrderEventEnum.VALIDATION_FAILED);
    }
}
