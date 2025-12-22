package ma.elhanchir.fileservice.service;

import io.minio.*;
import jakarta.annotation.PostConstruct;
import ma.elhanchir.fileservice.dto.FileDataResponse;
import ma.elhanchir.fileservice.dto.FileMetadata;
import ma.elhanchir.fileservice.entity.StoredFile;
import ma.elhanchir.fileservice.mapper.StoredFileMapper;
import ma.elhanchir.fileservice.repository.StoredFileRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.UUID;

import static ma.elhanchir.fileservice.utils.FileUtils.extractExtension;
//import static org.apache.commons.compress.utils.FileNameUtils.getExtension;

@Service
@ConditionalOnProperty(name = "file.storage.type", havingValue = "GED")
public class GedStorageService implements FileStorageService {

    private final MinioClient minioClient;
    private final StoredFileRepository repository;
    private final StoredFileMapper mapper;
    private static final String BUCKET = "documents";

    public GedStorageService(StoredFileRepository repository,
                             StoredFileMapper mapper,
                             @Value("${file.ged.url}") String gedUrl,
                             @Value("${file.ged.username}")String username,
                             @Value("${file.ged.password}")String password) {
        this.repository = repository;
        this.mapper = mapper;
        this.minioClient = MinioClient.builder()
                .endpoint(gedUrl)
                .credentials(username, password)
                .build();
    }
    @PostConstruct
    private void initBucket() {
        try {
            boolean exists = minioClient.bucketExists(
                    BucketExistsArgs.builder().bucket(BUCKET).build()
            );
            if (!exists) {
                minioClient.makeBucket(
                        MakeBucketArgs.builder().bucket(BUCKET).build()
                );
            }
        } catch (Exception e) {
            throw new IllegalStateException("Impossible de créer le bucket " + BUCKET, e);
        }
    }

    @Override
    public FileMetadata store(MultipartFile file) throws IOException {
        String objectName = UUID.randomUUID() + "_" + file.getOriginalFilename();

        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(BUCKET)
                            .object(objectName)
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );
        } catch (Exception e) {
            throw new RuntimeException("Erreur upload GED", e);
        }

        StoredFile entity = StoredFile.builder()
                .originalName(file.getOriginalFilename())
                .storedName(objectName)
                .extension(extractExtension(file.getOriginalFilename()))
                .contentType(file.getContentType())
                .size(file.getSize())
                .storageType("GED")
                .uploadedAt(LocalDateTime.now())
                .build();

        repository.save(entity);
        return mapper.toMetadata(entity);
    }

    @Override
    public FileDataResponse load(String fileId) throws IOException {
        StoredFile file = repository.findById(Long.parseLong(fileId))
                .orElseThrow(() -> new RuntimeException("Fichier introuvable"));

        try (InputStream is = minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(BUCKET)
                        .object(file.getStoredName())
                        .build())) {

            return new FileDataResponse(is.readAllBytes(), mapper.toMetadata(file));
        } catch (Exception e) {
            throw new RuntimeException("Erreur lecture GED", e);
        }
    }


}
