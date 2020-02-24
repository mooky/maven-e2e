package local.domain;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import lombok.With;

@Value
@With
@Builder
public class SomeDto {
  @NonNull
  private final String text;
  private final int value;
}
