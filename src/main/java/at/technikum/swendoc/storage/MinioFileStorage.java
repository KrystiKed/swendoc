package at.technikum.swendoc.storage;

import java.io.InputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;

@Component
public class MinioFileStorage implements FileStorage {

    private static final Logger log = LoggerFactory.getLogger(MinioFileStorage.class);

    private final MinioClient minio;
    private final String bucket;

    public MinioFileStorage(MinioClient minio, @Value("${minio.bucket}") String bucket) {
        this.minio = minio;
        this.bucket = bucket;
    }

    @Override
    public void store(String key, InputStream content, long size, String contentType) {
        try {
            minio.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(key)
                    .stream(content, size, -1)
                    .contentType(contentType)
                    .build());
            log.debug("Stored object {} ({} bytes) in bucket {}", key, size, bucket);
        } catch (Exception e) {
            throw new StorageException("Could not store " + key, e);
        }
    }

    @Override
    public InputStream load(String key) {
        try {
            return minio.getObject(GetObjectArgs.builder().bucket(bucket).object(key).build());
        } catch (Exception e) {
            throw new StorageException("Could not load " + key, e);
        }
    }

    @Override
    public void delete(String key) {
        try {
            minio.removeObject(RemoveObjectArgs.builder().bucket(bucket).object(key).build());
            log.debug("Removed object {} from bucket {}", key, bucket);
        } catch (Exception e) {
            throw new StorageException("Could not delete " + key, e);
        }
    }
}
