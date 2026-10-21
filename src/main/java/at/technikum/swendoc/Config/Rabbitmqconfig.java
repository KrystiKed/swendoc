package at.technikum.swendoc.Config;


import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Rabbitmqconfig {

    public static final String OCR_QUEUE = "ocr.jobs";

    @Bean
    public Queue ocrQueue() {
        return new Queue(OCR_QUEUE, true);

    }
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }

}
