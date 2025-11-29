package mchorse.bbs_mod.events;

import java.lang.reflect.Method;

public class Subscription
{
    public final Object target;
    public final Method method;

    public Subscription(Object target, Method method)
    {
        this.target = target;
        this.method = method;

        //If target does not have access to the method, then we give access to the method.
        if (!method.canAccess(target)) {
            method.setAccessible(true);
        }
    }
}
