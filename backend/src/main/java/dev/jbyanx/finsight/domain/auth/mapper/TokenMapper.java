package dev.jbyanx.finsight.domain.auth.mapper;

import dev.jbyanx.finsight.domain.auth.dto.SessionDTO;
import dev.jbyanx.finsight.domain.auth.entity.Token;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import java.util.List;

@Mapper(componentModel = "spring")
public interface TokenMapper {

    @Mapping(target = "type", expression = "java(token.getType().name())")
    SessionDTO toSessionDTO(Token token);

    List<SessionDTO> toSessionDTOList(List<Token> tokens);
}