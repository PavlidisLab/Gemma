/*
 * The Gemma_sec1 project
 *
 * Copyright (c) 2009 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package ubic.gemma.core.scheduler;

import org.springframework.scheduling.quartz.MethodInvokingJobDetailFactoryBean;
import org.springframework.security.concurrent.DelegatingSecurityContextCallable;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.util.Assert;

import java.lang.reflect.InvocationTargetException;

/**
 * Specialization of Spring task-running support so task threads have secure context (without using MODE_GLOBAL!). The
 * thread where Quartz is being run is authenticated as GROUP_AGENT.
 *
 * @author paul
 * @see SecureQuartzJobBean
 */
public class SecureMethodInvokingJobDetailFactoryBean extends MethodInvokingJobDetailFactoryBean {

    private final SecurityContext securityContext;

    public SecureMethodInvokingJobDetailFactoryBean( SecurityContext securityContext ) {
        Assert.notNull( securityContext, "A security context must be provided." );
        this.securityContext = securityContext;
    }

    @Override
    public Object invoke() throws InvocationTargetException, IllegalAccessException {
        try {
            return DelegatingSecurityContextCallable.create( super::invoke, securityContext ).call();
        } catch ( InvocationTargetException | RuntimeException e ) {
            throw e;
        } catch ( Exception e ) {
            throw new RuntimeException( e );
        }
    }
}
