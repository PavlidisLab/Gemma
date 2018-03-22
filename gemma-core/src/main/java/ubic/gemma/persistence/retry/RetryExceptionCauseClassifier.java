/*
 * The Gemma project
 *
 * Copyright (c) 2012 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package ubic.gemma.persistence.retry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.classify.BinaryExceptionClassifier;

import java.util.Map;

/**
 * Check the cause as well as the exception itself. Only allows filtering to include, not exclude (that is, the default
 * is 'false'). Sorry.
 *
 * @author paul
 */
class RetryExceptionCauseClassifier extends BinaryExceptionClassifier {

    private static final Log log = LogFactory.getLog( RetryExceptionCauseClassifier.class );

    RetryExceptionCauseClassifier( Map<Class<? extends Throwable>, Boolean> retryableExceptions ) {
        super( retryableExceptions );
    }

    RetryExceptionCauseClassifier() {
        super( false );
    }

    @Override
    public Boolean classify( Throwable classifiable ) {
        if ( classifiable == null ) {
            return this.getDefault();
        }

        if ( super.classify( classifiable ) ) {
            RetryExceptionCauseClassifier.log.info( "Can retry after " + classifiable.getClass().getName() );
            return true;
        }

        Throwable c = classifiable.getCause();

        while ( c != null ) {
            if ( super.classify( c ) ) {
                RetryExceptionCauseClassifier.log.info( "Can retry after cause " + c.getClass().getName() );
                return true; // we assume the default=false, so this is true.
            }
            c = c.getCause();
        }

        RetryExceptionCauseClassifier.log
                .debug( " **** could not retry after: " + classifiable.getClass().getSimpleName() + ": " + classifiable
                        .getMessage(), classifiable );

        return this.getDefault();
    }
}
