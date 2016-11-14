/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.source;

import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.execution.CompletionHandler;
import org.mule.runtime.api.message.MuleEvent;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.exception.MessagingException;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.execution.ExtensionFlowProcessingPhaseTemplate;
import org.mule.runtime.core.execution.ResponseCompletionCallback;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

final class ExtensionFlowProcessingTemplate implements ExtensionFlowProcessingPhaseTemplate {

  private final Message message;
  private final Processor messageProcessor;
  private final CompletionHandler<Event, MessagingException> completionHandler;
  private final Optional<Object> messagePolicyDescriptor;

  ExtensionFlowProcessingTemplate(Message message,
                                  Processor messageProcessor,
                                  CompletionHandler<Event, MessagingException> completionHandler,
                                  Optional<Object> messagePolicyDescriptor) {
    this.message = message;
    this.messageProcessor = messageProcessor;
    this.completionHandler = completionHandler;
    this.messagePolicyDescriptor = messagePolicyDescriptor;
  }

  public Optional<Object> getMessagePolicyDescriptor() {
    return messagePolicyDescriptor;
  }

  @Override
  public Function<Event, Map<String, Object>> getSuccessfulExecutionResponseParametersFunction() {
    if (completionHandler instanceof SourceAdapter.SourceCompletionHandler) {
      return (event -> ((SourceAdapter.SourceCompletionHandler) completionHandler).createResponseParameters(event));
    }
    throw new UnsupportedOperationException();
  }

  @Override
  public Function<Event, Map<String, Object>> getFailedExecutionResponseParametersFunction() {
    if (completionHandler instanceof SourceAdapter.SourceCompletionHandler) {
      return (event -> ((SourceAdapter.SourceCompletionHandler) completionHandler).createFailureResponseParameters(event));
    }
    throw new UnsupportedOperationException();
  }

  @Override
  public Message getMessage() throws MuleException {
    return message;
  }

  @Override
  public Event routeEvent(Event muleEvent) throws MuleException {
    return messageProcessor.process(muleEvent);
  }

  @Override
  public void sendResponseToClient(Event event, Map<String, Object> parameters,
                                   ResponseCompletionCallback responseCompletionCallback)
      throws MuleException {
    ExtensionSourceExceptionCallback exceptionCallback =
        new ExtensionSourceExceptionCallback(responseCompletionCallback, event, completionHandler::onFailure);
    ((SourceAdapter.SourceCompletionHandler) completionHandler).setResponseParameters(parameters);
    runAndNotify(() -> completionHandler.onCompletion(event, exceptionCallback), event, responseCompletionCallback);
  }

  @Override
  public void sendFailureResponseToClient(MessagingException messagingException,
                                          Map<String, Object> parameters, ResponseCompletionCallback responseCompletionCallback)
      throws MuleException {
    ((SourceAdapter.SourceCompletionHandler) completionHandler).setResponseParameters(parameters);
    runAndNotify(() -> completionHandler.onFailure(messagingException), messagingException.getEvent(),
                 responseCompletionCallback);
  }

  private void runAndNotify(Runnable runnable, MuleEvent event, ResponseCompletionCallback responseCompletionCallback) {
    try {
      runnable.run();
      responseCompletionCallback.responseSentSuccessfully();
    } catch (Exception e) {
      responseCompletionCallback.responseSentWithFailure(new MessagingException((Event) event, e),
                                                         (Event) event);
    }
  }
}
