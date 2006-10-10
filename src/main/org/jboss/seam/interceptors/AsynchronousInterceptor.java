package org.jboss.seam.interceptors;

import javax.ejb.Timer;
import javax.interceptor.AroundInvoke;
import javax.interceptor.InvocationContext;

import org.jboss.seam.annotations.Asynchronous;
import org.jboss.seam.annotations.Interceptor;
import org.jboss.seam.contexts.Contexts;
import org.jboss.seam.core.Dispatcher;

@Interceptor(around={BijectionInterceptor.class, RemoveInterceptor.class, 
                     ConversationInterceptor.class, EventInterceptor.class, 
                     RollbackInterceptor.class, TransactionInterceptor.class,
                     ExceptionInterceptor.class, BusinessProcessInterceptor.class,
                     ManagedEntityIdentityInterceptor.class})
public class AsynchronousInterceptor extends AbstractInterceptor
{
   @AroundInvoke
   public Object invokeAsynchronouslyIfNecessary(InvocationContext invocation) throws Exception
   {
      boolean scheduleAsync = invocation.getMethod().isAnnotationPresent(Asynchronous.class) && 
            !Contexts.getEventContext().isSet(Dispatcher.EXECUTING_ASYNCHRONOUS_CALL);
      if ( scheduleAsync )
      {
         Timer timer = Dispatcher.instance().scheduleInvocation(invocation, component);
         //if the method returns a Timer, return it to the client
         return invocation.getMethod().getReturnType().equals(Timer.class) ? timer : null;
      }
      else
      {
         return invocation.proceed();
      }
   }
}
