# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

import pytest
import mock

from airflow.models import (TaskInstance, DagRun)
from airflow.operators.dummy_operator import DummyOperator
from airflow.utils.decorators import apply_defaults
from airflow.utils.db import provide_session
from airflow.utils.dates import days_ago
from airflow.utils import timezone
from airflow.utils.state import State

from marquez_client.models import JobType, DatasetType

from marquez_airflow import DAG
from marquez_airflow.extractors import (
    BaseExtractor, StepMetadata, Source, Dataset
    )
from marquez_airflow.utils import get_location, get_job_name

from uuid import UUID

NO_INPUTS = []
NO_OUTPUTS = []

DEFAULT_DATE = timezone.datetime(2016, 1, 1)

DAG_ID = 'test_dag'
DAG_RUN_ID = 'test_run_id_for_task_completed_and_failed'
DAG_RUN_ARGS = {'external_trigger': False}
# TODO: check with a different namespace and owner
DAG_NAMESPACE = 'default'
DAG_OWNER = 'anonymous'
DAG_DESCRIPTION = \
    'A simple DAG to test the marquez.DAG metadata extraction flow.'

DAG_DEFAULT_ARGS = {
    'owner': DAG_OWNER,
    'depends_on_past': False,
    'start_date': days_ago(1),
    'email_on_failure': False,
    'email_on_retry': False,
    'email': ['owner@test.com']
}

TASK_ID_COMPLETED = 'test_task_completed'
TASK_ID_FAILED = 'test_task_failed'


@pytest.fixture
@provide_session
def clear_db_airflow_dags(session=None):
    session.query(DagRun).delete()
    session.query(TaskInstance).delete()


@provide_session
def test_new_run_id(clear_db_airflow_dags, session=None):
    dag = DAG(
        DAG_ID,
        schedule_interval='@daily',
        default_args=DAG_DEFAULT_ARGS,
        description=DAG_DESCRIPTION
    )
    run_id = dag.new_run_id()
    assert UUID(run_id).version == 4


# tests a simple workflow with default extraction mechanism
@mock.patch('marquez_airflow.DAG.new_run_id')
@mock.patch('marquez_airflow.DAG.get_or_create_marquez_client')
@provide_session
def test_marquez_dag(mock_get_or_create_marquez_client, mock_uuid,
                     clear_db_airflow_dags, session=None):

    dag = DAG(
        DAG_ID,
        schedule_interval='@daily',
        default_args=DAG_DEFAULT_ARGS,
        description=DAG_DESCRIPTION
    )
    # (1) Mock the marquez client method calls
    mock_marquez_client = mock.Mock()
    mock_get_or_create_marquez_client.return_value = mock_marquez_client
    run_id_completed = "my-test_marquez_dag-uuid-completed"
    run_id_failed = "my-test_marquez_dag-uuid-failed"
    mock_uuid.side_effect = [run_id_completed, run_id_failed]

    # (2) Add task that will be marked as completed
    task_will_complete = DummyOperator(
        task_id=TASK_ID_COMPLETED,
        dag=dag
    )
    completed_task_location = get_location(task_will_complete.dag.fileloc)

    # (3) Add task that will be marked as failed
    task_will_fail = DummyOperator(
        task_id=TASK_ID_FAILED,
        dag=dag
    )
    failed_task_location = get_location(task_will_complete.dag.fileloc)

    # (4) Create DAG run and mark as running
    dagrun = dag.create_dagrun(
        run_id=DAG_RUN_ID,
        execution_date=DEFAULT_DATE,
        state=State.RUNNING)

    # Assert namespace meta call
    mock_marquez_client.create_namespace.assert_called_once_with(DAG_NAMESPACE,
                                                                 DAG_OWNER)

    # Assert source and dataset meta calls
    mock_marquez_client.create_datasource.assert_not_called()
    mock_marquez_client.create_dataset.assert_not_called()

    # Assert job meta calls
    create_job_calls = [
        mock.call(
            job_name=f"{DAG_ID}.{TASK_ID_COMPLETED}",
            job_type=JobType.BATCH,
            location=completed_task_location,
            input_dataset=NO_INPUTS,
            output_dataset=NO_OUTPUTS,
            context=mock.ANY,
            description=DAG_DESCRIPTION,
            namespace_name=DAG_NAMESPACE
        ),
        mock.call(
            job_name=f"{DAG_ID}.{TASK_ID_FAILED}",
            job_type=JobType.BATCH,
            location=failed_task_location,
            input_dataset=NO_INPUTS,
            output_dataset=NO_OUTPUTS,
            context=mock.ANY,
            description=DAG_DESCRIPTION,
            namespace_name=DAG_NAMESPACE
        )
    ]
    mock_marquez_client.create_job.assert_has_calls(create_job_calls)

    # Assert job run meta calls
    create_job_run_calls = [
        mock.call(
            job_name=f"{DAG_ID}.{TASK_ID_COMPLETED}",
            run_id=mock.ANY,
            run_args=DAG_RUN_ARGS,
            nominal_start_time=mock.ANY,
            nominal_end_time=mock.ANY,
            namespace_name=DAG_NAMESPACE
        ),
        mock.call(
            job_name=f"{DAG_ID}.{TASK_ID_FAILED}",
            run_id=mock.ANY,
            run_args=DAG_RUN_ARGS,
            nominal_start_time=mock.ANY,
            nominal_end_time=mock.ANY,
            namespace_name=DAG_NAMESPACE
        )
    ]
    mock_marquez_client.create_job_run.assert_has_calls(create_job_run_calls)

    # Assert start run meta calls
    start_job_run_calls = [
        mock.call(run_id=run_id_completed),
        mock.call(run_id=run_id_failed)
    ]
    mock_marquez_client.mark_job_run_as_started.assert_has_calls(
        start_job_run_calls
    )

    # (5) Start task that will be marked as completed
    task_will_complete.run(start_date=DEFAULT_DATE, end_date=DEFAULT_DATE)

    dag.handle_callback(dagrun, success=True, session=session)
    mock_marquez_client.mark_job_run_as_completed.assert_called_once_with(
        run_id=run_id_completed
    )

    # When a task run completes, the task outputs are also updated in order
    # to link a job version (=task version) to a dataset version.
    # Using a DummyOperator, no outputs exists, so assert that the create
    # dataset call is not invoked.
    mock_marquez_client.create_dataset.assert_not_called()

    # (6) Start task that will be marked as failed
    task_will_fail.run(start_date=DEFAULT_DATE, end_date=DEFAULT_DATE)

    dag.handle_callback(dagrun, success=False, session=session)
    mock_marquez_client.mark_job_run_as_failed.assert_called_once_with(
        run_id=run_id_failed
    )

    # Assert an attempt to version the outputs of a task is not made when
    # a task fails
    mock_marquez_client.create_dataset.assert_not_called()


class TestFixtureDummyOperator(DummyOperator):

    @apply_defaults
    def __init__(self, *args, **kwargs):
        super(TestFixtureDummyOperator, self).__init__(*args, **kwargs)


class TestFixtureDummyExtractor(BaseExtractor):
    operator_class = TestFixtureDummyOperator
    source = Source(
            type="DummySource",
            name="dummy_source_name",
            connection_url="http://dummy/source/url")

    def __init__(self, operator):
        super().__init__(operator)

    def extract(self) -> [StepMetadata]:
        inputs = [
            Dataset.from_table(self.source, "extract_input1")
        ]
        outputs = [
            Dataset.from_table(self.source, "extract_output1")
        ]
        return [StepMetadata(
            name=get_job_name(task=self.operator),
            inputs=inputs,
            outputs=outputs,
            context={
                "extract": "extract"
            }
        )]

    def extract_on_complete(self, task_instance) -> [StepMetadata]:
        inputs = [
            Dataset.from_table(self.source, "extract_on_complete_input1")
        ]
        outputs = [
            Dataset.from_table(self.source, "extract_on_complete_output1")
        ]
        return [StepMetadata(
            name=get_job_name(task=self.operator),
            inputs=inputs,
            outputs=outputs,
            context={
                "extract_on_complete": "extract_on_complete"
            }
        )]


# test the lifecycle including with extractors
@mock.patch('marquez_airflow.DAG.new_run_id')
@mock.patch('marquez_airflow.DAG.get_or_create_marquez_client')
@provide_session
def test_marquez_dag_with_extractor(mock_get_or_create_marquez_client,
                                    mock_uuid,
                                    clear_db_airflow_dags,
                                    session=None):

    # --- test setup
    dag_id = 'test_marquez_dag_with_extractor'
    dag = DAG(
        dag_id,
        schedule_interval='@daily',
        default_args=DAG_DEFAULT_ARGS,
        description=DAG_DESCRIPTION
    )

    run_id = "my-test-uuid"
    mock_uuid.side_effect = [run_id]
    # Mock the marquez client method calls
    mock_marquez_client = mock.Mock()
    mock_get_or_create_marquez_client.return_value = mock_marquez_client

    # Add task that will be marked as completed
    task_will_complete = TestFixtureDummyOperator(
        task_id=TASK_ID_COMPLETED,
        dag=dag
    )
    completed_task_location = get_location(task_will_complete.dag.fileloc)

    # Add the dummy extractor to the list for the task above
    dag._extractors[task_will_complete.__class__] = TestFixtureDummyExtractor

    # --- pretend run the DAG

    # Create DAG run and mark as running
    dagrun = dag.create_dagrun(
        run_id='test_marquez_dag_with_extractor_run_id',
        execution_date=DEFAULT_DATE,
        state=State.RUNNING)

    # --- Asserts that the job starting triggers metadata updates

    # Namespace created
    mock_marquez_client.create_namespace.assert_called_once_with(DAG_NAMESPACE,
                                                                 DAG_OWNER)

    # Datasets are updated
    # TODO: why is create_datasource not called?
    mock_marquez_client.create_datasource.assert_not_called()
    mock_marquez_client.create_dataset.assert_has_calls([
        mock.call(
            dataset_name='extract_input1',
            dataset_type=DatasetType.DB_TABLE,
            physical_name='extract_input1',
            source_name='dummy_source_name',
            namespace_name=DAG_NAMESPACE,
            run_id=None
        ),
        mock.call(
            dataset_name='extract_output1',
            dataset_type=DatasetType.DB_TABLE,
            physical_name='extract_output1',
            source_name='dummy_source_name',
            namespace_name=DAG_NAMESPACE,
            run_id=None
        )
    ])
    mock_marquez_client.create_dataset.reset_mock()

    # job is updated
    mock_marquez_client.create_job.assert_called_once_with(
        job_name=f"{dag_id}.{TASK_ID_COMPLETED}",
        job_type=JobType.BATCH,
        location=completed_task_location,
        input_dataset=[{'namespace': 'default', 'name': 'extract_input1'}],
        output_dataset=[{'namespace': 'default', 'name': 'extract_output1'}],
        context=mock.ANY,
        description=DAG_DESCRIPTION,
        namespace_name=DAG_NAMESPACE
    )
    assert mock_marquez_client.create_job.mock_calls[0].\
        kwargs['context'].get('extract') == 'extract'
    mock_marquez_client.create_job.reset_mock()

    # run is created
    mock_marquez_client.create_job_run.assert_called_once_with(
        job_name=f"{dag_id}.{TASK_ID_COMPLETED}",
        run_id=run_id,
        run_args=DAG_RUN_ARGS,
        nominal_start_time=mock.ANY,
        nominal_end_time=mock.ANY,
        namespace_name=DAG_NAMESPACE
    )

    # run is started
    mock_marquez_client.mark_job_run_as_started.assert_called_once_with(
        run_id=run_id
    )

    # --- Pretend complete the task
    task_will_complete.run(start_date=DEFAULT_DATE, end_date=DEFAULT_DATE)

    dag.handle_callback(dagrun, success=True, session=session)

    # --- Assert that the right marquez calls are done

    # job is updated before completion
    mock_marquez_client.create_job.assert_has_calls([
        mock.call(
            job_name=f"{dag_id}.{TASK_ID_COMPLETED}",
            job_type=JobType.BATCH,
            location=completed_task_location,
            input_dataset=[
                {'namespace': 'default', 'name': 'extract_on_complete_input1'}
            ],
            output_dataset=[
                {'namespace': 'default', 'name': 'extract_on_complete_output1'}
            ],
            context=mock.ANY,
            description=DAG_DESCRIPTION,
            namespace_name=DAG_NAMESPACE,
            run_id=run_id
        ),
        # TODO: consolidate the two calls, this second call is spurious
        mock.call(
            job_name=f"{dag_id}.{TASK_ID_COMPLETED}",
            job_type=JobType.BATCH,
            location=completed_task_location,
            input_dataset=[
                {'namespace': 'default', 'name': 'extract_input1'}
            ],
            output_dataset=[
                {'namespace': 'default', 'name': 'extract_output1'}
            ],
            context=mock.ANY,
            description=DAG_DESCRIPTION,
            namespace_name=DAG_NAMESPACE,
        )
    ])

    assert mock_marquez_client.create_job.mock_calls[0].\
        kwargs['context'].get('extract_on_complete') == 'extract_on_complete'

    mock_marquez_client.mark_job_run_as_completed.assert_called_once_with(
        run_id=run_id
    )

    # When a task run completes, the task outputs are also updated in order
    # to link a job version (=task version) to a dataset version.

    # TODO: see what the correct contract is
    # in the current state, both inputs and outputs are updated here
    mock_marquez_client.create_dataset.assert_has_calls([
        mock.call(
            dataset_name='extract_on_complete_input1',
            dataset_type=DatasetType.DB_TABLE,
            physical_name='extract_on_complete_input1',
            source_name='dummy_source_name',
            namespace_name=DAG_NAMESPACE,
            run_id=run_id
        ),
        mock.call(
            dataset_name='extract_on_complete_output1',
            dataset_type=DatasetType.DB_TABLE,
            physical_name='extract_on_complete_output1',
            source_name='dummy_source_name',
            namespace_name=DAG_NAMESPACE,
            run_id=run_id
        )
    ])
