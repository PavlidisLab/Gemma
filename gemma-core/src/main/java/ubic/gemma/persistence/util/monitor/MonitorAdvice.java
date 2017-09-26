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
package ubic.gemma.persistence.util.monitor;

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
 */
@Aspect
public class MonitorAdvice {

    private static final Log log = LogFactory.getLog( MonitorAdvice.class );

    @Around("@annotation(ubic.gemma.persistence.util.monitor.Monitored)")
    public Object profile( ProceedingJoinPoint pjp ) throws Throwable {

        StopWatch stopWatch = new StopWatch();

        stopWatch.start();
        Object retVal = pjp.proceed();

        stopWatch.stop();

        log.info( pjp.getSignature().toString() + " took " + stopWatch.getLastTaskTimeMillis() + "ms." );

        return retVal;

    }

}
