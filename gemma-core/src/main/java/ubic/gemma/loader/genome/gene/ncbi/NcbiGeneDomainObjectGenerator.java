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

import java.io.IOException;
import java.util.Collection;

import ubic.gemma.loader.genome.gene.ncbi.model.NCBIGene2Accession;
import ubic.gemma.loader.genome.gene.ncbi.model.NCBIGeneInfo;
import ubic.gemma.loader.util.sdo.SourceDomainObjectGenerator;
import ubic.gemma.model.common.description.LocalFile;

/**
 * Combines information from the gene2accession and gene_info files from NCBI Gene.
 * 
 * @author pavlidis
 * @version $Id$
 */
public class NcbiGeneDomainObjectGenerator implements SourceDomainObjectGenerator {

    /**
     * @return a collection of NCBIGene2Accession
     * @see ubic.gemma.loader.loaderutils.SourceDomainObjectGenerator#generate(java.lang.String)
     */
    public Collection<NCBIGene2Accession> generate() {
        return this.generate( null );
    }

    /**
     * @param accession NOT USED HERE
     * @return a collection of NCBIGene2Accession
     * @see ubic.gemma.loader.loaderutils.SourceDomainObjectGenerator#generate(java.lang.String)
     */
    @SuppressWarnings("unused")
    public Collection<NCBIGene2Accession> generate( String accession ) {

        NCBIGeneFileFetcher fetcher = new NCBIGeneFileFetcher();
        LocalFile geneInfoFile = fetcher.fetch( "gene_info" ).iterator().next();
        LocalFile gene2AccessionFile = fetcher.fetch( "gene2accession" ).iterator().next();

        NcbiGeneInfoParser infoParser = new NcbiGeneInfoParser();
        NcbiGene2AccessionParser accParser = new NcbiGene2AccessionParser();
        try {
            infoParser.parse( geneInfoFile.asFile() );
            accParser.parse( gene2AccessionFile.asFile() );

            Collection<NCBIGene2Accession> ncbiGenes = accParser.getResults();

            for ( NCBIGene2Accession o : ncbiGenes ) {
                NCBIGeneInfo info = infoParser.get( o.getGeneId() );
                o.setInfo( info );
            }

            return ncbiGenes;

        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }

    }
}
