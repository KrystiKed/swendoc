package at.technikum.swendoc.storage;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import java.io.IOException;
import java.io.InputStream;
import org.junit.jupiter.api.Test;

class MinioFileStorageTest {

    private final MinioClient minio = mock(MinioClient.class);
    private final FileStorage storage = new MinioFileStorage(minio, "documents");

    @Test
    void minioFailuresBecomeStorageExceptionsWithTheCause() throws Exception {
        IOException down = new IOException("connection refused");
        when(minio.putObject(any(PutObjectArgs.class))).thenThrow(down);
        when(minio.getObject(any(GetObjectArgs.class))).thenThrow(down);
        doThrow(down).when(minio).removeObject(any(RemoveObjectArgs.class));

        assertSame(down, assertThrows(StorageException.class,
                () -> storage.store("key", InputStream.nullInputStream(), 0, "text/plain")).getCause());
        assertSame(down, assertThrows(StorageException.class, () -> storage.load("key")).getCause());
        assertSame(down, assertThrows(StorageException.class, () -> storage.delete("key")).getCause());
    }
}
