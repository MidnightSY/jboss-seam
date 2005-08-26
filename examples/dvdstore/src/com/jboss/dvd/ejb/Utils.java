/*
 * JBoss, Home of Professional Open Source
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */ 
package com.jboss.dvd.ejb;

import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import javax.naming.InitialContext;

import java.text.MessageFormat;

import javax.faces.context.FacesContext; 
import javax.faces.application.FacesMessage; 

public class Utils {
    public static void warnUser(String id, Object params[]) {
        FacesContext ctx = FacesContext.getCurrentInstance();
        ctx.addMessage(null, getMessage(id, params, FacesMessage.SEVERITY_ERROR));
    }

    // move somewhere else
    public static FacesMessage getMessage(String messageId, 
                                          Object params[], 
                                          FacesMessage.Severity severity)     
    { 
        FacesContext facesContext = FacesContext.getCurrentInstance(); 
        String bundleName = facesContext.getApplication().getMessageBundle(); 
        if (bundleName != null) {
            String summary = null; 
            String detail = null; 
            Locale locale = facesContext.getViewRoot().getLocale(); 
            ResourceBundle bundle = 
                ResourceBundle.getBundle(bundleName, 
                                         locale, 
                                         Thread.currentThread().getContextClassLoader());
            try { 
                summary = bundle.getString(messageId); 
                detail = bundle.getString(messageId + "_detail"); 
            } catch (MissingResourceException e) {
            } 

            if (summary != null) { 
                MessageFormat mf = null; 
                if (params != null) { 
                    mf = new MessageFormat(summary, locale); 
                    summary = mf.format(params, new StringBuffer(), null).toString(); 
                } 
                if (detail != null && params != null) { 
                    mf.applyPattern(detail); 
                    detail = mf.format(params, new StringBuffer(), null).toString(); 
                } 
                return (new FacesMessage(severity, summary, detail)); 
            } 
        } 
        return new FacesMessage(severity, 
                                "!! key " + messageId + " not found !!", 
                                null); 
    } 


}
