/*
 * The Gemma project
 * 
 * Copyright (c) 2006 University of British Columbia
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
package ubic.gemma.loader.genome.gene.ncbi;

import java.util.Collection;
import java.util.concurrent.ExecutionException;

import ubic.gemma.loader.util.persister.PersisterHelper;
import ubic.gemma.testing.BaseTransactionalSpringContextTest;

/**
 * @author pavlidis
 * @version $Id$
 */
public class NCBIGeneIntegrationTest extends BaseTransactionalSpringContextTest {

    public void testFetchAndLoad() throws Exception {
        NcbiGeneDomainObjectGenerator sdog = new NcbiGeneDomainObjectGenerator();
        try {
            Collection<Object> results = sdog.generate( null );
            NcbiGeneConverter ngc = new NcbiGeneConverter();
            Collection<Object> gemmaObj = ngc.convert( results );
            PersisterHelper persisterHelper = ( PersisterHelper ) this.getBean( "persisterHelper" );
            persisterHelper.persist( gemmaObj );
        } catch ( Exception e ) {
            if ( e.getCause() instanceof ExecutionException ) {
                log.warn( "Failed to get file -- skipping rest of test" );
                return;
            }
            throw ( e );
        }

    }
}
