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
package ubic.gemma.loader.association;

import java.util.Collection;
import java.util.HashSet;

import org.apache.commons.lang.StringUtils;

import ubic.gemma.loader.util.parser.BasicLineParser;
import ubic.gemma.model.association.LiteratureAssociation;
import ubic.gemma.model.association.LiteratureAssociationDao;
import ubic.gemma.model.association.LiteratureAssociationImpl;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.common.description.ExternalDatabaseDao;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.GeneDao;

/**
 * Class to parse a file of literature associations. Format: (read whole row)
 * 
 * <pre>
 *  g1_dbase\t    gl_name\t   g1_ncbiid\t g2_dbase\t    g2_name\t   g2_ncbiid\t  action\t    count\t database
 * </pre>
 * 
 * @author anshu
 * @version $Id$
 */

public class LitAssociationFileParser extends BasicLineParser {

    private Collection<LiteratureAssociation> results = new HashSet<LiteratureAssociation>();

    public static final int LIT_ASSOCIATION_FIELDS_PER_ROW = 9;
    public static final int PERSIST_CONCURRENTLY = 1;
    public static final int DO_NOT_PERSIST_CONCURRENTLY = 0;
    public static final int PERSIST_DEFAULT = 0;

    private GeneDao geneDao;
    private LiteratureAssociationDao laDao;
    private ExternalDatabaseDao dbDao;

    public LitAssociationFileParser( GeneDao gdao, LiteratureAssociationDao ldao, ExternalDatabaseDao dDao ) {
        this.geneDao = gdao;
        this.laDao = ldao;
        this.dbDao = dDao;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.loader.loaderutils.LineParser#parseOneLine(java.lang.String)
     */
    @SuppressWarnings("unchecked")
    public Object parseOneLine( String line ) {
        log.debug( line );
        String[] fields = StringUtils.splitPreserveAllTokens( line, '\t' );

        if ( fields.length != LIT_ASSOCIATION_FIELDS_PER_ROW ) {
            throw new IllegalArgumentException( "Line is not in the right format: has " + fields.length
                    + " fields, expected " + LIT_ASSOCIATION_FIELDS_PER_ROW );
        }

        Collection<Gene> c;
        LiteratureAssociation assoc = new LiteratureAssociationImpl();
        Gene g1 = null;
        Gene g2 = null;
        String id = null;
        ExternalDatabase db;
        try {
            id = fields[1];
            c = geneDao.findByNcbiId( id );

            if ( ( c != null ) && ( c.size() == 1 ) ) {
                g1 = ( c.iterator() ).next();
            } else {
                throw new Exception( "gene " + id + " not found. Entry skipped." );
            }

            id = fields[4];
            c = geneDao.findByNcbiId( id );

            if ( ( c != null ) && ( c.size() == 1 ) ) {
                g2 = ( c.iterator() ).next();
            } else {
                throw new Exception( "gene " + id + " not found. Entry skipped." );
            }

            assoc.setFirstGene( g1 );
            assoc.setSecondGene( g2 );
            assoc.setAction( fields[6] );
            assoc.setNumberOfMentions( new Integer( Integer.parseInt( fields[7] ) ) );
            // db = ExternalDatabase.Factory.newInstance();
            // db.setName( fields[8] );
            // db = dbDao.findOrCreate( db );
            // // db=dbDao.findByName(fields[8]); //calls fior external db to be pre-loaded
            // assoc.setSource( db );

            // if ( mPersist == PERSIST_CONCURRENTLY ) {
            // laDao.create( fields[6], g1, new Integer( fields[7] ), g2 ); // FIXME parser should not create
            // }
        } catch ( Exception e ) {
            log.error( e, e );
        }
        return null;
    }

    /**
     * 
     */
    public void removeAll() {
        Collection col = laDao.loadAll();
        laDao.remove( col );
    }

    @Override
    protected void addResult( Object obj ) {
        results.add( ( LiteratureAssociation ) obj );

    }

    @Override
    public Collection<LiteratureAssociation> getResults() {
        return results;
    }

}
