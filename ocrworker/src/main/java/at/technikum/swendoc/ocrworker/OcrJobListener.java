package at.technikum.swendoc.ocrworker;

import at.technikum.swendoc.messaging.OcrJobMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class OcrJobListener {

    private static final Logger log = LoggerFactory.getLogger(OcrJobListener.class);

    @RabbitListener(queues = RabbitMqConfig.OCR_QUEUE)
    public void onOcrJob(OcrJobMessage message) {
        log.info("Received OCR job: documentId={}, objectKey={}", message.documentId(), message.objectKey());
        // TODO: fetch the object from MinIO, run OCR and send the extracted text back
    }
}
