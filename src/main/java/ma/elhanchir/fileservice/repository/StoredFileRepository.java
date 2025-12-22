package ma.elhanchir.fileservice.repository;

import ma.elhanchir.fileservice.entity.StoredFile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StoredFileRepository extends JpaRepository<StoredFile, Long> {
}
