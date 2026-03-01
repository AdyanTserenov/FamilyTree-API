package com.project.familytree.tree.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.io.InputStream;
import java.time.Duration;

/**
 * Сервис для работы с Yandex Object Storage (S3-совместимый).
 * <p>
 * Ключи объектов (s3Key) имеют формат:
 *   trees/{treeId}/media/{uuid}.ext   — медиафайлы
 *   trees/{treeId}/avatars/{uuid}.ext — аватары персон
 */
@Service
public class S3Service {

    private static final Logger log = LoggerFactory.getLogger(S3Service.class);

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    @Value("${s3.bucket}")
    private String bucket;

    @Value("${s3.presigned-url-expiry-minutes:60}")
    private long presignedUrlExpiryMinutes;

    public S3Service(S3Client s3Client, S3Presigner s3Presigner) {
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
    }

    /**
     * Загрузить файл в S3.
     *
     * @param s3Key       ключ объекта в бакете (путь внутри бакета)
     * @param inputStream содержимое файла
     * @param contentType MIME-тип файла
     * @param fileSize    размер файла в байтах
     */
    public void upload(String s3Key, InputStream inputStream, String contentType, long fileSize) {
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucket)
                .key(s3Key)
                .contentType(contentType)
                .contentLength(fileSize)
                .build();

        s3Client.putObject(request, RequestBody.fromInputStream(inputStream, fileSize));
        log.info("Uploaded file to S3: s3://{}/{}", bucket, s3Key);
    }

    /**
     * Скачать файл из S3 как InputStream.
     *
     * @param s3Key ключ объекта в бакете
     * @return поток байт файла
     */
    public ResponseInputStream<GetObjectResponse> download(String s3Key) {
        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(bucket)
                .key(s3Key)
                .build();

        log.info("Downloading file from S3: s3://{}/{}", bucket, s3Key);
        return s3Client.getObject(request);
    }

    /**
     * Удалить файл из S3.
     *
     * @param s3Key ключ объекта в бакете
     */
    public void delete(String s3Key) {
        DeleteObjectRequest request = DeleteObjectRequest.builder()
                .bucket(bucket)
                .key(s3Key)
                .build();

        s3Client.deleteObject(request);
        log.info("Deleted file from S3: s3://{}/{}", bucket, s3Key);
    }

    /**
     * Сгенерировать presigned URL для временного доступа к файлу.
     * URL действителен в течение {@code s3.presigned-url-expiry-minutes} минут.
     *
     * @param s3Key ключ объекта в бакете
     * @return временный URL для скачивания файла
     */
    public String generatePresignedUrl(String s3Key) {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucket)
                .key(s3Key)
                .build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(presignedUrlExpiryMinutes))
                .getObjectRequest(getObjectRequest)
                .build();

        PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(presignRequest);
        String url = presignedRequest.url().toString();
        log.debug("Generated presigned URL for s3://{}/{}: expires in {} min", bucket, s3Key, presignedUrlExpiryMinutes);
        return url;
    }

    /**
     * Проверить существование объекта в S3.
     *
     * @param s3Key ключ объекта
     * @return true если объект существует
     */
    public boolean exists(String s3Key) {
        try {
            s3Client.headObject(HeadObjectRequest.builder()
                    .bucket(bucket)
                    .key(s3Key)
                    .build());
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        }
    }
}
