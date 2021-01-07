package marquez.db.models;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class LineageDatasetRow extends DatasetRow {
  @Getter private String sourceName;
  @Getter private String namespaceName;
  @Getter private String ioType;

  public LineageDatasetRow(
      final UUID uuid,
      final String type,
      final Instant createdAt,
      final Instant updatedAt,
      final UUID namespaceUuid,
      final String namespaceName,
      final UUID sourceUuid,
      @NonNull final String sourceName,
      final String name,
      final String physicalName,
      final List<UUID> tagUuids,
      @Nullable final Instant lastModified,
      @Nullable final String description,
      @Nullable final UUID currentVersionUuid,
      final String ioType) {
    super(
        uuid,
        type,
        createdAt,
        updatedAt,
        namespaceUuid,
        sourceUuid,
        name,
        physicalName,
        tagUuids,
        lastModified,
        description,
        currentVersionUuid);
    this.namespaceName = namespaceName;
    this.sourceName = sourceName;
    this.ioType = ioType;
  }
}
