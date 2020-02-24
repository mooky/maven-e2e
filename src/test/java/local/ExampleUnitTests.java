package local;

import static org.assertj.core.api.Assertions.assertThat;

import local.common.UnitTest;
import org.junit.jupiter.api.Test;

class ExampleUnitTests extends UnitTest {

  @Test
  void run_unit_test() {
    assertThat(2 + 2).isEqualTo(4);
  }
}
