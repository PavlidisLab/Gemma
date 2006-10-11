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
package ubic.gemma.loader.expression.arrayDesign;

import java.util.Collection;

import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.TaxonService;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.util.ConfigUtils;

/**
 * @author pavlidis
 * @version $Id$
 */
public class ArrayDesignProbeMapperServiceIntegrationTest extends AbstractArrayDesignProcessingTest {

    /**
     * Test method for
     * {@link ubic.gemma.loader.expression.arrayDesign.ArrayDesignProbeMapperService#processArrayDesign(ubic.gemma.model.expression.arrayDesign.ArrayDesign, ubic.gemma.model.genome.Taxon)}.
     */
    public final void testProcessArrayDesign() throws Exception {
        ArrayDesignSequenceProcessingService app = ( ArrayDesignSequenceProcessingService ) getBean( "arrayDesignSequenceProcessingService" );
        // finally the real business. There are 243 sequences on the array, but one is not going to be found ()
        Collection<BioSequence> res = app.processArrayDesign( ad, new String[] { "testblastdb", "testblastdbPartTwo" },
                ConfigUtils.getString( "gemma.home" ) + "/gemma-core/src/test/resources/data/loader/genome/blast" );

        ArrayDesignProbeMapperService arrayDesignProbeMapperService = ( ArrayDesignProbeMapperService ) this
                .getBean( "arrayDesignProbeMapperService" );
        //
        arrayDesignProbeMapperService.setIdentityThreshold( 0.5 );
        arrayDesignProbeMapperService.setScoreThreshold( 0.5 );
        arrayDesignProbeMapperService.setBlatScoreThreshold( 0.5 );

        // see also the ArrayDesignSequenceAlignementTest.
        Taxon taxon = ( ( TaxonService ) getBean( "taxonService" ) ).findByScientificName( "Homo sapiens" );
        ArrayDesignSequenceAlignmentService aligner = ( ArrayDesignSequenceAlignmentService ) getBean( "arrayDesignSequenceAlignmentService" );
        aligner.processArrayDesign( ad, taxon );

        // real stuff.
        arrayDesignProbeMapperService.processArrayDesign( ad, taxon );

    }

}
