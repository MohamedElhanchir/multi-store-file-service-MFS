package ma.elhanchir.fileservice.web;

import lombok.AllArgsConstructor;
import ma.elhanchir.fileservice.dto.FileDataResponse;
import ma.elhanchir.fileservice.dto.FileMetadata;
import ma.elhanchir.fileservice.dto.UploadResponse;
import ma.elhanchir.fileservice.service.FileStorageService;
import ma.elhanchir.fileservice.validation.FileValidator;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/files")
@AllArgsConstructor
public class FileController {

    private final FileStorageService storageService;
    private final FileValidator validator;

    /**
     * Upload un fichier et retourne les métadonnées en JSON
     */
    @PostMapping("/upload")
    public ResponseEntity<UploadResponse> upload(@RequestParam("file") MultipartFile file) {
        try {
            validator.validate(file);
            FileMetadata metadata = storageService.store(file);

            UploadResponse response = UploadResponse.builder()
                    .success(true)
                    .message("Fichier uploadé avec succès")
                    .metadata(metadata)
                    .build();

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            UploadResponse response = UploadResponse.builder()
                    .success(false)
                    .message("Validation échouée: " + e.getMessage())
                    .metadata(null)
                    .build();
            return ResponseEntity.badRequest().body(response);

        } catch (Exception e) {
            UploadResponse response = UploadResponse.builder()
                    .success(false)
                    .message("Erreur lors de l'upload: " + e.getMessage())
                    .metadata(null)
                    .build();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Récupère les métadonnées d'un fichier en JSON
     */
    @GetMapping("/{id}/metadata")
    public ResponseEntity<FileMetadata> getMetadata(@PathVariable String id) {
        try {
            FileDataResponse response = storageService.load(id);
            return ResponseEntity.ok(response.getMetadata());
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Télécharge un fichier avec son nom original et extension
     */
    @GetMapping("/{id}/download")
    public ResponseEntity<byte[]> download(@PathVariable String id) {
        try {
            FileDataResponse response = storageService.load(id);
            FileMetadata metadata = response.getMetadata();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(metadata.getContentType()));
            headers.setContentDispositionFormData("attachment", metadata.getOriginalName());
            headers.setContentLength(metadata.getSize());

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(response.getData());

        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Affiche un fichier dans le navigateur (preview)
     */
    @GetMapping("/{id}/preview")
    public ResponseEntity<byte[]> preview(@PathVariable String id) {
        try {
            FileDataResponse response = storageService.load(id);
            FileMetadata metadata = response.getMetadata();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(metadata.getContentType()));
            headers.setContentDispositionFormData("inline", metadata.getOriginalName());
            headers.setContentLength(metadata.getSize());

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(response.getData());

        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
}