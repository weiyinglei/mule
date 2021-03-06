/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.context.notification;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;
import static org.mule.runtime.core.DefaultEventContext.create;
import static org.mule.runtime.core.context.notification.MessageProcessingFlowTraceManager.FLOW_STACK_INFO_KEY;

import org.mule.runtime.api.meta.AnnotatedObject;
import org.mule.runtime.core.api.CoreEventContext;
import org.mule.runtime.core.api.EventContext;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.config.MuleConfiguration;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.context.notification.FlowCallStack;
import org.mule.runtime.core.api.context.notification.ProcessorsTrace;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.config.DefaultMuleConfiguration;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.xml.namespace.QName;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.hamcrest.collection.IsCollectionWithSize.hasSize;

@SmallTest
public class MessageProcessingFlowTraceManagerTestCase extends AbstractMuleTestCase {

  private static QName docNameAttrName = new QName("http://www.mulesoft.org/schema/mule/documentation", "name");
  private static QName sourceFileNameAttrName = new QName("http://www.mulesoft.org/schema/mule/documentation", "sourceFileName");
  private static QName sourceFileLineAttrName = new QName("http://www.mulesoft.org/schema/mule/documentation", "sourceFileLine");

  private static final String NESTED_FLOW_NAME = "nestedFlow";
  private static final String ROOT_FLOW_NAME = "rootFlow";
  private static final String APP_ID = "MessageProcessingFlowTraceManagerTestCase";

  private static boolean originalFlowTrace;

  @BeforeClass
  public static void beforeClass() {
    originalFlowTrace = DefaultMuleConfiguration.flowTrace;
    DefaultMuleConfiguration.flowTrace = true;
  }

  @AfterClass
  public static void afterClass() {
    DefaultMuleConfiguration.flowTrace = originalFlowTrace;
  }

  private MessageProcessingFlowTraceManager manager;
  private EventContext messageContext;

  private FlowConstruct rootFlowConstruct;
  private FlowConstruct nestedFlowConstruct;

  @Before
  public void before() {
    manager = new MessageProcessingFlowTraceManager();
    MuleContext context = mock(MuleContext.class);
    MuleConfiguration config = mock(MuleConfiguration.class);
    when(config.getId()).thenReturn(APP_ID);
    when(context.getConfiguration()).thenReturn(config);
    manager.setMuleContext(context);

    rootFlowConstruct = mock(FlowConstruct.class);
    when(rootFlowConstruct.getName()).thenReturn(ROOT_FLOW_NAME);
    when(rootFlowConstruct.getMuleContext()).thenReturn(context);
    nestedFlowConstruct = mock(FlowConstruct.class);
    when(nestedFlowConstruct.getName()).thenReturn(NESTED_FLOW_NAME);
    when(nestedFlowConstruct.getMuleContext()).thenReturn(context);

    messageContext = create(rootFlowConstruct, "test");
  }

  @Test
  public void newFlowInvocation() {
    Event event = buildEvent("newFlowInvocation");
    PipelineMessageNotification pipelineNotification = buildPipelineNotification(event, rootFlowConstruct.getName());
    assertThat(getContextInfo(event, rootFlowConstruct), is(""));

    manager.onPipelineNotificationStart(pipelineNotification);
    assertThat(getContextInfo(event, rootFlowConstruct), is("at " + rootFlowConstruct.getName()));

    manager.onPipelineNotificationComplete(pipelineNotification);
    assertThat(getContextInfo(event, rootFlowConstruct), is(""));
  }

  @Test
  public void nestedFlowInvocations() {
    Event event = buildEvent("nestedFlowInvocations");
    PipelineMessageNotification pipelineNotification = buildPipelineNotification(event, rootFlowConstruct.getName());
    assertThat(getContextInfo(event, rootFlowConstruct), is(""));

    manager.onPipelineNotificationStart(pipelineNotification);
    manager.onMessageProcessorNotificationPreInvoke(buildProcessorNotification(event, mock(Processor.class),
                                                                               NESTED_FLOW_NAME + "_ref"));

    PipelineMessageNotification pipelineNotificationNested = buildPipelineNotification(event, NESTED_FLOW_NAME);
    manager.onPipelineNotificationStart(pipelineNotificationNested);

    String rootEntry = "at " + rootFlowConstruct.getName() + "(" + NESTED_FLOW_NAME + "_ref @ " + APP_ID + ")";
    assertThat(getContextInfo(event, rootFlowConstruct), is("at " + NESTED_FLOW_NAME + System.lineSeparator() + rootEntry));

    manager.onPipelineNotificationComplete(pipelineNotificationNested);
    assertThat(getContextInfo(event, rootFlowConstruct), is(rootEntry));

    manager.onPipelineNotificationComplete(pipelineNotification);
    assertThat(getContextInfo(event, rootFlowConstruct), is(""));
  }

  @Test
  public void newComponentCall() {
    Event event = buildEvent("newComponentCall");
    PipelineMessageNotification pipelineNotification = buildPipelineNotification(event, rootFlowConstruct.getName());
    assertThat(getContextInfo(event, rootFlowConstruct), is(""));

    manager.onPipelineNotificationStart(pipelineNotification);
    assertThat(getContextInfo(event, rootFlowConstruct), is("at " + ROOT_FLOW_NAME));

    manager.onMessageProcessorNotificationPreInvoke(buildProcessorNotification(event, mock(Processor.class), "/comp"));
    assertThat(getContextInfo(event, rootFlowConstruct), is("at " + ROOT_FLOW_NAME + "(/comp @ " + APP_ID + ")"));

    manager.onPipelineNotificationComplete(pipelineNotification);
    assertThat(getContextInfo(event, rootFlowConstruct), is(""));
  }

  protected String getContextInfo(Event event, FlowConstruct flow) {
    return (String) manager.getContextInfo(event, null, flow).get(FLOW_STACK_INFO_KEY);
  }

  @Test
  public void newAnnotatedComponentCall() {
    Event event = buildEvent("newAnnotatedComponentCall");
    PipelineMessageNotification pipelineNotification = buildPipelineNotification(event, rootFlowConstruct.getName());
    assertThat(getContextInfo(event, rootFlowConstruct), is(""));

    manager.onPipelineNotificationStart(pipelineNotification);
    assertThat(getContextInfo(event, rootFlowConstruct), is("at " + rootFlowConstruct.getName()));

    AnnotatedObject annotatedMessageProcessor =
        (AnnotatedObject) mock(Processor.class, withSettings().extraInterfaces(AnnotatedObject.class));
    when(annotatedMessageProcessor.getAnnotation(docNameAttrName)).thenReturn("annotatedName");
    when(annotatedMessageProcessor.getAnnotation(sourceFileNameAttrName)).thenReturn("muleApp.xml");
    when(annotatedMessageProcessor.getAnnotation(sourceFileLineAttrName)).thenReturn(10);
    manager
        .onMessageProcessorNotificationPreInvoke(buildProcessorNotification(event, (Processor) annotatedMessageProcessor,
                                                                            "/comp"));
    assertThat(getContextInfo(event, rootFlowConstruct),
               is("at " + rootFlowConstruct.getName() + "(/comp @ " + APP_ID + ":muleApp.xml:10 (annotatedName))"));

    manager.onPipelineNotificationComplete(pipelineNotification);
    assertThat(getContextInfo(event, rootFlowConstruct), is(""));
  }

  @Test
  public void splitStack() {
    Event event = buildEvent("newAnnotatedComponentCall");
    PipelineMessageNotification pipelineNotification = buildPipelineNotification(event, rootFlowConstruct.getName());
    assertThat(getContextInfo(event, rootFlowConstruct), is(""));

    manager.onPipelineNotificationStart(pipelineNotification);
    assertThat(getContextInfo(event, rootFlowConstruct), is("at " + rootFlowConstruct.getName()));

    manager.onMessageProcessorNotificationPreInvoke(buildProcessorNotification(event, mock(Processor.class), "/comp"));
    assertThat(getContextInfo(event, rootFlowConstruct), is("at " + rootFlowConstruct.getName() + "(/comp @ " + APP_ID + ")"));

    Event eventCopy = buildEvent("newAnnotatedComponentCall", event.getFlowCallStack().clone());
    assertThat(getContextInfo(eventCopy, rootFlowConstruct),
               is("at " + rootFlowConstruct.getName() + "(/comp @ " + APP_ID + ")"));

    manager.onPipelineNotificationComplete(pipelineNotification);
    assertThat(getContextInfo(event, rootFlowConstruct), is(""));

    final FlowConstruct asyncFlowConstruct = mock(FlowConstruct.class);
    when(asyncFlowConstruct.getName()).thenReturn("asyncFlow");
    manager.onPipelineNotificationStart(buildPipelineNotification(eventCopy, asyncFlowConstruct.getName()));

    manager.onMessageProcessorNotificationPreInvoke(buildProcessorNotification(eventCopy, mock(Processor.class),
                                                                               "/asyncComp"));
    assertThat(getContextInfo(eventCopy, asyncFlowConstruct),
               is("at " + asyncFlowConstruct.getName() + "(/asyncComp @ " + APP_ID + ")" + System.lineSeparator()
                   + "at " + rootFlowConstruct.getName() + "(/comp @ " + APP_ID + ")"));
  }

  @Test
  public void splitStackEnd() {
    Event event = buildEvent("newAnnotatedComponentCall");
    PipelineMessageNotification pipelineNotification = buildPipelineNotification(event, rootFlowConstruct.getName());

    manager.onPipelineNotificationStart(pipelineNotification);
    manager.onMessageProcessorNotificationPreInvoke(buildProcessorNotification(event, mock(Processor.class), "/comp"));
    FlowCallStack flowCallStack = event.getFlowCallStack();
    Event eventCopy = buildEvent("newAnnotatedComponentCall", flowCallStack.clone());
    manager.onPipelineNotificationComplete(pipelineNotification);
    String asyncFlowName = "asyncFlow";
    manager.onPipelineNotificationStart(buildPipelineNotification(eventCopy, asyncFlowName));
    manager.onMessageProcessorNotificationPreInvoke(buildProcessorNotification(eventCopy, mock(Processor.class),
                                                                               "/asyncComp"));

    assertThat(((CoreEventContext) event.getContext()).getProcessorsTrace(),
               hasExecutedProcessors("/comp @ " + APP_ID, "/asyncComp @ " + APP_ID));
  }

  @Test
  public void joinStack() {
    Event event = buildEvent("joinStack");
    PipelineMessageNotification pipelineNotification = buildPipelineNotification(event, rootFlowConstruct.getName());
    assertThat(getContextInfo(event, rootFlowConstruct), is(""));

    manager.onPipelineNotificationStart(pipelineNotification);
    assertThat(getContextInfo(event, rootFlowConstruct), is("at " + rootFlowConstruct.getName()));

    manager.onMessageProcessorNotificationPreInvoke(buildProcessorNotification(event, mock(Processor.class),
                                                                               "/scatter-gather"));
    assertThat(getContextInfo(event, rootFlowConstruct),
               is("at " + rootFlowConstruct.getName() + "(/scatter-gather @ " + APP_ID + ")"));

    Event eventCopy0 = buildEvent("joinStack_0", event.getFlowCallStack().clone());
    Event eventCopy1 = buildEvent("joinStack_1", event.getFlowCallStack().clone());

    manager.onMessageProcessorNotificationPreInvoke(buildProcessorNotification(eventCopy0, mock(Processor.class),
                                                                               "/route_0"));

    manager.onMessageProcessorNotificationPreInvoke(buildProcessorNotification(eventCopy1, mock(Processor.class),
                                                                               NESTED_FLOW_NAME + "_ref"));
    PipelineMessageNotification pipelineNotificationNested = buildPipelineNotification(eventCopy1, NESTED_FLOW_NAME);
    manager.onPipelineNotificationStart(pipelineNotificationNested);
    manager.onMessageProcessorNotificationPreInvoke(buildProcessorNotification(eventCopy1, mock(Processor.class),
                                                                               "/route_1"));
    assertThat(getContextInfo(eventCopy1, rootFlowConstruct),
               is("at " + NESTED_FLOW_NAME + "(/route_1 @ " + APP_ID + ")" + System.lineSeparator()
                   + "at " + ROOT_FLOW_NAME + "(" + NESTED_FLOW_NAME + "_ref @ " + APP_ID + ")"));

    manager.onPipelineNotificationComplete(pipelineNotificationNested);

    assertThat(getContextInfo(event, rootFlowConstruct),
               is("at " + rootFlowConstruct.getName() + "(/scatter-gather @ " + APP_ID + ")"));

    manager.onPipelineNotificationComplete(pipelineNotification);
    assertThat(getContextInfo(event, rootFlowConstruct), is(""));
  }

  @Test
  public void joinStackEnd() {
    Event event = buildEvent("joinStack");
    PipelineMessageNotification pipelineNotification = buildPipelineNotification(event, rootFlowConstruct.getName());

    manager.onPipelineNotificationStart(pipelineNotification);
    manager.onMessageProcessorNotificationPreInvoke(buildProcessorNotification(event, mock(Processor.class),
                                                                               "/scatter-gather"));

    FlowCallStack flowCallStack = event.getFlowCallStack();
    Event eventCopy0 = buildEvent("joinStack_0", flowCallStack.clone());
    Event eventCopy1 = buildEvent("joinStack_1", flowCallStack.clone());

    manager.onMessageProcessorNotificationPreInvoke(buildProcessorNotification(eventCopy0, mock(Processor.class),
                                                                               "/route_0"));

    manager.onMessageProcessorNotificationPreInvoke(buildProcessorNotification(eventCopy1, mock(Processor.class),
                                                                               NESTED_FLOW_NAME + "_ref"));
    PipelineMessageNotification pipelineNotificationNested = buildPipelineNotification(eventCopy1, NESTED_FLOW_NAME);
    manager.onPipelineNotificationStart(pipelineNotificationNested);
    manager.onMessageProcessorNotificationPreInvoke(buildProcessorNotification(eventCopy1, mock(Processor.class),
                                                                               "/route_1"));
    manager.onPipelineNotificationComplete(pipelineNotificationNested);

    manager.onPipelineNotificationComplete(pipelineNotification);

    assertThat(((CoreEventContext) event.getContext()).getProcessorsTrace(),
               hasExecutedProcessors("/scatter-gather @ " + APP_ID, "/route_0 @ " + APP_ID,
                                     NESTED_FLOW_NAME + "_ref @ " + APP_ID, "/route_1 @ " + APP_ID));
  }

  @Test
  public void mixedEvents() {
    Event event1 = buildEvent("mixedEvents_1");
    Event event2 = buildEvent("mixedEvents_2");

    PipelineMessageNotification pipelineNotification1 = buildPipelineNotification(event1, rootFlowConstruct.getName());
    PipelineMessageNotification pipelineNotification2 = buildPipelineNotification(event2, rootFlowConstruct.getName());
    assertThat(getContextInfo(event1, rootFlowConstruct), is(""));
    assertThat(getContextInfo(event2, rootFlowConstruct), is(""));

    manager.onPipelineNotificationStart(pipelineNotification1);
    assertThat(getContextInfo(event1, rootFlowConstruct), is("at " + rootFlowConstruct.getName()));
    assertThat(getContextInfo(event2, rootFlowConstruct), is(""));

    manager.onPipelineNotificationStart(pipelineNotification2);
    assertThat(getContextInfo(event1, rootFlowConstruct), is("at " + rootFlowConstruct.getName()));
    assertThat(getContextInfo(event2, rootFlowConstruct), is("at " + rootFlowConstruct.getName()));

    manager.onPipelineNotificationComplete(pipelineNotification1);
    assertThat(getContextInfo(event1, rootFlowConstruct), is(""));
    assertThat(getContextInfo(event2, rootFlowConstruct), is("at " + rootFlowConstruct.getName()));

    manager.onPipelineNotificationComplete(pipelineNotification2);
    assertThat(getContextInfo(event1, rootFlowConstruct), is(""));
    assertThat(getContextInfo(event2, rootFlowConstruct), is(""));
  }

  protected Event buildEvent(String eventId) {
    return buildEvent(eventId, new DefaultFlowCallStack());
  }

  protected Event buildEvent(String eventId, FlowCallStack flowStack) {
    Event event = mock(Event.class);
    when(event.getContext()).thenReturn(messageContext);
    when(event.getFlowCallStack()).thenReturn(flowStack);
    return event;
  }

  protected MessageProcessorNotification buildProcessorNotification(Event event, Processor processor,
                                                                    String processorPath) {
    MessageProcessorNotification processorNotification = mock(MessageProcessorNotification.class);
    when(processorNotification.getProcessor()).thenReturn(processor);
    when(processorNotification.getProcessorPath()).thenReturn(processorPath);
    when(processorNotification.getSource()).thenReturn(event);
    return processorNotification;
  }

  protected PipelineMessageNotification buildPipelineNotification(Event event, String flowName) {
    PipelineMessageNotification pipelineNotification = mock(PipelineMessageNotification.class);
    when(pipelineNotification.getSource()).thenReturn(event);
    when(pipelineNotification.getResourceIdentifier()).thenReturn(flowName);
    return pipelineNotification;
  }

  private Matcher<ProcessorsTrace> hasExecutedProcessors(final String... expectedProcessors) {
    return new TypeSafeMatcher<ProcessorsTrace>() {

      private List<Matcher> failed = new ArrayList<>();

      @Override
      protected boolean matchesSafely(ProcessorsTrace processorsTrace) {
        Matcher<Collection<? extends Object>> sizeMatcher = hasSize(expectedProcessors.length);
        if (!sizeMatcher.matches(processorsTrace.getExecutedProcessors())) {
          failed.add(sizeMatcher);
        }

        int i = 0;
        for (String expectedProcessor : expectedProcessors) {
          Matcher processorItemMatcher = is(expectedProcessor);
          if (!processorItemMatcher.matches(processorsTrace.getExecutedProcessors().get(i))) {
            failed.add(processorItemMatcher);
          }
          ++i;
        }

        return failed.isEmpty();
      }

      @Override
      public void describeTo(Description description) {
        description.appendValue(Arrays.asList(expectedProcessors));
      }

      @Override
      protected void describeMismatchSafely(ProcessorsTrace item, Description description) {
        description.appendText("was ").appendValue(item.getExecutedProcessors());
      }
    };
  }

}
