package at.technikum.swendoc.document;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** Creates the bucket on first start so a fresh compose stack just works. */
@Component
@ConditionalOnProperty(name = "minio.init-bucket", matchIfMissing = true)
public class MinioBucketInitializer {

    private final MinioClient client;
    private final String bucket;

    public MinioBucketInitializer(MinioClient client, @Value("${minio.bucket}") String bucket) {
        this.client = client;
        this.bucket = bucket;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void createBucketIfMissing() throws Exception {
        if (!client.bucketExists(BucketExistsArgs.builder().bucket(bucket).build())) {
            client.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
        }
    }
}
