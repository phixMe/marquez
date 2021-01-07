package marquez.service.models;

import static com.google.common.base.Preconditions.checkArgument;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.regex.Pattern;
import lombok.Getter;
import marquez.common.models.DatasetId;
import marquez.common.models.DatasetName;
import marquez.common.models.JobId;
import marquez.common.models.JobName;
import marquez.common.models.NamespaceName;

public class NodeId {
  public static final String ID_DELIM = ":";

  private static final String ID_PREFX_DATASET = "dataset";
  private static final String ID_PREFX_JOB = "job";
  private static final Pattern ID_PATTERN =
      Pattern.compile(String.format("^(%s|%s):.*$", ID_PREFX_DATASET, ID_PREFX_JOB));

  @Getter private final String value;

  public NodeId(final String value) {
    checkArgument(
        ID_PATTERN.matcher(value).matches(),
        "node ID (%s) must start with '%s', '%s', or '%s'",
        value,
        ID_PREFX_DATASET,
        ID_PREFX_JOB);
    this.value = value;
  }

  @JsonIgnore
  public boolean isDatasetType() {
    return value.startsWith(ID_PREFX_DATASET);
  }

  @JsonIgnore
  public boolean isJobType() {
    return value.startsWith(ID_PREFX_JOB);
  }

  @JsonIgnore
  private String[] parts(int expectedParts, String expectedType) {
    String[] parts = value.split(ID_DELIM);
    if (parts.length != expectedParts || !expectedType.equals(parts[0])) {
      throw new UnsupportedOperationException(
          String.format(
              "Expected NodeId of type %s with %s parts. Got: %s",
              expectedType, expectedParts, getValue()));
    }
    return parts;
  }

  @JsonIgnore
  public JobId asJobId() {
    String[] parts = parts(3, ID_PREFX_JOB);
    return new JobId(NamespaceName.of(parts[1]), JobName.of(parts[2]));
  }

  @JsonIgnore
  public DatasetId asDatasetId() {
    String[] parts = parts(3, ID_PREFX_DATASET);
    return new DatasetId(NamespaceName.of(parts[1]), DatasetName.of(parts[2]));
  }
}
