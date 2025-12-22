package ma.elhanchir.fileservice.service;

import lombok.AllArgsConstructor;
import ma.elhanchir.fileservice.dto.FileDataResponse;
import ma.elhanchir.fileservice.dto.FileMetadata;
import ma.elhanchir.fileservice.entity.StoredFile;
import ma.elhanchir.fileservice.mapper.StoredFileMapper;
import ma.elhanchir.fileservice.repository.StoredFileRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

import static ma.elhanchir.fileservice.utils.FileUtils.extractExtension;

@Service
@AllArgsConstructor
@ConditionalOnProperty(name = "file.storage.type", havingValue = "DB")
public class DbStorageService implements FileStorageService {

    private final StoredFileRepository repository;
    private final StoredFileMapper mapper;

    @Override
    public FileMetadata store(MultipartFile file) throws IOException {
        String originalName = file.getOriginalFilename();
        String extension = extractExtension(originalName);
        String storedName = originalName + "_"+ UUID.randomUUID() + "." + extension;

        StoredFile storedFile = StoredFile.builder()
                .originalName(originalName)
                .extension(extension)
                .storedName(storedName)
                .contentType(file.getContentType())
                .size(file.getSize())
                .storageType("DB")
                .data(file.getBytes())
                .build();

        StoredFile saved = repository.save(storedFile);

        return mapper.toMetadata(storedFile);
    }

    @Override
    public FileDataResponse load(String fileId) {
        StoredFile storedFile = repository.findById(Long.parseLong(fileId))
                .orElseThrow(() -> new RuntimeException("Fichier introuvable avec l'ID: " + fileId));

        return new FileDataResponse(storedFile.getData(), mapper.toMetadata(storedFile));
    }

}
