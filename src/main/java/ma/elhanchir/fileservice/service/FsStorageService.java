package ma.elhanchir.fileservice.service;

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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.UUID;

import static ma.elhanchir.fileservice.utils.FileUtils.extractExtension;

@Service
@ConditionalOnProperty(name = "file.storage.type", havingValue = "FS")
public class FsStorageService implements FileStorageService {

    private final Path root;
    private final StoredFileRepository repository;
    private final StoredFileMapper mapper;

    public FsStorageService(
            @Value("${file.fs.upload-dir}") String uploadDir,
            StoredFileRepository repository,
            StoredFileMapper mapper) {
        this.root = Paths.get(uploadDir);
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public FileMetadata store(MultipartFile file) throws IOException {
        Files.createDirectories(root);

        String originalName = Objects.requireNonNull(file.getOriginalFilename());
        String extension = extractExtension(originalName);
        String storedName = originalName + "_"+ UUID.randomUUID() + "." + extension;


        // Sauvegarder le fichier avec un nom unique
        Path folder = root.resolve(LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM")));
        Files.createDirectories(folder);
        Path filePath = folder.resolve(storedName);

        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);


        // Créer et sauvegarder les métadonnées
        StoredFile entity = StoredFile.builder()
                .originalName(originalName)
                .storedName(storedName)
                .extension(extension)
                .contentType(file.getContentType())
                .size(file.getSize())
                .storageType("FS")
                .storagePath(filePath.toString())
                .uploadedAt(LocalDateTime.now())
                .build();

        repository.save(entity);

        return mapper.toMetadata(entity);
    }

    @Override
    public FileDataResponse load(String fileId) throws IOException {
        StoredFile file = repository.findById(Long.parseLong(fileId))
                .orElseThrow(() -> new RuntimeException("Fichier introuvable: " + fileId));

        byte[] data = Files.readAllBytes(Paths.get(file.getStoragePath()));
        return new FileDataResponse(data, mapper.toMetadata(file));
    }

}