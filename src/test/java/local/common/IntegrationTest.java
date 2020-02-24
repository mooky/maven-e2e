package local.common;

import com.github.dockerjava.api.command.CreateContainerCmd;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import lombok.val;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestInstance;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

@Tag(TestTags.Integration)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class IntegrationTest {

  private GenericContainer newman;

  @BeforeAll
  protected void setupNewman() throws IOException {
    startNewmanContainer();
  }

  @AfterAll
  protected void teardownNewman() {
    stopNewmanContainer();
  }

  protected String getBaseUrl() {
    try {
      val ip = InetAddress.getLocalHost().getHostAddress();
      return "http://" + ip + ":8080";
    } catch (Exception e) {
      throw new RuntimeException("Unknown local host", e);
    }
  }

  protected void startNewmanContainer() throws IOException {
    /*
     * Start Newman container so that it sits quiesced at a shell prompt. Test cases will
     * invoke into the running newman container using docker exec.
     */
    newman = new GenericContainer("postman/newman:ubuntu")
        .withCreateContainerCmdModifier(new Consumer<CreateContainerCmd>() {
          @Override
          public void accept(final CreateContainerCmd cmd) {
            cmd
                .withEntrypoint("sh")
                .withTty(true)
                .withStdinOpen(true);
          }
        });

    // TODO: Investigate moving this mapping to a target subfolder so that test
    //  reports can be captured.
    newman.withFileSystemBind(
        new File("./src/main/postman").getCanonicalPath(), "/etc/newman");

    newman.start();
  }

  protected void stopNewmanContainer() {
    newman.stop();
  }

  protected EndToEndTest.NewmanResult newmanRun(final File collection,
      final Map<String, String> env) {

    try {
      val environment = Map.of(
          "id", UUID.randomUUID().toString(),
          "name", "e2e",
          "values", List.of(
              Map.of(
                  "key", "baseUrl",
                  "value", getBaseUrl(),
                  "enabled", true
              )
          ),
          "_postman_variable_scope", "environment"
      );

      // TODO: Investigate moving this mapping to a target subfolder so that test
      //  reports can be captured.
      val environmentFile = new File("./src/main/postman/environment.json");
      environmentFile.deleteOnExit();

      new ObjectMapper().writeValue(environmentFile, environment);

      // Equivalent to cli docker exec ...
      var result = newman
          .execInContainer("newman", "run", collection.toString(), "-e", "environment.json");

      return new EndToEndTest.NewmanResult(result.getStdout(), result.getStderr(),
          result.getExitCode());

    } catch (IOException | InterruptedException e) {
      throw new RuntimeException("Newman operation failed.", e);
    }
  }

  public static class NewmanResult {

    public final String stdout;
    public final String stderr;
    public final int exitCode;

    /**
     * Creates a data object collect output results.
     * @param stdout Text collected from stdout
     * @param stderr Text collected from stderr
     * @param exitCode Integer exit code
     */
    public NewmanResult(final String stdout, final String stderr, final int exitCode) {
      this.stdout = stdout;
      this.stderr = stderr;
      this.exitCode = exitCode;
    }
  }
}
