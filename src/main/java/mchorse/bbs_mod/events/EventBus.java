package mchorse.bbs_mod.events;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

public class EventBus
{
    private final Map<Class<?>, CopyOnWriteArrayList<Subscription>> subscribers = new HashMap<>();

    public void register(Object subscriber)
    {
        for (Method method : subscriber.getClass().getMethods())
        {
            this.subscribe(subscriber, method);
        }
    }

    private void subscribe(Object subscriber, Method method)
    {
        if (method.isAnnotationPresent(Subscribe.class))
        {
            //Getting the event from method parameters
            if (method.getParameterCount() != 1) {
                return;
            }

            this.subscribers.computeIfAbsent(method.getParameterTypes()[0],
                            clazz -> new CopyOnWriteArrayList<>())
                    .add(new Subscription(subscriber, method));
        }
    }

    public void post(Object event)
    {
        //Getting subscribers by eventType
        CopyOnWriteArrayList<Subscription> eventSubscribers = this.subscribers.get(event.getClass());

        if (eventSubscribers == null || eventSubscribers.isEmpty())
        {
            return;
        }

        for (Subscription subscription : eventSubscribers)
        {
            try
            {
                subscription.method.invoke(subscription.target, event);
            } catch (Exception ignored)
            {
            }
        }
    }
}
