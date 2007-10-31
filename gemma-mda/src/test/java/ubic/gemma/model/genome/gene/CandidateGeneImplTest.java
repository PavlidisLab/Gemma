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
package ubic.gemma.model.genome.gene;

import junit.framework.TestCase;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;

/**
 * @author daq2101
 * @version $Id$
 */
public class CandidateGeneImplTest extends TestCase {
    private final Log log = LogFactory.getLog( CandidateGeneImplTest.class );

    public void testSetCandidateGene() {
        log.debug( "test setting of CandidateGene" );
        // SET UP
        Gene g = null;
        Taxon t = null;

        t = Taxon.Factory.newInstance();
        t.setCommonName( "mouse" );
        t.setScientificName( "mouse" );

        g = Gene.Factory.newInstance();
        g.setName( "testmygene" );
        g.setOfficialSymbol( "foo" );
        g.setOfficialName( "testmygene" );
        g.setTaxon( t );

        // TEST
        CandidateGene cg = CandidateGene.Factory.newInstance();
        cg.setRank( new Integer( 1 ) );
        cg.setGene( g );

        assertTrue( cg.getGene().equals( g ) );
    }

}
