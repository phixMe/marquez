package marquez.service;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.net.URL;
import java.time.Instant;
import java.util.Optional;
import javax.annotation.Nullable;
import lombok.NonNull;
import lombok.Value;
import marquez.common.models.DatasetId;
import marquez.common.models.JobId;
import marquez.common.models.JobName;
import marquez.common.models.JobType;
import marquez.common.models.NamespaceName;
import marquez.db.models.NodeData;
import marquez.service.models.Run;

@Value
public class JobData implements NodeData {
  @NonNull JobId id;
  @NonNull JobType type;
  @NonNull JobName name;
  @NonNull Instant createdAt;
  @NonNull Instant updatedAt;
  @NonNull NamespaceName namespace;
  @NonNull ImmutableSet<DatasetId> inputs;
  @NonNull ImmutableSet<DatasetId> outputs;
  @Nullable URL location;
  @NonNull ImmutableMap<String, String> context;
  @Nullable String description;
  @Nullable Run latestRun;

  public Optional<URL> getLocation() {
    return Optional.ofNullable(location);
  }

  public Optional<String> getDescription() {
    return Optional.ofNullable(description);
  }

  public Optional<Run> getLatestRun() {
    return Optional.ofNullable(latestRun);
  }
}
