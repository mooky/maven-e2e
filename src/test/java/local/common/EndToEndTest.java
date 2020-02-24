package local.common;

import com.github.dockerjava.api.command.CreateContainerCmd;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import lombok.val;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestInstance;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

@Tag(TestTags.EndToEnd)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class EndToEndTest {

  private static final String BASE_URL = "baseUrl";
  private static final String DOCKER_COMPOSE_PROJECT_RELATIVE_PATH
      = "src/main/docker/end-to-end-docker-compose.yml";
  private static final String NEWMAN_IMAGE_NAME = "postman/newman:ubuntu";

  private DockerComposeContainer composeContainer;
  private GenericContainer newman;

  @BeforeAll
  protected void setupEnvironment() throws IOException {
    startDockerCompose(new File(DOCKER_COMPOSE_PROJECT_RELATIVE_PATH));
    startNewmanContainer();
  }

  @AfterAll
  protected void teardownEnvironment() {
    stopNewmanContainer();
    stopDockerCompose();
  }

  /*
   * Override to provide base url and port for service under test for Postman's Newman to invoke.
   * Note that you cannot use 'localhost' as the Newman container would treat that as a container
   * relative address and not the host operating system address.
   *
   * Example: http://192.168.0.23:8080
   */
  protected String getBaseUrl() {
    try {
      val ip = InetAddress.getLocalHost().getHostAddress();
      return "http://" + ip + ":8080";
    } catch (Exception e) {
      throw new RuntimeException("Unknown local host", e);
    }
  }

  protected void startNewmanContainer() throws IOException {

    if (newman != null) {
      throw new RuntimeException("Newman container already started.");
    }

    /*
     * Start Newman container so that it sits quiesced at a shell prompt. Test cases will
     * invoke into the running newman container using docker exec.
     */
    newman = new GenericContainer(NEWMAN_IMAGE_NAME)
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

  protected void startDockerCompose(final File dockerComposeYaml) {

    Assert.notNull(dockerComposeYaml, "dockerComposeYaml is required");

    if (composeContainer != null) {
      throw new RuntimeException("Docker compose container already started.");
    }

    composeContainer = new DockerComposeContainer("e2e", dockerComposeYaml)
        .withLocalCompose(false)
        .withExposedService("kafka_1", 9092,
            Wait.forListeningPort())
        .withExposedService("api_1", 8080,
            Wait.forHttp("/actuator/health").forStatusCode(200));

    composeContainer.start();

  }

  protected void stopDockerCompose() {

    if (composeContainer == null) {
      throw new RuntimeException("Docker compose already stopped.");
    }

    composeContainer.stop();
    composeContainer = null;
  }

  protected void stopNewmanContainer() {

    if (newman == null) {
      throw new RuntimeException("Newman container already stopped.");
    }

    newman.stop();
    newman = null;
  }

  /**
   * Executes a Postman collection using the newman runner.
   *
   * @param collection The collection to run relative to the postman folder.
   * @return The exit code and stdout / stderr output.
   */
  protected NewmanResult runNewman(final File collection) {
    Assert.notNull(collection, "collection is required.");

    return runNewman(collection, Map.of());
  }

  protected NewmanResult runNewman(final File collection, final Map<String, String> environment) {

    Assert.notNull(collection, "collection is required.");
    Assert.notNull(environment, "environment is required");

    try {
      val envWithBaseUrl = new HashMap<>(environment);
      if (!environment.containsKey(BASE_URL)) {
        envWithBaseUrl.put(BASE_URL, getBaseUrl());
      }

      final var values = envWithBaseUrl.entrySet().stream()
          .map(entry -> Map.of(
              "key", entry.getKey(),
              "value", entry.getValue(),
              "enabled", true
          ))
          .collect(Collectors.toList());

      val envData = Map.of(
          "id", UUID.randomUUID().toString(),
          "name", "e2e",
          "values", values,
          "_postman_variable_scope", "environment"
      );

      // TODO: Investigate moving this mapping to a target subfolder so that test
      //  reports can be captured.
      val environmentFile = File
          .createTempFile("e2e-env-", ".json", new File("./src/main/postman"));
      environmentFile.deleteOnExit();

      // Convert to JSON and write.
      new ObjectMapper().writeValue(environmentFile, envData);

      // Equivalent to cli docker exec ...
      val result = newman
          .execInContainer("newman", "run", collection.toString(), "-e", environmentFile.getName());

      return new NewmanResult(result.getStdout(), result.getStderr(), result.getExitCode());

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

    @Override
    public String toString() {
      val sb = new StringBuilder()
          .append("Newman exit code = ")
          .append(exitCode);

      if (StringUtils.hasText(stderr)) {
        sb
            .append("\n-------------------- [Begin: Newman STDERR] --------------------\n")
            .append(stderr)
            .append("\n-------------------- [End: Newman STDERR] --------------------\n");
      }

      if (StringUtils.hasText(stdout)) {
        sb
            .append("\n-------------------- [Begin: Newman STDOUT] --------------------\n")
            .append(stdout)
            .append("\n-------------------- [End: Newman STDOUT] --------------------\n");
      }

      return sb.toString();
    }
  }
}
