//$Id$
package org.jboss.seam.intercept;

import static org.jboss.seam.util.EJB.AROUND_INVOKE;
import static org.jboss.seam.util.EJB.POST_ACTIVATE;
import static org.jboss.seam.util.EJB.POST_CONSTRUCT;
import static org.jboss.seam.util.EJB.PRE_DESTROY;
import static org.jboss.seam.util.EJB.PRE_PASSIVATE;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import org.jboss.seam.Component;
import org.jboss.seam.InterceptorType;
import org.jboss.seam.annotations.AroundInvoke;
import org.jboss.seam.interceptors.OptimizedInterceptor;
import org.jboss.seam.util.Reflections;

/**
 * Wraps and delegates to a Seam interceptor.
 * 
 * @author Gavin King
 */
public final class Interceptor extends Reflections
{
   private Class<?> userInterceptorClass;
   private Object statelessUserInterceptorInstance;
   private Method aroundInvokeMethod;
   private Method postConstructMethod;
   private Method preDestroyMethod;
   private Method postActivateMethod;
   private Method prePassivateMethod;
   private Method componentInjectorMethod;
   private Method annotationInjectorMethod;
   private InterceptorType type;
   private Annotation annotation;
   private Component component;
   private boolean optimized;

   private boolean isStateless()
   {
      return userInterceptorClass.isAnnotationPresent(org.jboss.seam.annotations.Interceptor.class) &&
            userInterceptorClass.getAnnotation(org.jboss.seam.annotations.Interceptor.class).stateless();
   }
   
   public Object createUserInterceptor()
   {
      if ( isStateless() )
      {
         return statelessUserInterceptorInstance;
      }
      else
      {
         try
         {
            Object userInterceptor = userInterceptorClass.newInstance();
            if (componentInjectorMethod!=null)
            {
               Reflections.invokeAndWrap(componentInjectorMethod, userInterceptor, component);
            }
            if (annotationInjectorMethod!=null) 
            {
               Reflections.invokeAndWrap(annotationInjectorMethod, userInterceptor, annotation);
            }
            return userInterceptor;
         }
         catch (Exception e)
         {
            throw new RuntimeException(e);
         }
      }
   }

   public Class getUserInterceptorClass()
   {
      return userInterceptorClass;
   }
   
   public InterceptorType getType()
   {
      return type;
   }
   
   @Override
   public String toString()
   {
      return "Interceptor(" + userInterceptorClass.getName() + ")";
   }
   
   public Interceptor(Object interceptor, Component component)
   {
      userInterceptorClass = interceptor.getClass();
      statelessUserInterceptorInstance = interceptor;
      this.component = component;
      init();
   }
   
   public Interceptor(Class[] classes, Annotation annotation, Component component) 
   {
      if (classes.length!=1)
      {
         //TODO: remove this silly restriction!
         throw new IllegalArgumentException("Must be exactly one interceptor when used as a meta-annotation");
      }
      userInterceptorClass = classes[0];
      
      try
      {
         statelessUserInterceptorInstance = userInterceptorClass.newInstance();
      }
      catch (Exception e)
      {
         throw new IllegalArgumentException("could not instantiate interceptor", e);
      }
      
      this.annotation = annotation;
      this.component = component;
      
      init();
   }
   

   private void init()
   {
      for (Method method : userInterceptorClass.getMethods())
      {
         if ( !method.isAccessible() ) method.setAccessible(true);
         if ( method.isAnnotationPresent(AROUND_INVOKE) || method.isAnnotationPresent(AroundInvoke.class) )
         {
            aroundInvokeMethod = method;
         }
         if ( method.isAnnotationPresent(POST_CONSTRUCT) )
         {
            postConstructMethod = method;
         }
         if ( method.isAnnotationPresent(PRE_DESTROY) )
         {
            preDestroyMethod = method;
         }
         if ( method.isAnnotationPresent(PRE_PASSIVATE) )
         {
            prePassivateMethod = method;
         }
         if ( method.isAnnotationPresent(POST_ACTIVATE) )
         {
            postActivateMethod = method;
         }

         Class[] params = method.getParameterTypes();
         //if there is a method that takes the annotation, call it, to pass initialization info
         if ( annotation!=null && params.length==1 && params[0]==annotation.annotationType() )
         {
            annotationInjectorMethod = method;
            Reflections.invokeAndWrap(method, userInterceptorClass, annotation);
         }
         //if there is a method that takes the component, call it
         if ( params.length==1 && params[0]==Component.class )
         {
            componentInjectorMethod = method;
            Reflections.invokeAndWrap(method, statelessUserInterceptorInstance, component);
         }
      }

      type = userInterceptorClass.isAnnotationPresent(org.jboss.seam.annotations.Interceptor.class) ?
            userInterceptorClass.getAnnotation(org.jboss.seam.annotations.Interceptor.class).type() :
            InterceptorType.SERVER;
            
      optimized = OptimizedInterceptor.class.isAssignableFrom(userInterceptorClass);
   }
   
   public boolean isOptimized()
   {
      return optimized;
   }
   
   public Object aroundInvoke(InvocationContext invocation, Object userInterceptor) throws Exception
   {
      return aroundInvokeMethod==null ?
            invocation.proceed() :
            Reflections.invoke( aroundInvokeMethod, userInterceptor, invocation );
   }
   
   public Object postConstruct(InvocationContext invocation, Object userInterceptor) throws Exception
   {
      return postConstructMethod==null ?
            invocation.proceed() :
            Reflections.invoke( postConstructMethod, userInterceptor, invocation );
   }
   
   public Object preDestroy(InvocationContext invocation, Object userInterceptor) throws Exception
   {
      return preDestroyMethod==null ?
            invocation.proceed() :
            Reflections.invoke( preDestroyMethod, userInterceptor, invocation );
   }
   
   public Object prePassivate(InvocationContext invocation, Object userInterceptor) throws Exception
   {
      return prePassivateMethod==null ?
            invocation.proceed() :
            Reflections.invoke( prePassivateMethod, userInterceptor, invocation );
   }
   
   public Object postActivate(InvocationContext invocation, Object userInterceptor) throws Exception
   {
      return postActivateMethod==null ?
            invocation.proceed() :
            Reflections.invoke( postActivateMethod, userInterceptor, invocation );
   }
   
}
