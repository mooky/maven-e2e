package local;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import local.common.EndToEndTest;
import org.junit.jupiter.api.Test;

class ExampleEndToEndTest extends EndToEndTest {

  @Test
  void api_passes_happy_path_scenarios() {

    var result = runNewman(new File("collection.json"));

    System.out.println(result);
    assertThat(result.exitCode).isEqualTo(0);
  }

}
