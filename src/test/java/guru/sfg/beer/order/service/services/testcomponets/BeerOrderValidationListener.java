package guru.sfg.beer.order.service.services.testcomponets;

import com.springframework.brewery.model.events.ValidateOrderRequest;
import com.springframework.brewery.model.events.ValidateOrderResult;
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
public class BeerOrderValidationListener {

    private final JmsTemplate jmsTemplate;

    @JmsListener(destination = JmsConfig.VALIDATE_ORDER)
    public void listen(Message msg){
        Boolean isValid = true;
        Boolean sendResponse = true;

        ValidateOrderRequest request = (ValidateOrderRequest) msg.getPayload();

        if("validation-fail".equalsIgnoreCase(request.getBeerOrder().getCustomerRef())){
            isValid = false;
        }else if("dont-validate".equalsIgnoreCase(request.getBeerOrder().getCustomerRef())){
            sendResponse = false;
        }

        if(sendResponse)
            jmsTemplate.convertAndSend(JmsConfig.VALIDATE_ORDER_RESPONSE,
                ValidateOrderResult.builder().isValid(isValid).orderId(request.getBeerOrder().getId()).build());
    }
}
