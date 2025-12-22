package ma.elhanchir.fileservice.dto;

import lombok.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UploadResponse  {
    private boolean success;
    private String message;
    private FileMetadata metadata;
}