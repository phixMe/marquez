package marquez.db.models;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import marquez.service.models.Dataset;
import marquez.service.models.Job;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXTERNAL_PROPERTY,
        property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = Job.class, name = "DATASET"),
        @JsonSubTypes.Type(value = Dataset.class, name = "JOB")
})
public interface NodeData {
}
