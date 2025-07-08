# SparkMonitor Plugin

The SparkMonitor plugin provides real-time monitoring of Apache Spark jobs, stages, and tasks through a Jupyter comm channel in Apache Toree.

## Features

- **SparkListener Integration**: Automatically registers a JupyterSparkMonitorListener to the SparkContext when Spark becomes ready
- **Communication Channel**: Provides a "SparkMonitor" comm target for bi-directional communication with clients
- **Real-time Updates**: Sends comprehensive real-time notifications about:
  - Application start/end events
  - Job start/end events with detailed information
  - Stage submission/completion events with metrics
  - Task start/end events with performance data
  - Executor addition/removal events
- **Error Handling**: Robust error handling to prevent plugin failures from affecting the kernel
- **Performance Monitoring**: Detailed task metrics including execution time, shuffle data, and memory usage

## Architecture

The plugin consists of two main components:

### SparkMonitorPlugin
- Extends the Toree `Plugin` class
- Registers the "SparkMonitor" comm target during initialization
- Listens for the "sparkReady" event to register the SparkListener
- Manages the communication channel lifecycle

### JupyterSparkMonitorListener
- Extends Spark's `SparkListener` class
- Monitors comprehensive Spark events and sends updates through the comm channel
- Handles communication failures gracefully
- Provides detailed event tracking with JSON-formatted messages
- Includes active stage monitoring with periodic updates

## Usage

### Plugin Registration

The plugin is automatically discovered and loaded by Toree's plugin system. No manual registration is required.

### Client Communication

Clients can connect to the SparkMonitor by opening a comm with the target name "SparkMonitor":

```python
# Example using Jupyter notebook
from IPython.display import display
import ipywidgets as widgets
from traitlets import Unicode
import json

# Create a comm to connect to SparkMonitor
comm = Comm(target_name='SparkMonitor')

def handle_spark_event(msg):
    data = msg['content']['data']
    event_type = data.get('event', 'unknown')
    print(f"Spark Event: {event_type}")
    print(f"Details: {data}")

comm.on_msg(handle_spark_event)
```

### Event Types

The plugin sends the following types of events:

#### Application Events
- **sparkApplicationStart**: Fired when a Spark application starts
  - `startTime`: Application start timestamp
  - `appId`: Application identifier
  - `appAttemptId`: Application attempt identifier
  - `appName`: Application name
  - `sparkUser`: Spark user

- **sparkApplicationEnd**: Fired when a Spark application ends
  - `endTime`: Application end timestamp

#### Job Events
- **sparkJobStart**: Fired when a Spark job starts
  - `jobId`: Job identifier
  - `jobGroup`: Job group identifier
  - `status`: Job status (RUNNING)
  - `submissionTime`: Job submission timestamp
  - `stageIds`: Array of stage IDs
  - `stageInfos`: Detailed stage information
  - `numTasks`: Total number of tasks
  - `totalCores`: Total available cores
  - `numExecutors`: Number of executors
  - `name`: Job name/description

- **sparkJobEnd**: Fired when a Spark job ends
  - `jobId`: Job identifier
  - `status`: Job completion status (COMPLETED/FAILED)
  - `completionTime`: Job completion timestamp

#### Stage Events
- **sparkStageSubmitted**: Fired when a stage is submitted
  - `stageId`: Stage identifier
  - `stageAttemptId`: Stage attempt identifier
  - `name`: Stage name
  - `numTasks`: Number of tasks in the stage
  - `parentIds`: Parent stage IDs
  - `submissionTime`: Submission timestamp
  - `jobIds`: Associated job IDs

- **sparkStageCompleted**: Fired when a stage completes
  - `stageId`: Stage identifier
  - `stageAttemptId`: Stage attempt identifier
  - `completionTime`: Completion timestamp
  - `submissionTime`: Submission timestamp
  - `numTasks`: Total number of tasks
  - `numFailedTasks`: Number of failed tasks
  - `numCompletedTasks`: Number of completed tasks
  - `status`: Stage status (COMPLETED/FAILED)
  - `jobIds`: Associated job IDs

- **sparkStageActive**: Periodic updates for active stages
  - `stageId`: Stage identifier
  - `stageAttemptId`: Stage attempt identifier
  - `name`: Stage name
  - `parentIds`: Parent stage IDs
  - `numTasks`: Total number of tasks
  - `numActiveTasks`: Number of currently active tasks
  - `numFailedTasks`: Number of failed tasks
  - `numCompletedTasks`: Number of completed tasks
  - `jobIds`: Associated job IDs

#### Task Events
- **sparkTaskStart**: Fired when a task starts
  - `launchTime`: Task launch timestamp
  - `taskId`: Task identifier
  - `stageId`: Parent stage identifier
  - `stageAttemptId`: Stage attempt identifier
  - `index`: Task index
  - `attemptNumber`: Task attempt number
  - `executorId`: Executor identifier
  - `host`: Host name
  - `status`: Task status
  - `speculative`: Whether task is speculative

- **sparkTaskEnd**: Fired when a task ends
  - `launchTime`: Task launch timestamp
  - `finishTime`: Task finish timestamp
  - `taskId`: Task identifier
  - `stageId`: Parent stage identifier
  - `taskType`: Task type
  - `stageAttemptId`: Stage attempt identifier
  - `index`: Task index
  - `attemptNumber`: Task attempt number
  - `executorId`: Executor identifier
  - `host`: Host name
  - `status`: Task status
  - `speculative`: Whether task is speculative
  - `errorMessage`: Error message (if failed)
  - `metrics`: Detailed task metrics (execution time, shuffle data, memory usage, etc.)

#### Executor Events
- **sparkExecutorAdded**: Fired when an executor is added
  - `executorId`: Executor identifier
  - `time`: Addition timestamp
  - `host`: Host name
  - `numCores`: Number of cores
  - `totalCores`: Total cores across all executors

- **sparkExecutorRemoved**: Fired when an executor is removed
  - `executorId`: Executor identifier
  - `time`: Removal timestamp
  - `totalCores`: Remaining total cores

## Example Spark Code

To see the SparkMonitor in action, run some Spark operations:

```scala
// Create an RDD and perform some operations
val rdd = sc.parallelize(1 to 1000, 10)
val result = rdd.map(_ * 2).filter(_ > 100).collect()

// The SparkMonitor will send notifications about:
// - Job start/end
// - Stage submissions/completions
// - Task executions
```

## Configuration

The plugin doesn't require any specific configuration. It automatically:
- Registers the "SparkMonitor" comm target during kernel initialization
- Registers the JupyterSparkMonitorListener when Spark becomes ready
- Handles comm connection lifecycle automatically
- Provides periodic active stage monitoring (every 1 second)

### Configurable Parameters

The following parameters can be adjusted by modifying the JupyterSparkMonitorListener class:
- `sparkStageActiveTasksMaxMessages`: Maximum number of task messages to buffer (default: 250)
- `sparkStageActiveRate`: Rate of active stage monitoring in milliseconds (default: 1000ms)

## Error Handling

The plugin includes comprehensive error handling:
- Graceful handling of SparkContext unavailability
- Safe communication channel operations
- Logging of errors without affecting kernel stability

## Development

To extend the plugin:

1. Add new event handlers to `SparkMonitorListener`
2. Modify the `sendUpdate` method to include additional data
3. Update the comm message handlers in `SparkMonitorPlugin`

## Testing

Run the plugin tests using:

```bash
sbt "project plugins" test
```

The test suite includes:
- Plugin initialization tests
- SparkListener registration tests
- Communication channel tests
- Error handling tests