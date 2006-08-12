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

import ubic.basecode.util.StringUtil;
import ubic.gemma.loader.genome.gene.ncbi.model.NCBIGene2Accession;
import ubic.gemma.loader.util.parser.BasicLineParser;

/**
 * Class to parse the NCBI gene2accession files. Results are stored in a "Source domain object", not a Gemma Gene.
 * 
 * @author pavlidis
 * @version $Id$
 */
public class NcbiGene2AccessionParser extends BasicLineParser {

    /**
     * 
     */
    private static final int NCBI_GENE2ACCESSION_FIELDS_PER_ROW = 13;

    Collection<NCBIGene2Accession> results = new HashSet<NCBIGene2Accession>();

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.loader.loaderutils.LineParser#parseOneLine(java.lang.String)
     */
    public Object parseOneLine( String line ) {
        String[] fields = StringUtil.splitPreserveAllTokens( line, '\t' );

        if ( fields.length != NCBI_GENE2ACCESSION_FIELDS_PER_ROW ) {
            throw new IllegalArgumentException( "Line is not in the right format: has " + fields.length
                    + " fields, expected " + NCBI_GENE2ACCESSION_FIELDS_PER_ROW );
        }

        NCBIGene2Accession newGene = new NCBIGene2Accession();
        try {
            newGene.setTaxId( Integer.parseInt( fields[0] ) );
            newGene.setGeneId( fields[1] );
            newGene.setStatus( fields[2].equals( "-" ) ? null : fields[2] );
            newGene.setRNANucleotideAccession( fields[3].equals( "-" ) ? null : fields[3] );
            newGene.setRNANucleotideGI( fields[4].equals( "-" ) ? null : fields[4] );
            newGene.setProteinAccession( fields[5].equals( "-" ) ? null : fields[5] );
            newGene.setProteinGI( fields[6].equals( "-" ) ? null : fields[6] );
            newGene.setGenomicNucleotideAccession( fields[7].equals( "-" ) ? null : fields[7] );
            newGene.setGenomicNucleotideGI( fields[8].equals( "-" ) ? null : fields[8] );
            newGene.setStartPosition( fields[9].equals( "-" ) ? -1 : Integer.parseInt( fields[9] ) );
            newGene.setEndPosition( fields[10].equals( "-" ) ? -1 : Integer.parseInt( fields[10] ) );
            newGene.setOrientation( fields[11].equals( "?" ) ? null : fields[11] );
        } catch ( NumberFormatException e ) {
            throw new RuntimeException( e );
        }
        return newGene;
    }

    @Override
    protected void addResult( Object obj ) {
        results.add( ( NCBIGene2Accession ) obj );

    }

    @Override
    public Collection<NCBIGene2Accession> getResults() {
        return results;
    }

}
