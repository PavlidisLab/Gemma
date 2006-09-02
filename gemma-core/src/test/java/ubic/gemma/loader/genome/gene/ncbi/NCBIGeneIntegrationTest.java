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
import java.util.HashSet;

import ubic.gemma.loader.genome.gene.ncbi.model.NCBIGene2Accession;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.testing.BaseTransactionalSpringContextTest;
import ubic.gemma.util.ConfigUtils;

/**
 * @author pavlidis
 * @version $Id$
 */
public class NCBIGeneIntegrationTest extends BaseTransactionalSpringContextTest {

    @SuppressWarnings("unchecked")
    public void testFetchAndLoad() throws Exception {
        NcbiGeneDomainObjectGenerator sdog = new NcbiGeneDomainObjectGenerator();

        String geneInfoTestFile = "/gemma-core/src/test/resources/data/loader/genome/gene/gene_info.sample.gz";
        String gene2AccTestFile = "/gemma-core/src/test/resources/data/loader/genome/gene/gene2accession.sample.gz";

        String basePath = ConfigUtils.getString( "gemma.home" );

        Collection<NCBIGene2Accession> results = sdog.generateLocal( basePath + geneInfoTestFile, basePath
                + gene2AccTestFile );

        Collection<NCBIGene2Accession> smallSample = new HashSet<NCBIGene2Accession>();
        int i = 0;
        for ( NCBIGene2Accession gene : results ) {
            smallSample.add( gene );
            i++;
            if ( i > 10 ) break;
        }
        results = null;

        NcbiGeneConverter ngc = new NcbiGeneConverter();
        log.info( "Converting..." );
        Collection<Gene> gemmaObj = ( Collection<Gene> ) ngc.convert( smallSample );

        Collection<Gene> persistedObj = ( Collection<Gene> ) persisterHelper.persist( gemmaObj );

        for ( Gene gene : persistedObj ) {
            assertTrue( gene.getId() != null );
        }

    }
}
