package at.technikum.swendoc.user;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface UserMapper {

    UserResponse toResponse(User user);

    @Mapping(target = "userId", source = "user.id")
    SessionResponse toResponse(Session session);
}
