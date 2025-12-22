package ma.elhanchir.fileservice.mapper;

import ma.elhanchir.fileservice.dto.FileMetadata;
import ma.elhanchir.fileservice.entity.StoredFile;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface StoredFileMapper {

    StoredFileMapper INSTANCE = Mappers.getMapper(StoredFileMapper.class);

    @Mapping(source = "id", target = "fileId")
    @Mapping(source = "storedName", target = "storedName")
    @Mapping(source = "originalName", target = "originalName")
    @Mapping(source = "extension", target = "extension")
    @Mapping(source = "contentType", target = "contentType")
    @Mapping(source = "size", target = "size")
    @Mapping(source = "storageType", target = "storageType")
    @Mapping(source = "uploadedAt", target = "uploadedAt")
    FileMetadata toMetadata(StoredFile file);


}