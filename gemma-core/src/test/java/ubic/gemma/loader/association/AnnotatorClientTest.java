/*
 * The Gemma project
 * 
 * Copyright (c) 2013 University of British Columbia
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
package ubic.gemma.loader.association;

import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.HashSet;

import org.junit.Test;

import ubic.basecode.ncboAnnotator.AnnotatorClient;
import ubic.basecode.ncboAnnotator.AnnotatorResponse;

/**
 * @author Paul
 * @version $Id$
 */
public class AnnotatorClientTest {

    @Test
    public void test() throws Exception {

        Collection<Long> ontologiesToUse = new HashSet<Long>();
        ontologiesToUse.add( 1009l );
        ontologiesToUse.add( 1125l );
        AnnotatorClient client = new AnnotatorClient( ontologiesToUse );
        Collection<AnnotatorResponse> results = client.findTerm( "cancer" );
        assertTrue( results.size() > 0 );
    }

}
