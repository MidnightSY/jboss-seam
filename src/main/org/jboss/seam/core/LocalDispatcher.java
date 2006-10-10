package org.jboss.seam.core;

import java.util.Date;

import javax.ejb.Local;
import javax.ejb.Timer;
import javax.interceptor.InvocationContext;

import org.jboss.seam.Component;

@Local
public interface LocalDispatcher
{
   public Timer scheduleInvocation(InvocationContext invocation, Component component);
   public Timer scheduleEvent(String type, Long duration, Date expiration, Long intervalDuration);
   
}
