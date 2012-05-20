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

import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.classify.BinaryExceptionClassifier;

/**
 * Check the cause as well as the exception itself. Only allows filtering to include, not exclude (that is, the default
 * is 'false'). Sorry.
 * 
 * @author paul
 * @version $Id$
 */
public class RetryExceptionCauseClassifier extends BinaryExceptionClassifier {

    private static Log log = LogFactory.getLog( RetryExceptionCauseClassifier.class );

    /**
     * @param retryableExceptions
     */
    public RetryExceptionCauseClassifier( Map<Class<? extends Throwable>, Boolean> retryableExceptions ) {
        super( retryableExceptions );
    }

    /**
    
     */
    public RetryExceptionCauseClassifier() {
        super( false );
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.classify.SubclassClassifier#classify(java.lang.Object)
     */
    @Override
    public Boolean classify( Throwable classifiable ) {
        if ( classifiable == null ) {
            return this.getDefault();
        }

        if ( super.classify( classifiable ) ) {
            log.info( "Can retry after " + classifiable.getClass() );
            return true;
        }

        Throwable c = classifiable.getCause();

        while ( c != null ) {
            if ( super.classify( c ) ) {
                log.info( "Can retry after cause " + c.getClass() );
                return true; // we assume the default=false, so this is true.
            }
            c = c.getCause();
        }

        return this.getDefault();
    }
}
