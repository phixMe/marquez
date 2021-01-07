package marquez.api;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.ResponseMetered;
import com.codahale.metrics.annotation.Timed;
import java.util.ArrayList;
import java.util.List;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import lombok.NonNull;
import marquez.common.models.DatasetId;
import marquez.common.models.JobId;
import marquez.db.models.LineageDatasetRow;
import marquez.db.models.ResultData;
import marquez.service.DatasetData;
import marquez.service.DatasetService;
import marquez.service.JobData;
import marquez.service.JobService;
import marquez.service.models.Dataset;
import marquez.service.models.Job;
import marquez.service.models.NodeId;

@Path("/api/v1/lineage")
public class LineageResource {

  private final JobService jobService;
  private final DatasetService datasetService;

  public LineageResource(
      @NonNull final DatasetService datasetService, @NonNull final JobService jobService) {
    this.datasetService = datasetService;
    this.jobService = jobService;
  }

  @Timed
  @ResponseMetered
  @ExceptionMetered
  @PUT
  @Path("{nodeId}")
  @Consumes(APPLICATION_JSON)
  @Produces(APPLICATION_JSON)
  public Response get(
      @QueryParam("nodeId") @NotNull NodeId nodeId,
      @DefaultValue("20") @Min(value = 0) @Max(value = 100) @QueryParam("depth") int depth) {
    List<ResultData> lineageResults = new ArrayList<>();

    if (nodeId.isJobType()) {
      JobId jobId = nodeId.asJobId();
      Job job = this.jobService.get(jobId.getNamespace(), jobId.getName()).get();
      lineageResults.add(
          new ResultData(
              nodeId,
              new JobData(
                  jobId,
                  job.getType(),
                  job.getName(),
                  job.getCreatedAt(),
                  job.getUpdatedAt(),
                  job.getNamespace(),
                  job.getInputs(),
                  job.getOutputs(),
                  job.getLocation().get(),
                  job.getContext(),
                  job.getDescription().get(),
                  job.getLatestRun().get())));
      List<LineageDatasetRow> lineageDatasetRow =
          this.datasetService.getLinks(jobId.getNamespace(), jobId.getName());
    } else if (nodeId.isDatasetType()) {
      DatasetId datasetId = nodeId.asDatasetId();
      Dataset dataset =
          this.datasetService.get(datasetId.getNamespace(), datasetId.getName()).get();
      lineageResults.add(
          new ResultData(
              nodeId,
              new DatasetData(
                  dataset.getId(),
                  dataset.getType(),
                  dataset.getName(),
                  dataset.getPhysicalName(),
                  dataset.getCreatedAt(),
                  dataset.getUpdatedAt(),
                  dataset.getNamespace(),
                  dataset.getSourceName(),
                  dataset.getFields(),
                  dataset.getTags(),
                  dataset.getLastModifiedAt().get(),
                  dataset.getDescription().get())));
    }

    return Response.ok().build();
  }
}
