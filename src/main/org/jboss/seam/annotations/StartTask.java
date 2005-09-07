/*
 * JBoss, Home of Professional Open Source
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jboss.seam.annotations;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Target;
import java.lang.annotation.Retention;
import java.lang.annotation.Documented;

/**
 * Marks a method as causing jBPM {@link org.jbpm.taskmgmt.exe.TaskInstance task}
 * to be marked as started.
 * <p/>
 * Note that both {@link ResumeTask} and {@link StartTask} have effect
 * before invocation of the intercepted method in that they are both
 * about setting up appropriate {@link org.jbpm.context.exe.ContextInstance}
 * for the current {@link org.jboss.seam.contexts.BusinessProcessContext};
 * {@link StartTask} however, also has effect after method invocation
 * as that is the time it actually marks the task as started.
 *
 * @see org.jbpm.taskmgmt.exe.TaskInstance#start()
 */
@Target( METHOD )
@Retention( RUNTIME )
@Documented
public @interface StartTask
{
   /**
    * The name of the context variable under which we should locate the
    * the id of task to be started.
    */
   String taskIdName();
   /**
    * Should we push actor information onto the task, or allow any defined
    * assigments/swimlanes take effect?
    */
   boolean pushActor() default false;
   /**
    * A (optional) JSF expression resolving to the jBPM actorId to which
    * the task should be assigned using the push model.
    */
   String actorExpression() default "";
   /**
    * The name under which to expose the jBPM
    * {@link org.jbpm.taskmgmt.exe.TaskInstance} into conversation context.
    *
    * optional; defaults to {@link org.jboss.seam.interceptors.BusinessProcessInterceptor#DEF_TASK_NAME}.
    */
   String taskName() default "";
   /**
    * The name under which to expose the jBPM
    * {@link org.jbpm.graph.exe.ProcessInstance} into conversation context.
    *
    * optional; defaults to {@link org.jboss.seam.interceptors.BusinessProcessInterceptor#DEF_PROCESS_NAME}.
    */
   String processName() default "";
}
