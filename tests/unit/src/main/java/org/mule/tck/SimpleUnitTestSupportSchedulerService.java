/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tck;

import static java.util.Collections.unmodifiableList;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Stoppable;
import org.mule.runtime.core.api.scheduler.Scheduler;
import org.mule.runtime.core.api.scheduler.SchedulerService;
import org.mule.runtime.core.util.concurrent.NamedThreadFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadPoolExecutor.AbortPolicy;

/**
 * {@link SchedulerService} implementation that provides a shared {@link SimpleUnitTestSupportScheduler}.
 *
 * @since 4.0
 */
public class SimpleUnitTestSupportSchedulerService implements SchedulerService, Stoppable {

  private SimpleUnitTestSupportScheduler scheduler =
      new SimpleUnitTestSupportScheduler(2, new NamedThreadFactory(SimpleUnitTestSupportScheduler.class.getSimpleName()),
                                         new AbortPolicy());

  private List<Scheduler> decorators = new ArrayList<>();

  @Override
  public String getName() {
    return this.getClass().getSimpleName();
  }

  @Override
  public Scheduler cpuLightScheduler() {
    final SimpleUnitTestSupportLifecycleSchedulerDecorator decorator = decorateScheduler();
    decorators.add(decorator);
    return decorator;
  }

  @Override
  public Scheduler ioScheduler() {
    final SimpleUnitTestSupportLifecycleSchedulerDecorator decorator = decorateScheduler();
    decorators.add(decorator);
    return decorator;
  }

  @Override
  public Scheduler computationScheduler() {
    final SimpleUnitTestSupportLifecycleSchedulerDecorator decorator = decorateScheduler();
    decorators.add(decorator);
    return decorator;
  }

  protected SimpleUnitTestSupportLifecycleSchedulerDecorator decorateScheduler() {
    SimpleUnitTestSupportLifecycleSchedulerDecorator spied = spy(new SimpleUnitTestSupportLifecycleSchedulerDecorator(scheduler));

    doReturn(mock(ScheduledFuture.class)).when(spied).scheduleWithCronExpression(any(), anyString());
    doReturn(mock(ScheduledFuture.class)).when(spied).scheduleWithCronExpression(any(), anyString(), any());
    return spied;
  }

  @Override
  public void stop() throws MuleException {
    scheduler.shutdownNow();
  }

  public List<Scheduler> getCreatedSchedulers() {
    return unmodifiableList(decorators);
  }

  public int getScheduledTasks() {
    return scheduler.getScheduledTasks();
  }

  /**
   * @return {@code true} since this Services's scheduler unit testing threads are general purpose.
   */
  @Override
  public boolean isCurrentThreadCpuLight() {
    return true;
  }

  /**
   * @return {@code true} since this Services's scheduler unit testing threads are general purpose.
   */
  @Override
  public boolean isCurrentThreadIo() {
    return true;
  }

  /**
   * @return {@code true} since this Services's scheduler unit testing threads are general purpose.
   */
  @Override
  public boolean isCurrentThreadComputation() {
    return true;
  }
}
