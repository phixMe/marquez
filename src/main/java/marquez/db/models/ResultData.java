package marquez.db.models;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;
import marquez.service.models.NodeId;

@EqualsAndHashCode
@ToString
public final class ResultData implements NodeData {
  @Getter private final NodeId id;
  @Getter private final NodeData data;

  public ResultData(@NonNull final NodeId id, @NonNull final NodeData data) {
    this.id = id;
    this.data = data;
  }
}
