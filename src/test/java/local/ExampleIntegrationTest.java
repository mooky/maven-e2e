package local;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import local.common.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
class ExampleIntegrationTest extends IntegrationTest {

  @Test
  void contextLoads() {
  }

  @Test
  void execute_within_docker_container() throws IOException, InterruptedException {
    var result = newmanRun(new File("collection.json"), null);
    System.out.println(result.stdout);
    assertThat(result.exitCode).isEqualTo(0);
  }
}
