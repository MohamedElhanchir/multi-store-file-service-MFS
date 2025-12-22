package ma.elhanchir.fileservice.dto;

import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FileMetadata {
    private String fileId;
    private String originalName;
    private String storedName;
    private String extension;
    private String contentType;
    private long size;
    private String storageType;
    private LocalDateTime uploadedAt;
}
