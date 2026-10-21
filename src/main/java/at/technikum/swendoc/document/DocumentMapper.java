package at.technikum.swendoc.document;

import org.mapstruct.Mapper;

@Mapper
public interface DocumentMapper {

    DocumentResponse toResponse(Document document);
}
