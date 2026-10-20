package at.technikum.swendoc.DocumentGroup;

import org.mapstruct.Mapper;

import at.technikum.swendoc.document.DocumentMapper;

@Mapper(uses = DocumentMapper.class)
public interface DocumentGroupMapper {

    DocumentGroupResponse toResponse(DocumentGroup group);
}
