package marquez;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NonNull;
import marquez.api.DatasetResource;
import marquez.api.JobResource;
import marquez.api.NamespaceResource;
import marquez.api.SourceResource;
import marquez.api.TagResource;
import marquez.api.exceptions.JdbiExceptionExceptionMapper;
import marquez.api.exceptions.MarquezServiceExceptionMapper;
import marquez.db.DatasetDao;
import marquez.db.DatasetFieldDao;
import marquez.db.DatasetVersionDao;
import marquez.db.JobContextDao;
import marquez.db.JobDao;
import marquez.db.JobVersionDao;
import marquez.db.NamespaceDao;
import marquez.db.NamespaceOwnershipDao;
import marquez.db.OwnerDao;
import marquez.db.RunArgsDao;
import marquez.db.RunDao;
import marquez.db.RunStateDao;
import marquez.db.SourceDao;
import marquez.db.TagDao;
import marquez.service.DatasetService;
import marquez.service.JobService;
import marquez.service.NamespaceService;
import marquez.service.RunService;
import marquez.service.RunTransitionListener;
import marquez.service.SourceService;
import marquez.service.TagService;
import marquez.service.exceptions.MarquezServiceException;
import marquez.service.models.Tag;
import org.jdbi.v3.core.Jdbi;

public final class MarquezContext {
  @Getter private final NamespaceDao namespaceDao;
  @Getter private final OwnerDao ownerDao;
  @Getter private final NamespaceOwnershipDao namespaceOwnershipDao;
  @Getter private final SourceDao sourceDao;
  @Getter private final DatasetDao datasetDao;
  @Getter private final DatasetFieldDao datasetFieldDao;
  @Getter private final DatasetVersionDao datasetVersionDao;
  @Getter private final JobDao jobDao;
  @Getter private final JobVersionDao jobVersionDao;
  @Getter private final JobContextDao jobContextDao;
  @Getter private final RunDao runDao;
  @Getter private final RunArgsDao runArgsDao;
  @Getter private final RunStateDao runStateDao;
  @Getter private final TagDao tagDao;

  private final List<RunTransitionListener> runTransitionListeners;

  @Getter private final NamespaceService namespaceService;
  @Getter private final SourceService sourceService;
  @Getter private final DatasetService datasetService;
  @Getter private final JobService jobService;
  @Getter private final TagService tagService;
  @Getter private final MarquezServiceExceptionMapper serviceExceptionMapper;

  @Getter private final NamespaceResource namespaceResource;
  @Getter private final SourceResource sourceResource;
  @Getter private final DatasetResource datasetResource;
  @Getter private final JobResource jobResource;
  @Getter private final TagResource tagResource;

  @Getter private final ImmutableList<Object> resources;
  @Getter private final JdbiExceptionExceptionMapper jdbiException;
  @Getter private final RunService runService;

  private MarquezContext(
      @NonNull final Jdbi jdbi,
      @NonNull final ImmutableSet<Tag> tags,
      @NonNull final List<RunTransitionListener> runTransitionListeners)
      throws MarquezServiceException {
    this.namespaceDao = jdbi.onDemand(NamespaceDao.class);
    this.ownerDao = jdbi.onDemand(OwnerDao.class);
    this.namespaceOwnershipDao = jdbi.onDemand(NamespaceOwnershipDao.class);
    this.sourceDao = jdbi.onDemand(SourceDao.class);
    this.datasetDao = jdbi.onDemand(DatasetDao.class);
    this.datasetFieldDao = jdbi.onDemand(DatasetFieldDao.class);
    this.datasetVersionDao = jdbi.onDemand(DatasetVersionDao.class);
    this.jobDao = jdbi.onDemand(JobDao.class);
    this.jobVersionDao = jdbi.onDemand(JobVersionDao.class);
    this.jobContextDao = jdbi.onDemand(JobContextDao.class);
    this.runDao = jdbi.onDemand(RunDao.class);
    this.runArgsDao = jdbi.onDemand(RunArgsDao.class);
    this.runStateDao = jdbi.onDemand(RunStateDao.class);
    this.tagDao = jdbi.onDemand(TagDao.class);

    this.runTransitionListeners = runTransitionListeners;

    this.namespaceService = new NamespaceService(namespaceDao, ownerDao, namespaceOwnershipDao);
    this.sourceService = new SourceService(sourceDao);
    this.datasetService =
        new DatasetService(
            namespaceDao, sourceDao, datasetDao, datasetFieldDao, datasetVersionDao, tagDao);
    this.runService =
        new RunService(
            jobVersionDao,
            datasetDao,
            runArgsDao,
            runDao,
            datasetVersionDao,
            runStateDao,
            runTransitionListeners);

    this.jobService =
        new JobService(
            namespaceDao, datasetDao, jobDao, jobVersionDao, jobContextDao, runDao, runService);
    this.tagService = new TagService(tagDao);
    this.tagService.init(tags);
    this.serviceExceptionMapper = new MarquezServiceExceptionMapper();
    this.jdbiException = new JdbiExceptionExceptionMapper();

    this.namespaceResource = new NamespaceResource(namespaceService);
    this.sourceResource = new SourceResource(sourceService);
    this.datasetResource =
        new DatasetResource(namespaceService, datasetService, tagService, runService);
    this.jobResource = new JobResource(namespaceService, jobService, runService);
    this.tagResource = new TagResource(tagService);

    this.resources =
        ImmutableList.of(
            namespaceResource,
            sourceResource,
            datasetResource,
            jobResource,
            tagResource,
            serviceExceptionMapper,
            jdbiException);
  }

  public void registerListener(@NonNull RunTransitionListener listener) {
    runTransitionListeners.add(listener);
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private Jdbi jdbi;
    private ImmutableSet<Tag> tags;
    private List<RunTransitionListener> runTransitionListeners;

    Builder() {
      this.tags = ImmutableSet.of();
      this.runTransitionListeners = new ArrayList<>();
    }

    public Builder jdbi(@NonNull Jdbi jdbi) {
      this.jdbi = jdbi;
      return this;
    }

    public Builder tags(@NonNull ImmutableSet<Tag> tags) {
      this.tags = tags;
      return this;
    }

    public Builder runTransitionListener(@NonNull RunTransitionListener runTransitionListener) {
      return runTransitionListeners(Lists.newArrayList(runTransitionListener));
    }

    public Builder runTransitionListeners(
        @NonNull List<RunTransitionListener> runTransitionListeners) {
      this.runTransitionListeners.addAll(runTransitionListeners);
      return this;
    }

    public MarquezContext build() throws MarquezServiceException {
      return new MarquezContext(jdbi, tags, runTransitionListeners);
    }
  }
}
