/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat, Inc., and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.seam.drools.interceptor.flow;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

import org.drools.runtime.StatefulKnowledgeSession;
import org.drools.runtime.process.ProcessInstance;
import org.jboss.seam.drools.annotations.flow.AbortProcess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@AbortProcess
@Interceptor
public class AbortProcessInterceptor {
    @Inject
    BeanManager manager;

    @Inject
    @Any
    Instance<StatefulKnowledgeSession> ksessionSource;

    private static final Logger log = LoggerFactory.getLogger(AbortProcessInterceptor.class);

    @AroundInvoke
    public Object abortProcess(InvocationContext ctx) throws Exception {

        String processName = null;

        Annotation[] methodAnnotations = ctx.getMethod().getAnnotations();
        List<Annotation> annotationTypeList = new ArrayList<Annotation>();

        for (Annotation nextAnnotation : methodAnnotations) {
            if (manager.isQualifier(nextAnnotation.annotationType())) {
                annotationTypeList.add(nextAnnotation);
            }
            if (manager.isInterceptorBinding(nextAnnotation.annotationType())) {
                if (nextAnnotation instanceof AbortProcess) {
                    processName = ((AbortProcess) nextAnnotation).value();
                }
            }
        }

        StatefulKnowledgeSession ksession = ksessionSource.select((Annotation[]) annotationTypeList.toArray(new Annotation[annotationTypeList.size()])).get();
        if (ksession != null) {
            Object retObj = ctx.proceed();
            if (processName != null && processName.length() > 0) {
                Iterator<ProcessInstance> iter = ksession.getProcessInstances().iterator();
                while (iter.hasNext()) {
                    ProcessInstance pi = iter.next();
                    if (pi.getProcessName().equals(processName)) {
                        ksession.abortProcessInstance(pi.getId());
                    }
                }
            } else {
                log.info("Invalid process name: " + processName);
            }
            return retObj;
        } else {
            log.info("Could not obtain StatefulKnowledgeSession.");
            return ctx.proceed();
        }
    }

}
