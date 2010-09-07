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

import org.apache.commons.logging.Log;

import org.apache.commons.logging.LogFactory;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.util.StopWatch;

/**
 * Interceptor for monitoring methods.
 * 
 * @author paul
 * @version $Id$
 */
@Aspect
public class MonitorAdvice {

    private static Log log = LogFactory.getLog( MonitorAdvice.class );

    //@Autowired
    //private SessionFactory sessionFactory;

    /**
     * Entry point.
     * 
     * @param pjp
     * @param monitored
     * @return
     * @throws Throwable
     */
    @Around("@annotation(ubic.gemma.util.monitor.Monitored)")
    public Object profile( ProceedingJoinPoint pjp ) throws Throwable {
        //long cacheHitsBefore = sessionFactory.getStatistics().getQueryCacheHitCount();

        StopWatch stopWatch = new StopWatch();
//        try {
            stopWatch.start();
            Object retVal =  pjp.proceed();
//        } finally {
            stopWatch.stop();
            // if ( stopWatch.getTotalTimeMillis() > monitored.minTimeToReport() ) {
//            long cacheHitsAfter = sessionFactory.getStatistics().getQueryCacheHitCount();
//            long cacheHits = cacheHitsAfter - cacheHitsBefore;
//            String chs = "";
//            if ( cacheHits > 0 ) {
//                chs = " - query cache hit"; // this hit could have been from another thread...
//            }
            log.warn( pjp.getSignature().toString()+ " took " + stopWatch.getLastTaskTimeMillis() + "ms." );            
            // }            
            return retVal;
//        }
    }

    /**
     * @param sessionFactory the sessionFactory to set
     */
//    public void setSessionFactory( SessionFactory sessionFactory ) {
//        this.sessionFactory = sessionFactory;
//    }

}
