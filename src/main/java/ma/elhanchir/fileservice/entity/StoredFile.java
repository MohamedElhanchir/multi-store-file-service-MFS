package ma.elhanchir.fileservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;


@Entity @Data @AllArgsConstructor @NoArgsConstructor @Builder
public class StoredFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String originalName;
    private String storedName; // UUID.ext
    private String extension;
    private String contentType;
    private long size;
    private String storageType; // FS | DB | GED
    private String storagePath; // chemin FS si FS
    private LocalDateTime uploadedAt;

    @Lob
    private byte[] data;

    @PrePersist
    public void prePersist() {
        if (uploadedAt == null) {
            uploadedAt = LocalDateTime.now();
        }
    }
}
