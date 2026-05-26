/**
 * The contents of this file are subject to the OpenMRS Public License Version 1.0 (the "License");
 * you may not use this file except in compliance with the License. You may obtain a copy of the
 * License at http://license.openmrs.org
 *
 * <p>Software distributed under the License is distributed on an "AS IS" basis, WITHOUT WARRANTY OF
 * ANY KIND, either express or implied. See the License for the specific language governing rights
 * and limitations under the License.
 *
 * <p>Copyright (C) OpenMRS, LLC. All Rights Reserved.
 */
package org.openmrs.event;

import jakarta.jms.Destination;
import jakarta.jms.JMSException;
import jakarta.jms.MapMessage;
import jakarta.jms.Message;
import jakarta.jms.Topic;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.openmrs.OpenmrsObject;
import org.openmrs.api.APIException;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.context.Context;
import org.openmrs.util.OpenmrsUtil;
import org.openmrs.util.PrivilegeConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Used by {@link Event}. */
public class EventEngine {

  protected static final String DELIMITER = ":";

  protected static Logger log = LoggerFactory.getLogger(EventEngine.class);

  protected Map<String, Map<String, EventListener>> subscribers =
      new HashMap<String, Map<String, EventListener>>();

  /**
   * This inner class holds the context for managing a subscription. Basically it serves to simplify
   * using the {@link EventClassScanner} to manage subscriptions for a specific class
   *
   * @param <T>
   */
  private static class SubscriptionContext<T> implements AutoCloseable {
    private volatile Collection<Class<? extends T>> eventClasses = null;
    private final EventClassScanner classScanner;
    private final Class<T> clazz;

    public SubscriptionContext(Class<T> clazz) {
      this.classScanner =
          EventClassScannerThreadHolder.getCurrentEventClassScanner()
              .orElseGet(EventClassScanner::new);
      this.clazz = clazz;
    }

    @Override
    public void close() {
      try {
        if (eventClasses != null) {
          eventClasses.clear();
        }
      } finally {
        classScanner.close();
      }
    }

    public Class<T> getClazz() {
      return clazz;
    }

    public Collection<Class<? extends T>> getEventClasses()
        throws IOException, ClassNotFoundException {
      if (eventClasses == null) {
        synchronized (this) {
          if (eventClasses == null) {
            eventClasses = classScanner.getClasses(clazz);
          }
        }
      }

      return eventClasses;
    }
  }

  /**
   * @see Event#fireAction(String, Object)
   */
  public void fireAction(String action, final Object object) {
    Destination key = getDestination(object.getClass(), action);
    fireEvent(key, object);
  }

  /**
   * @see Event#fireEvent(Destination, Object)
   */
  public void fireEvent(final Destination dest, final Object object) {
    EventMessage eventMessage = new EventMessage();
    if (object instanceof OpenmrsObject) {
      eventMessage.put("uuid", ((OpenmrsObject) object).getUuid());
    }
    eventMessage.put("classname", object.getClass().getName());
    eventMessage.put("action", getAction(dest));

    doFireEvent(dest, eventMessage);
  }

  /**
   * @see Event#fireEvent(String, EventMessage)
   */
  public void fireEvent(String topicName, EventMessage eventMessage) {
    if (StringUtils.isBlank(topicName)) {
      throw new APIException("Topic name cannot be null or blank");
    }
    doFireEvent(getDestination(topicName), eventMessage);
  }

  private void doFireEvent(final Destination dest, final EventMessage eventMessage) {
    if (!enabled()) {
      return;
    }

    try {
      String topicName = getTopicName(dest);
      Map<String, EventListener> topicSubscribers = subscribers.get(topicName);
      if (topicSubscribers == null || topicSubscribers.isEmpty()) {
        return;
      }
      Message message = createMapMessage(eventMessage, dest);
      if (log.isInfoEnabled()) {
        log.info("Sending data {}", eventMessage);
      }
      for (EventListener listener : new ArrayList<>(topicSubscribers.values())) {
        listener.onMessage(message);
      }
    } catch (JMSException e) {
      throw new APIException("Exception raised while firing event", e);
    }
  }

  private boolean enabled() {
    return !OpenmrsUtil.getApplicationDataDirectoryAsFile()
        .toPath()
        .resolve("activemq-data")
        .resolve("disabled")
        .toFile()
        .exists();
  }

  private String getExternalUrl() {
    boolean addedProxyPrivilege =
        Context.isAuthenticated()
            && !Context.hasPrivilege(PrivilegeConstants.GET_GLOBAL_PROPERTIES);
    try {
      if (addedProxyPrivilege) {
        Context.addProxyPrivilege(PrivilegeConstants.GET_GLOBAL_PROPERTIES);
      }
      return Context.getRegisteredComponent("adminService", AdministrationService.class)
          .getGlobalProperty("activeMQ.externalUrl");
    } catch (NullPointerException ex) {
      log.error(
          "AdministrationService not yet initialized to get the activeMQ.externalUrl setting", ex);
    } finally {
      if (addedProxyPrivilege) {
        Context.removeProxyPrivilege(PrivilegeConstants.GET_GLOBAL_PROPERTIES);
      }
    }
    return null;
  }

  /**
   * @see Event#subscribe(Class, String, EventListener)
   */
  public <T> void subscribe(Class<T> clazz, String action, EventListener listener) {
    if (clazz == null || listener == null) {
      return;
    }

    try (SubscriptionContext<T> context = new SubscriptionContext<>(clazz)) {
      if (action != null) {
        subscribeToClass(context, Collections.singletonList(action), listener);
        log.info(
            "{} subscribed to {} events for {}",
            listener.getClass(),
            action,
            clazz.getSimpleName());
      } else {
        subscribeToClass(context, Event.Action.getActionNames(), listener);
        log.info("{} subscribed to all events for {}", listener.getClass(), clazz.getSimpleName());
      }
    }
  }

  /** */
  public <T> void subscribe(Class<T> clazz, Collection<String> actions, EventListener listener) {
    if (clazz == null || listener == null) {
      return;
    }

    try (SubscriptionContext<T> context = new SubscriptionContext<>(clazz)) {
      if (actions != null) {
        if (!actions.isEmpty()) {
          subscribeToClass(context, actions, listener);
          log.info(
              "{} subscribed to {} events for {}",
              listener.getClass(),
              StringUtils.join(actions, ','),
              clazz.getSimpleName());
        }
      } else {
        subscribeToClass(context, Event.Action.getActionNames(), listener);
      }
    }
  }

  /**
   * Adds subscriptions to the topics that match the specified action and class including subclasses
   *
   * @param context the current subscription context
   * @param actions the action(s) to match
   * @param listener the Listener subscribing to the topic
   */
  private <T> void subscribeToClass(
      SubscriptionContext<T> context, Collection<String> actions, EventListener listener) {
    try {
      for (Class<? extends T> c : context.getEventClasses()) {
        for (String action : actions) {
          Destination dest = getDestination(c, action);
          subscribe(dest, listener);
        }
      }
    } catch (IOException | ClassNotFoundException e) {
      throw new APIException(
          "Exception raised while creating subscription for " + context.getClazz(), e);
    }
  }

  /**
   * @see Event#subscribe(String, EventListener)
   */
  public void subscribe(String topicName, EventListener listener) {
    if (StringUtils.isBlank(topicName)) {
      throw new APIException("Topic name cannot be null or blank");
    }

    subscribe(getDestination(topicName), listener);
  }

  /**
   * @see Event#unsubscribe(Class, Event.Action, EventListener)
   */
  public void unsubscribe(Class<?> clazz, Event.Action action, EventListener listener) {
    if (clazz == null || listener == null) {
      return;
    }

    if (action != null) {
      unsubscribeFromClass(clazz, action.toString(), listener);
      log.info(
          "{} unsubscribed from {} events for {}",
          action,
          listener.getClass(),
          clazz.getSimpleName());
    } else {
      unsubscribeFromClass(clazz, Event.Action.getActionNames(), listener);
      log.info(
          "{} unsubscribed from all events for {}", listener.getClass(), clazz.getSimpleName());
    }
  }

  /**
   * @see Event#unsubscribe(Class, Collection, EventListener)
   */
  public void unsubscribe(Class<?> clazz, Collection<Event.Action> action, EventListener listener) {
    if (clazz == null || listener == null) {
      return;
    }

    if (action != null) {
      List<String> eventActions =
          action.stream().map(Event.Action::toString).collect(Collectors.toList());
      unsubscribeFromClass(clazz, eventActions, listener);
      log.info(
          "{} unsubscribed from {} events for {}",
          listener.getClass(),
          StringUtils.join(eventActions, ','),
          clazz.getSimpleName());
    } else {
      unsubscribeFromClass(clazz, Event.Action.getActionNames(), listener);
      log.info(
          "{} unsubscribed from all events for {}", listener.getClass(), clazz.getSimpleName());
    }
  }

  /**
   * Removes subscriptions from the topics that match the specified action and class including
   * subclasses
   *
   * @param clazz the class to match
   * @param action the action to match
   * @param listener the Listener subscribing to the top
   */
  private <T> void unsubscribeFromClass(Class<T> clazz, String action, EventListener listener) {
    if (clazz == null || listener == null) {
      return;
    }

    try (SubscriptionContext<T> context = new SubscriptionContext<>(clazz)) {
      for (Class<? extends T> c : context.getEventClasses()) {
        Destination dest = getDestination(c, action);
        unsubscribe(dest, listener);
      }
    } catch (IOException | ClassNotFoundException e) {
      throw new APIException(e);
    }
  }

  /**
   * Removes subscriptions from the topics that match the specified action and class including
   * subclasses
   *
   * @param clazz the class to match
   * @param actions the actions to match
   * @param listener the Listener subscribing to the top
   */
  private <T> void unsubscribeFromClass(
      Class<T> clazz, Collection<String> actions, EventListener listener) {
    if (clazz == null || listener == null) {
      return;
    }

    try (SubscriptionContext<T> context = new SubscriptionContext<>(clazz)) {
      for (Class<? extends T> c : context.getEventClasses()) {
        for (String action : actions) {
          Destination dest = getDestination(c, action);
          unsubscribe(dest, listener);
        }
      }
    } catch (IOException | ClassNotFoundException e) {
      throw new APIException(e);
    }
  }

  /**
   * @see Event#unsubscribe(String, EventListener)
   */
  public void unsubscribe(String topicName, EventListener listener) {
    if (StringUtils.isBlank(topicName)) {
      throw new APIException("Topic name cannot be null or blank");
    }
    unsubscribe(getDestination(topicName), listener);
  }

  /**
   * @see Event#getDestination(Class, String)
   */
  public Destination getDestination(final Class<?> clazz, final String action) {
    return getDestination(action + DELIMITER + clazz.getName());
  }

  /**
   * @see Event#getDestinationFor(String)
   */
  public Destination getDestination(final String topicName) {
    return (Topic) () -> topicName;
  }

  /**
   * @see Event#subscribe(Destination, EventListener)
   */
  public void subscribe(Destination destination, final EventListener listenerToRegister) {
    if (enabled()) {
      try {
        String topicName = getTopicName(destination);
        Map<String, EventListener> topicSubscribers =
            subscribers.computeIfAbsent(topicName, key -> new LinkedHashMap<>());
        topicSubscribers.put(listenerToRegister.getClass().getName(), listenerToRegister);

      } catch (JMSException e) {
        log.error("Exception occurred while subscribing", e);
      }
    }
  }

  /**
   * @see Event#unsubscribe(Destination, EventListener)
   */
  public void unsubscribe(Destination dest, EventListener listener) {
    if (enabled()) {
      if (dest != null) {
        try {
          String topicName = getTopicName(dest);
          Map<String, EventListener> topicSubscribers = subscribers.get(topicName);
          if (topicSubscribers != null) {
            topicSubscribers.remove(listener.getClass().getName());
            if (topicSubscribers.isEmpty()) {
              subscribers.remove(topicName);
            }
          }
        } catch (JMSException e) {
          log.error("Failed to unsubscribe from the specified destination:", e);
        }
      }
    }
  }

  /**
   * @see Event#setSubscription(SubscribableEventListener)
   */
  public void setSubscription(SubscribableEventListener listenerToRegister) {

    // loop over each object and each action to register
    for (Class<? extends OpenmrsObject> objectClass : listenerToRegister.subscribeToObjects()) {
      for (String action : listenerToRegister.subscribeToActions()) {
        Destination key = getDestination(objectClass, action);
        subscribe(key, listenerToRegister);
      }
    }
  }

  /**
   * @see Event#unsetSubscription(SubscribableEventListener)
   */
  public void unsetSubscription(SubscribableEventListener listenerToRegister) {
    for (Class<? extends OpenmrsObject> objectClass : listenerToRegister.subscribeToObjects()) {
      for (String action : listenerToRegister.subscribeToActions()) {
        Destination key = getDestination(objectClass, action);
        unsubscribe(key, listenerToRegister);
      }
    }
  }

  protected String getAction(final Destination dest) {
    if (dest instanceof Topic) {
      // look for delimiter and get string before that
      String topicName;
      try {
        topicName = getTopicName(dest);
      } catch (JMSException e) {
        // TODO fail hard here? document this in javadoc too
        return null;
      }
      int index = topicName.indexOf(DELIMITER);
      if (index < 0) {
        // uh, what? TODO: document this
        return null;
      }

      return topicName.substring(0, index);

    } else {
      // what kind of Destination is this if not a Topic??
      // TODO: document this
      return null;
    }
  }

  protected String getTopicName(final Destination dest) throws JMSException {
    if (dest instanceof Topic) {
      return ((Topic) dest).getTopicName();
    }
    return String.valueOf(dest);
  }

  protected MapMessage createMapMessage(EventMessage eventMessage, Destination destination) {
    Map<String, Object> body = new LinkedHashMap<>();
    if (eventMessage != null) {
      body.putAll(eventMessage);
    }
    Map<String, Object> properties = new HashMap<>();
    return (MapMessage)
        Proxy.newProxyInstance(
            MapMessage.class.getClassLoader(),
            new Class<?>[] {MapMessage.class},
            (proxy, method, args) ->
                handleMapMessageInvocation(method, args, body, properties, destination));
  }

  private Object handleMapMessageInvocation(
      Method method,
      Object[] args,
      Map<String, Object> body,
      Map<String, Object> properties,
      Destination destination) {
    String methodName = method.getName();
    if ("toString".equals(methodName)) {
      return body.toString();
    }
    if ("hashCode".equals(methodName)) {
      return System.identityHashCode(body);
    }
    if ("equals".equals(methodName)) {
      return args != null && args.length == 1 && args[0] == body;
    }
    if ("getMapNames".equals(methodName)) {
      return Collections.enumeration(body.keySet());
    }
    if ("itemExists".equals(methodName)) {
      return body.containsKey(args[0]);
    }
    if ("getObject".equals(methodName)) {
      return body.get(args[0]);
    }
    if (methodName.startsWith("get")
        && args != null
        && args.length == 1
        && args[0] instanceof String) {
      return coerce(body.get(args[0]), method.getReturnType());
    }
    if (methodName.startsWith("set")
        && args != null
        && args.length >= 2
        && args[0] instanceof String) {
      body.put((String) args[0], args[1]);
      return null;
    }
    if ("clearBody".equals(methodName)) {
      body.clear();
      return null;
    }
    if ("getPropertyNames".equals(methodName)) {
      return Collections.enumeration(properties.keySet());
    }
    if ("propertyExists".equals(methodName) && args != null && args.length > 0) {
      return properties.containsKey(args[0]);
    }
    if ("getObjectProperty".equals(methodName) && args != null && args.length > 0) {
      return properties.get(args[0]);
    }
    if (methodName.startsWith("get")
        && methodName.endsWith("Property")
        && args != null
        && args.length > 0) {
      return coerce(properties.get(args[0]), method.getReturnType());
    }
    if (methodName.startsWith("set")
        && methodName.endsWith("Property")
        && args != null
        && args.length >= 2) {
      properties.put((String) args[0], args[1]);
      return null;
    }
    if ("clearProperties".equals(methodName)) {
      properties.clear();
      return null;
    }
    if ("getJMSDestination".equals(methodName)) {
      return destination;
    }
    if ("getBody".equals(methodName)) {
      if (args == null || args.length != 1 || args[0] == null) {
        return null;
      }
      Class<?> bodyType = (Class<?>) args[0];
      return bodyType.isInstance(body) ? bodyType.cast(body) : null;
    }
    if ("isBodyAssignableTo".equals(methodName)) {
      if (args == null || args.length != 1 || args[0] == null) {
        return false;
      }
      return ((Class<?>) args[0]).isInstance(body);
    }
    if ("acknowledge".equals(methodName) || method.getReturnType() == Void.TYPE) {
      return null;
    }
    return defaultValue(method.getReturnType());
  }

  private Object coerce(Object value, Class<?> targetType) {
    if (value == null) {
      return defaultValue(targetType);
    }
    if (targetType == String.class) {
      return value.toString();
    }
    if (targetType == byte[].class && value instanceof byte[]) {
      return value;
    }
    if (targetType == boolean.class) {
      return value instanceof Boolean ? value : Boolean.parseBoolean(value.toString());
    }
    if (targetType == char.class) {
      return value instanceof Character ? value : value.toString().charAt(0);
    }
    if (value instanceof Number number) {
      if (targetType == byte.class) {
        return number.byteValue();
      }
      if (targetType == short.class) {
        return number.shortValue();
      }
      if (targetType == int.class) {
        return number.intValue();
      }
      if (targetType == long.class) {
        return number.longValue();
      }
      if (targetType == float.class) {
        return number.floatValue();
      }
      if (targetType == double.class) {
        return number.doubleValue();
      }
    }
    return value;
  }

  private Object defaultValue(Class<?> targetType) {
    if (!targetType.isPrimitive()) {
      return null;
    }
    if (targetType == boolean.class) {
      return false;
    }
    if (targetType == char.class) {
      return '\0';
    }
    return 0;
  }

  /** Closes the underlying shared connection which will close the broker too under the hood */
  public void shutdown() {
    if (log.isDebugEnabled()) log.debug("Clearing event subscriptions...");

    subscribers.clear();
  }
}
