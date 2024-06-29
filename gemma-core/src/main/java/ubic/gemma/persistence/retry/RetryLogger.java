/*
 * The Gemma project
 *
 * Copyright (c) 2012 University of British Columbia
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

package ubic.gemma.persistence.retry;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.listener.RetryListenerSupport;
import org.springframework.stereotype.Component;
import ubic.gemma.core.lang.Nullable;

/**
 * Provide logging when an operation has failed and is being retried. This would not be needed if there was better
 * logging control over the default RetryContext.
 *
 * @author paul
 */
@Component
public class RetryLogger extends RetryListenerSupport {

    private static final Log log = LogFactory.getLog( RetryLogger.class );

    @Override
    public <T> void close( RetryContext context, RetryCallback<T> callback, @Nullable Throwable throwable ) {
        if ( context.getRetryCount() > 1 ) {
            if ( throwable == null ) {
                RetryLogger.log.info( String.format( "Retry was successful after %d attempts!", context.getRetryCount() ) );
            } else {
                // a full stacktrace is included here
                RetryLogger.log.error( String.format( String.format( "Retry failed after %d attempts.", context.getRetryCount() ) ), throwable );
            }
        }
    }

    @Override
    public <T> void onError( RetryContext context, RetryCallback<T> callback, Throwable throwable ) {
        if ( context.getRetryCount() > 0 ) {
            // only include a brief & specific stacktrace
            RetryLogger.log.warn( String.format( "Retry attempt #%d failed.", context.getRetryCount() ),
                    ExceptionUtils.getRootCause( throwable ) );
        }
    }
}
