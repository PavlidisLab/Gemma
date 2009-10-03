/*
 * The Gemma project
 * 
 * Copyright (c) 2008 University of British Columbia
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
package ubic.gemma.util.monitor;

import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.logging.Log;
import org.hibernate.SessionFactory;
import org.springframework.aop.interceptor.PerformanceMonitorInterceptor;
import org.springframework.util.StopWatch;

/**
 * Interceptor for monitoring methods.
 * 
 * @author paul
 * @version $Id$
 */
public class MonitorAdvice extends PerformanceMonitorInterceptor {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    private static final int DEFAULT_THRESHOLD = 1000;

    private int threshold = DEFAULT_THRESHOLD;

    private SessionFactory sessionFactory;

    public void setSessionFactory( SessionFactory sessionFactory ) {
        this.sessionFactory = sessionFactory;
    }

    public void setThreshold( int threshold ) {
        this.threshold = threshold;
    }

    @Override
    protected Object invokeUnderTrace( MethodInvocation invocation, Log logger ) throws Throwable {
        String name = createInvocationTraceName( invocation );
        StopWatch stopWatch = new StopWatch( name );
        long cacheHitsBefore = sessionFactory.getStatistics().getQueryCacheHitCount();
        stopWatch.start( name );
        try {
            return invocation.proceed();
        } finally {
            stopWatch.stop();
            if ( stopWatch.getTotalTimeMillis() > threshold ) {
                long cacheHitsAfter = sessionFactory.getStatistics().getQueryCacheHitCount();
                long cacheHits = cacheHitsAfter - cacheHitsBefore;
                String chs = "";
                if ( cacheHits > 0 ) {
                    chs = " - cache hit";
                }
                logger.info( stopWatch.shortSummary() + chs );
            }
        }
    }

    @Override
    protected boolean isLogEnabled( Log logger ) {
        return logger.isInfoEnabled();
    }

}
