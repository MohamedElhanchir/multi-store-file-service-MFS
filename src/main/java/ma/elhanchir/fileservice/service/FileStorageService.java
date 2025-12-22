package ma.elhanchir.fileservice.service;

import ma.elhanchir.fileservice.dto.FileDataResponse;
import org.springframework.web.multipart.MultipartFile;
import ma.elhanchir.fileservice.dto.FileMetadata;
import java.io.IOException;



public interface FileStorageService {
    FileMetadata store(MultipartFile file) throws IOException;
    FileDataResponse load(String fileId) throws IOException;
}
