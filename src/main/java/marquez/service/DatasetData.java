package marquez.service;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.time.Instant;
import java.util.Optional;
import javax.annotation.Nullable;
import lombok.NonNull;
import lombok.Value;
import marquez.common.models.DatasetId;
import marquez.common.models.DatasetName;
import marquez.common.models.DatasetType;
import marquez.common.models.Field;
import marquez.common.models.NamespaceName;
import marquez.common.models.SourceName;
import marquez.common.models.TagName;
import marquez.db.models.NodeData;

@Value
public class DatasetData implements NodeData {
    @NonNull DatasetId id;
    @NonNull DatasetType type;
    @NonNull DatasetName name;
    @NonNull DatasetName physicalName;
    @NonNull Instant createdAt;
    @NonNull Instant updatedAt;
    @NonNull NamespaceName namespace;
    @NonNull SourceName sourceName;
    @NonNull ImmutableList<Field> fields;
    @NonNull ImmutableSet<TagName> tags;
    @Nullable Instant lastModifiedAt;
    @Nullable String description;

    public Optional<Instant> getLastModifiedAt() {
        return Optional.ofNullable(lastModifiedAt);
    }

    public Optional<String> getDescription() {
        return Optional.ofNullable(description);
    }
}

