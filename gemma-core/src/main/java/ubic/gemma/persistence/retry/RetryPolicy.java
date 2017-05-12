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

import java.util.Map;

import org.springframework.retry.RetryContext;
import org.springframework.retry.policy.SimpleRetryPolicy;

/**
 * @author paul
 * @version $Id$
 */
public class RetryPolicy extends SimpleRetryPolicy {

    private RetryExceptionCauseClassifier classifier = new RetryExceptionCauseClassifier();

    public RetryPolicy() {
        super();
    }

    public RetryPolicy( int maxAttempts, Map<Class<? extends Throwable>, Boolean> retryableExceptions ) {
        this.setMaxAttempts( maxAttempts );
        this.classifier = new RetryExceptionCauseClassifier( retryableExceptions );
    }

    /*
     * Have to override so we use our classifier. (non-Javadoc)
     * 
     * @see org.springframework.retry.policy.SimpleRetryPolicy#canRetry(org.springframework.retry.RetryContext)
     */
    @Override
    public boolean canRetry( RetryContext context ) {
        Throwable t = context.getLastThrowable();
        return ( t == null || classifier.classify( t ) ) && context.getRetryCount() < getMaxAttempts();
    }

}
