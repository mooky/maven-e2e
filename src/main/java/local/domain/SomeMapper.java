package local.domain;

import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper
public interface SomeMapper {

  // Won't be needed. Can be injected as a Spring Bean instead.
  SomeMapper INSTANCE = Mappers.getMapper(SomeMapper.class);

  @Mapping(source = "value", target = "number")
  SomeModel mapFrom(final SomeDto dto);

  @InheritInverseConfiguration(name = "mapFrom")
  SomeDto mapTo(final SomeModel model);

  default SomeModel update(final SomeDto source, final SomeModel target) {
    return target.withText(source.getText());
  }
}

