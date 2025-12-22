package ma.elhanchir.fileservice.validation;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;

@Component
public class FileValidator {

    private final List<String> allowedExtensions;
    private final long maxSize;

    public FileValidator(
            @Value("${file.allowed.extensions}") String extensions,
            @Value("${file.max.size}") long maxSize) {

        this.allowedExtensions = Arrays.stream(extensions.split(","))
                .map(String::trim)
                .map(String::toLowerCase)
                .toList();

        this.maxSize = maxSize;
    }

    public void validate(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Fichier vide");
        }

        if (file.getSize() > maxSize) {
            throw new IllegalArgumentException("Taille maximale dépassée");
        }

        String filename = file.getOriginalFilename();
        if (filename == null || !filename.contains(".")) {
            throw new IllegalArgumentException("Nom de fichier invalide");
        }

        String extension = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
        if (!allowedExtensions.contains(extension)) {
            throw new IllegalArgumentException("Extension non autorisée : " + extension);
        }
    }
}
