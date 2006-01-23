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
package edu.columbia.gemma.loader.genome.gene;

import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import baseCode.util.StringUtil;

import edu.columbia.gemma.genome.Gene;
import edu.columbia.gemma.genome.GeneDao;
import edu.columbia.gemma.genome.gene.GeneProductDao;
import edu.columbia.gemma.genome.gene.GeneProduct;
import edu.columbia.gemma.genome.gene.GeneProductType;
import edu.columbia.gemma.loader.loaderutils.BasicLineParser;

/**
 * Class to parse a file of literature associations. Format: (read whole row)
 * 
 * <pre>
 *   taxon\t ncbi_gene_id\t ncbi_prot_id
 * </pre>
 * 
 * <hr>
 * 
 * @author anshu
 * @version $Id$
 */
public class ProteinFileParser extends BasicLineParser {

    private static Log log = LogFactory.getLog( ProteinFileParser.class.getName() );

    public static final int FIELDS_PER_ROW = 3;
    public static final int PERSIST_CONCURRENTLY = 1;
    public static final int DO_NOT_PERSIST_CONCURRENTLY = 0;
    public static final int PERSIST_DEFAULT = 0;

    private int mPersist = PERSIST_DEFAULT;
    private GeneDao geneDao;
    private GeneProductDao gpDao;

    public ProteinFileParser( int persistType, GeneDao gdao, GeneProductDao pdao ) {
        this.mPersist = persistType;
        this.geneDao = gdao;
        this.gpDao = pdao;
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.columbia.gemma.loader.loaderutils.LineParser#parseOneLine(java.lang.String)
     */
    @SuppressWarnings("unchecked")
    public Object parseOneLine( String line ) {
        log.debug( line );
        String[] fields = StringUtil.splitPreserveAllTokens( line, '\t' );

        if ( fields.length != FIELDS_PER_ROW ) {
            throw new IllegalArgumentException( "Line is not in the right format: has " + fields.length
                    + " fields, expected " + FIELDS_PER_ROW );
        }

        Collection<Gene> c;
        GeneProduct gp = GeneProduct.Factory.newInstance();
        Gene g1 = null;
        Integer id = null;
        try {
            id = new Integer( fields[2] );
            c = geneDao.findByNcbiId( id.intValue() );
            if ( ( c != null ) && ( c.size() == 1 ) ) {
                g1 = ( c.iterator() ).next();
            } else
                throw new Exception( "gene " + id + " not found. Entry skipped." );

            if ( ( fields[3] ).startsWith( "NM_" ) ) {
                gp.setType( GeneProductType.RNA );
            } else {
                gp.setType( GeneProductType.PROTEIN );
            }
            gp.setGene( g1 );
            gp.setNcbiId( fields[3] );
            gp.setName( fields[3] );
            gp.setDescription( fields[3] );

            if ( mPersist == PERSIST_CONCURRENTLY ) {
                gpDao.create( gp ); // FIXME parser should not be persisting.
            }
        } catch ( Exception e ) {
            log.error( e, e );
        }
        return null;
    }

    /**
     * 
     */
    public void removeAll() {
        Collection col = gpDao.loadAll();
        gpDao.remove( col );
    }

}