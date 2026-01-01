package ma.elhanchir.fileservice.service;

import io.minio.*;
import io.minio.errors.MinioException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.UUID;

import static ma.elhanchir.fileservice.utils.FileUtils.extractExtension;

@Slf4j
@Service
@ConditionalOnProperty(name = "file.storage.type", havingValue = "GED")
public class GedStorageService implements FileStorageService {

    private final MinioClient minioClient;
    private final StoredFileRepository repository;
    private final StoredFileMapper mapper;
    private static final String BUCKET = "documents";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM");

    public GedStorageService(StoredFileRepository repository,
                             StoredFileMapper mapper,
                             @Value("${file.ged.url}") String gedUrl,
                             @Value("${file.ged.username}") String username,
                             @Value("${file.ged.password}") String password) {
        this.repository = repository;
        this.mapper = mapper;
        try {
            this.minioClient = MinioClient.builder()
                    .endpoint(gedUrl)
                    .credentials(username, password)
                    .build();
            log.info("Client MinIO initialisé avec succès pour GED: {}", gedUrl);
        } catch (Exception e) {
            log.error("Erreur lors de l'initialisation du client MinIO", e);
            throw new IllegalStateException("Impossible d'initialiser le client MinIO pour GED", e);
        }
    }

    /*@PostConstruct
    private void initBucket() {
        try {
            boolean exists = minioClient.bucketExists(
                    BucketExistsArgs.builder().bucket(BUCKET).build()
            );
            if (!exists) {
                minioClient.makeBucket(
                        MakeBucketArgs.builder()
                                .bucket(BUCKET)
                                .build()
                );
                log.info("Bucket '{}' créé avec succès dans MinIO", BUCKET);
            } else {
                log.info("Bucket '{}' existe déjà dans MinIO", BUCKET);
            }
        } catch (Exception e) {
            log.error("Erreur lors de l'initialisation du bucket '{}'", BUCKET, e);
            throw new IllegalStateException("Impossible de créer ou vérifier le bucket " + BUCKET + " dans MinIO", e);
        }
    }*/
    private void ensureBucketExists() throws Exception {
        boolean exists = minioClient.bucketExists(
                BucketExistsArgs.builder().bucket(BUCKET).build()
        );
        if (!exists) {
            minioClient.makeBucket(
                    MakeBucketArgs.builder().bucket(BUCKET).build()
            );
        }
    }


    @Override
    public FileMetadata store(MultipartFile file) throws IOException {
        try {
            ensureBucketExists();
            String originalName = Objects.requireNonNull(file.getOriginalFilename(), 
                    "Le nom du fichier ne peut pas être null");
            String extension = extractExtension(originalName);
            String storedName = originalName + "_" + UUID.randomUUID() + "." + extension;

            // Organisation par date (yyyy/MM) comme dans FsStorageService
            String dateFolder = LocalDate.now().format(DATE_FORMATTER);
            String objectPath = dateFolder + "/" + storedName;

            log.debug("Upload du fichier '{}' vers MinIO: {}/{}", originalName, BUCKET, objectPath);

            // Upload vers MinIO
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(BUCKET)
                            .object(objectPath)
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );

            log.info("Fichier '{}' uploadé avec succès vers MinIO: {}/{}", originalName, BUCKET, objectPath);

            // Créer et sauvegarder les métadonnées en base de données
            StoredFile entity = StoredFile.builder()
                    .originalName(originalName)
                    .storedName(storedName)
                    .extension(extension)
                    .contentType(file.getContentType())
                    .size(file.getSize())
                    .storageType("GED")
                    .storagePath(BUCKET + "/" + objectPath) // Chemin complet dans MinIO
                    .uploadedAt(LocalDateTime.now())
                    .build();

            StoredFile saved = repository.save(entity);
            log.info("Métadonnées sauvegardées en base pour le fichier ID: {}", saved.getId());

            return mapper.toMetadata(saved);

        } catch (MinioException e) {
            log.error("Erreur MinIO lors de l'upload du fichier", e);
            throw new RuntimeException("Erreur lors de l'upload vers GED (MinIO): " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Erreur inattendue lors de l'upload du fichier", e);
            throw new RuntimeException("Erreur lors de l'upload vers GED: " + e.getMessage(), e);
        }
    }

    @Override
    public FileDataResponse load(String fileId) throws IOException {
        try {
            StoredFile file = repository.findById(Long.parseLong(fileId))
                    .orElseThrow(() -> {
                        log.warn("Fichier introuvable avec l'ID: {}", fileId);
                        return new RuntimeException("Fichier introuvable avec l'ID: " + fileId);
                    });

            // Récupérer le chemin complet depuis storagePath ou reconstruire depuis storedName
            String objectPath;
            if (file.getStoragePath() != null && file.getStoragePath().startsWith(BUCKET + "/")) {
                // Extraire le chemin depuis storagePath (format: bucket/yyyy/MM/filename)
                objectPath = file.getStoragePath().substring(BUCKET.length() + 1);
            } else {
                // Fallback: utiliser storedName directement (pour compatibilité avec anciens fichiers)
                log.warn("storagePath manquant pour le fichier ID: {}, utilisation de storedName", fileId);
                objectPath = file.getStoredName();
            }

            log.debug("Téléchargement du fichier depuis MinIO: {}/{}", BUCKET, objectPath);

            try (InputStream is = minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(BUCKET)
                            .object(objectPath)
                            .build())) {

                byte[] data = is.readAllBytes();
                log.info("Fichier ID: {} téléchargé avec succès depuis MinIO ({} bytes)", fileId, data.length);
                return new FileDataResponse(data, mapper.toMetadata(file));
            }

        } catch (MinioException e) {
            log.error("Erreur MinIO lors du téléchargement du fichier ID: {}", fileId, e);
            throw new RuntimeException("Erreur lors de la lecture depuis GED (MinIO): " + e.getMessage(), e);
        } catch (NumberFormatException e) {
            log.error("ID de fichier invalide: {}", fileId, e);
            throw new IllegalArgumentException("ID de fichier invalide: " + fileId, e);
        } catch (Exception e) {
            log.error("Erreur inattendue lors du téléchargement du fichier ID: {}", fileId, e);
            throw new RuntimeException("Erreur lors de la lecture depuis GED: " + e.getMessage(), e);
        }
    }
}
