package local.domain;

import lombok.Builder;
import lombok.Value;
import lombok.With;

@Value
@With
@Builder
public class SomeModel {
  final String text;
  final int number;
}
