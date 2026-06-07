package dev.jbyanx.finsight.domain.auth.mapper;

import dev.jbyanx.finsight.domain.auth.dto.UserDTO;
import dev.jbyanx.finsight.infrastructure.security.UserPrincipal;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserDTO toDTO(UserPrincipal principal);
}