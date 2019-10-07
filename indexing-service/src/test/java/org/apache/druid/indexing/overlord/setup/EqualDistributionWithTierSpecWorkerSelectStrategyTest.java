/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.druid.indexing.overlord.setup;

import com.google.common.collect.ImmutableMap;
import org.apache.druid.indexing.common.task.NoopTask;
import org.apache.druid.indexing.overlord.ImmutableWorkerInfo;
import org.apache.druid.indexing.overlord.config.RemoteTaskRunnerConfig;
import org.apache.druid.indexing.worker.Worker;
import org.apache.druid.java.util.common.DateTimes;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashSet;

public class EqualDistributionWithTierSpecWorkerSelectStrategyTest
{
  private static final ImmutableMap<String, ImmutableWorkerInfo> WORKERS_FOR_TIER_TESTS =
      ImmutableMap.of(
          "localhost0",
          new ImmutableWorkerInfo(
              new Worker("http", "localhost0", "localhost0", 1, "v1", "t1"), 0,
              new HashSet<>(),
              new HashSet<>(),
              DateTimes.nowUtc()
          ),
          "localhost1",
          new ImmutableWorkerInfo(
              new Worker("http", "localhost1", "localhost1", 2, "v1", "t1"), 0,
              new HashSet<>(),
              new HashSet<>(),
              DateTimes.nowUtc()
          ),
          "localhost2",
          new ImmutableWorkerInfo(
              new Worker("http", "localhost2", "localhost2", 3, "v1", "t2"), 0,
              new HashSet<>(),
              new HashSet<>(),
              DateTimes.nowUtc()
          ),
          "localhost3",
          new ImmutableWorkerInfo(
              new Worker("http", "localhost3", "localhost3", 4, "v1", "t2"), 0,
              new HashSet<>(),
              new HashSet<>(),
              DateTimes.nowUtc()
          )
      );

  @Test
  public void testFindWorkerForTaskWithNullWorkerTierSpec()
  {
    ImmutableWorkerInfo worker = selectWorker(null);
    Assert.assertEquals("localhost3", worker.getWorker().getHost());
  }

  @Test
  public void testFindWorkerForTaskWithPreferredTier()
  {
    // test defaultTier != null and tierAffinity is not empty
    final WorkerTierSpec workerTierSpec1 = new WorkerTierSpec(
        ImmutableMap.of(
            "noop",
            new WorkerTierSpec.TierConfig(
                "t2",
                ImmutableMap.of("ds1", "t2")
            )
        ),
        false
    );

    ImmutableWorkerInfo worker1 = selectWorker(workerTierSpec1);
    Assert.assertEquals("localhost3", worker1.getWorker().getHost());

    // test defaultTier == null and tierAffinity is not empty
    final WorkerTierSpec workerTierSpec2 = new WorkerTierSpec(
        ImmutableMap.of(
            "noop",
            new WorkerTierSpec.TierConfig(
                null,
                ImmutableMap.of("ds1", "t2")
            )
        ),
        false
    );

    ImmutableWorkerInfo worker2 = selectWorker(workerTierSpec2);
    Assert.assertEquals("localhost3", worker2.getWorker().getHost());

    // test defaultTier != null and tierAffinity is empty
    final WorkerTierSpec workerTierSpec3 = new WorkerTierSpec(
        ImmutableMap.of(
            "noop",
            new WorkerTierSpec.TierConfig(
                "t2",
                null
            )
        ),
        false
    );

    ImmutableWorkerInfo worker3 = selectWorker(workerTierSpec3);
    Assert.assertEquals("localhost3", worker3.getWorker().getHost());
  }

  @Test
  public void testFindWorkerForTaskWithNullPreferredTier()
  {
    final WorkerTierSpec workerTierSpec = new WorkerTierSpec(
        ImmutableMap.of(
            "noop",
            new WorkerTierSpec.TierConfig(
                null,
                null
            )
        ),
        false
    );

    ImmutableWorkerInfo worker = selectWorker(workerTierSpec);
    Assert.assertEquals("localhost3", worker.getWorker().getHost());
  }

  @Test
  public void testWeakTierSpec()
  {
    final WorkerTierSpec workerTierSpec = new WorkerTierSpec(
        ImmutableMap.of(
            "noop",
            new WorkerTierSpec.TierConfig(
                "t1",
                ImmutableMap.of("ds1", "t3")
            )
        ),
        false
    );

    ImmutableWorkerInfo worker = selectWorker(workerTierSpec);
    Assert.assertEquals("localhost3", worker.getWorker().getHost());
  }

  @Test
  public void testStrongTierSpec()
  {
    final WorkerTierSpec workerTierSpec = new WorkerTierSpec(
        ImmutableMap.of(
            "noop",
            new WorkerTierSpec.TierConfig(
                "t1",
                ImmutableMap.of("ds1", "t3")
            )
        ),
        true
    );

    ImmutableWorkerInfo worker = selectWorker(workerTierSpec);
    Assert.assertNull(worker);
  }

  private ImmutableWorkerInfo selectWorker(WorkerTierSpec workerTierSpec)
  {
    final EqualDistributionWithTierSpecWorkerSelectStrategy strategy = new EqualDistributionWithTierSpecWorkerSelectStrategy(
        workerTierSpec);

    ImmutableWorkerInfo worker = strategy.findWorkerForTask(
        new RemoteTaskRunnerConfig(),
        WORKERS_FOR_TIER_TESTS,
        new NoopTask(null, null, "ds1", 1, 0, null, null, null)
    );

    return worker;
  }
}
