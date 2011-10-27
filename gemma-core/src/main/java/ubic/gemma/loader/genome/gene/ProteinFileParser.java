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
package ubic.gemma.loader.genome.gene;

import java.util.Collection;
import java.util.HashSet;

import org.apache.commons.lang.StringUtils;

import ubic.gemma.loader.util.parser.BasicLineParser;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.GeneDao;
import ubic.gemma.model.genome.gene.GeneProduct;
import ubic.gemma.model.genome.gene.GeneProductDao;
import ubic.gemma.model.genome.gene.GeneProductType;

/**
 * Class to parse a file of literature associations. Format: (read whole row)
 * 
 * <pre>
 *             taxon\t ncbi_gene_id\t ncbi_prot_id
 * </pre>
 * <hr>
 * 
 * @author anshu
 * @version $Id$
 * @deprecated
 */
@Deprecated
public class ProteinFileParser extends BasicLineParser<GeneProduct> {

    public static final int FIELDS_PER_ROW = 3;

    private GeneDao geneDao;
    private GeneProductDao gpDao;

    private Collection<GeneProduct> results = new HashSet<GeneProduct>();

    public ProteinFileParser( GeneDao gdao, GeneProductDao pdao ) {
        this.geneDao = gdao;
        this.gpDao = pdao;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.loader.loaderutils.LineParser#parseOneLine(java.lang.String)
     */
    public GeneProduct parseOneLine( String line ) {
        log.debug( line );
        String[] fields = StringUtils.splitPreserveAllTokens( line, '\t' );

        if ( fields.length != FIELDS_PER_ROW ) {
            throw new IllegalArgumentException( "Line is not in the right format: has " + fields.length
                    + " fields, expected " + FIELDS_PER_ROW );
        }

        GeneProduct gp = GeneProduct.Factory.newInstance();
        Gene g1 = null;
        String id = null;
        id = fields[2];
        g1 = geneDao.findByNcbiId( Integer.parseInt( id ) ); // FIXME this should only parse, not use the database.
        if ( g1 == null ) {
            throw new RuntimeException( "gene " + id + " not found. Entry skipped." );
        }
        if ( ( fields[3] ).startsWith( "NM_" ) ) {
            gp.setType( GeneProductType.RNA );
        } else {
            gp.setType( GeneProductType.PROTEIN );
        }
        gp.setGene( g1 );
        gp.setNcbiGi( fields[3] );
        gp.setName( fields[3] );
        gp.setDescription( fields[3] );

        return null;
    }

    /**
     * 
     */
    public void removeAll() {
        Collection<? extends GeneProduct> col = gpDao.loadAll();
        gpDao.remove( col );
    }

    @Override
    protected void addResult( GeneProduct obj ) {
        results.add( obj );

    }

    @Override
    public Collection<GeneProduct> getResults() {
        return results;
    }

}