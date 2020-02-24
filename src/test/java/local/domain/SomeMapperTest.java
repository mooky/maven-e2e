package local.domain;

import static org.assertj.core.api.Assertions.assertThat;

import local.common.UnitTest;
import org.junit.jupiter.api.Test;

class SomeMapperTest extends UnitTest {

  @Test
  void fromDto() {

    // ...just experimenting with MapStruct and immutable types

    final SomeDto dto = SomeDto.builder()
        .text("test")
        .value(123)
        .build();

    final SomeMapper mapper = SomeMapper.INSTANCE;
    final SomeModel model = mapper.mapFrom(dto);
    final SomeModel bazModel = mapper.update(dto.withText("baz"), model);

    assertThat(bazModel.getText()).isEqualTo("baz");
  }

}
