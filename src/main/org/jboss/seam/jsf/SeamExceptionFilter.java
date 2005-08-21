//$Id$
package org.jboss.seam.jsf;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.jboss.logging.Logger;

public class SeamExceptionFilter implements Filter
{
   
   private static Logger log = Logger.getLogger(SeamExceptionFilter.class);

   public void init(FilterConfig cfg) throws ServletException {}

   public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) 
         throws IOException, ServletException
   {
      try
      {
         chain.doFilter(request, response);
      }
      catch (Exception e)
      {
         log.error("uncaught exception handled by Seam"+ e.getMessage());
         rollbackAfterException();
         endWebRequestAfterException(request);
         throw new ServletException(e);
      }
   }

   private void endWebRequestAfterException(ServletRequest request)
   {
      try {
         SeamPhaseListener.endWebRequest( (HttpServletRequest) request);
      }
      catch (Exception ee)
      {
         log.error("could not destroy contexts", ee);
      }
   }

   private void rollbackAfterException()
   {
      try {
         if ( SeamTransactionPhaseListener.isTransactionActiveOrMarkedRollback() )
         {
            log.info("killing transaction");
            SeamTransactionPhaseListener.getUserTransaction().rollback();
         }
      }
      catch (Exception te)
      {
         log.error("could not roll back transaction", te);
      }
   }

   public void destroy() {}

}
