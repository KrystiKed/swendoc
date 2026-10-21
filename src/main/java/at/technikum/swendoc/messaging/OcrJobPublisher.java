package at.technikum.swendoc.messaging;

import at.technikum.swendoc.Config.Rabbitmqconfig;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class OcrJobPublisher {

    private final RabbitTemplate rabbitTemplate;

    public OcrJobPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publish(UUID documentId, String objectKey) {
        rabbitTemplate.convertAndSend(
                Rabbitmqconfig.OCR_QUEUE,
                new OcrJobMessage(documentId, objectKey)
        );
    }
}
