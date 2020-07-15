package guru.sfg.beer.order.service.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.support.converter.MappingJackson2MessageConverter;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.jms.support.converter.MessageType;

@Configuration
public class JmsConfig {
    public static final String VALIDATE_ORDER = "validate-order";
    public static final String VALIDATE_ORDER_RESPONSE = "validate_order_response";
    public static final String ALLOCATE_ORDER = "allocate_order";
    public static final String DEALLOCATE_ORDER = "deallocate_order";
    public static final String ALLOCATE_ORDER_RESPONSE = "allocate_order_response";
    public static final String ALLOCATE_FAILURE_QUEUE = "allocate_failure_queue";
    @Bean
    public MessageConverter messageConverter(ObjectMapper objectMapper){
        MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
        converter.setTargetType(MessageType.TEXT);
        converter.setTypeIdPropertyName("_type");
        converter.setObjectMapper(objectMapper);
        return converter;
    }
}
