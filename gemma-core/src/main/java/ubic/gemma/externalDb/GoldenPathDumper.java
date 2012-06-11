/*
 * The Gemma project
 * 
 * Copyright (c) 2006-2007 University of British Columbia
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
package ubic.gemma.externalDb;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashSet;
import java.util.concurrent.BlockingQueue;

import javax.sql.DataSource;

import org.springframework.jdbc.object.MappingSqlQuery;

import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.common.description.DatabaseType;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.model.genome.biosequence.PolymerType;
import ubic.gemma.model.genome.biosequence.SequenceType;

/**
 * Class to handle dumping data from Goldenpath into Gemma.
 * 
 * @author pavlidis
 * @version $Id$
 */
public class GoldenPathDumper extends GoldenPath {

    private static final String REF_GENE_TABLE_NAME = "refGene";
    private static final String ENSEMBL_TABLE_NAME = "ensGene";

    // these are provided for testing (switch off ones not using)
    private boolean DO_EST = true;
    private boolean DO_MRNA = true;
    private boolean DO_REFSEQ = true;

    public GoldenPathDumper( int port, String databaseName, String host, String user, String password )
            throws SQLException {
        super( port, databaseName, host, user, password );
    }

    public GoldenPathDumper( Taxon taxon ) {
        super( taxon );
    }

    public GoldenPathDumper() {
        super();
    }

    ExternalDatabase genbank;

    ExternalDatabase ensembl;

    /**
     * Get all ESTs and mRNAs for a taxon. This will return a very large collection.
     * 
     * @param limit per table, for testing purposes. Set to value <=0 to ignore.
     * @param queue to store the results in, to be consumed by another method.
     */
    public void dumpTranscriptBioSequences( int limit, BlockingQueue<BioSequence> queue ) {
        initGenbank();
        initEnsembl();

        double loadFactor = 0.5;

        int batchSize = ( int ) Math.floor( ( queue.remainingCapacity() + queue.size() ) * loadFactor );

        assert batchSize > 0;

        String limitSuffix = "";
        if ( limit > 0 ) {
            limitSuffix = "limit " + limit;
        } else {
            limitSuffix = " limit " + batchSize;
        }

        log.info( "starting ests" );
        int offset = 0;
        int numInput = 0;
        Collection<Integer> seen = new HashSet<Integer>();
        while ( DO_EST && !( limit > 0 && numInput >= limit ) ) {
            try {
                Collection<BioSequence> sequences = loadSequencesByQuery( "all_est", SequenceType.EST, limitSuffix
                        + " offset " + offset );
                if ( sequences.size() == 0 ) {
                    break;
                }

                Collection<BioSequence> toAdd = screenDuplicates( seen, sequences );

                numInput = addToQueue( queue, numInput, toAdd );
                offset += batchSize;
            } catch ( Exception e ) {
                log.info( e );
                break;
            }
        }

        log.info( "starting mrnas" );
        offset = 0;
        numInput = 0;
        while ( DO_MRNA && !( limit > 0 && numInput >= limit ) ) {
            try {
                Collection<BioSequence> sequences = loadSequencesByQuery( "all_mrna", SequenceType.mRNA, limitSuffix
                        + " offset " + offset );
                if ( sequences.size() == 0 ) {
                    break;
                }

                Collection<BioSequence> toAdd = screenDuplicates( seen, sequences );

                numInput = addToQueue( queue, numInput, toAdd );

                offset += batchSize;
            } catch ( Exception e ) {
                log.info( e );
                break;
            }
        }

        log.info( "starting refseq" );
        offset = 0;
        numInput = 0;
        while ( DO_REFSEQ && !( limit > 0 && numInput >= limit ) ) {
            try {
                Collection<BioSequence> sequences = loadRefseqByQuery( limitSuffix + " offset " + offset );
                if ( sequences.size() == 0 ) {
                    break;
                }
                Collection<BioSequence> toAdd = screenDuplicates( seen, sequences );
                numInput = addToQueue( queue, numInput, toAdd );

                offset += batchSize;
            } catch ( Exception e ) {
                log.info( e );
                break;
            }
        }

        log.info( "starting ensembl" ); // not for mouse.
        offset = 0;
        numInput = 0;
        while ( DO_REFSEQ && !( limit > 0 && numInput >= limit ) ) {
            try {
                Collection<BioSequence> sequences = loadEnsemblByQuery( limitSuffix + " offset " + offset );
                if ( sequences.size() == 0 ) {
                    break;
                }
                Collection<BioSequence> toAdd = screenDuplicates( seen, sequences );
                numInput = addToQueue( queue, numInput, toAdd );

                offset += batchSize;
            } catch ( Exception e ) {
                log.info( e ); // This will happen if we run on a genome that doesn't have the ensembl tracks.
                break;
            }
        }

    }

    /**
     * @param seen
     * @param sequences
     * @return
     */
    private Collection<BioSequence> screenDuplicates( Collection<Integer> seen, Collection<BioSequence> sequences ) {
        Collection<BioSequence> toAdd = new HashSet<BioSequence>();
        for ( BioSequence sequence : sequences ) {
            if ( seen.contains( sequence.getName().hashCode() ) ) {
                continue;
            }
            toAdd.add( sequence );
            seen.add( sequence.getName().hashCode() );
        }
        return toAdd;
    }

    /**
     * @param queue
     * @param numInput
     * @param sequences
     * @return
     * @throws InterruptedException
     */
    private int addToQueue( BlockingQueue<BioSequence> queue, int numInput, Collection<BioSequence> sequences )
            throws InterruptedException {
        for ( BioSequence sequence : sequences ) {
            queue.put( sequence );
            if ( ++numInput % 1000 == 0 ) {
                log.info( "Input " + numInput + " from goldenpath db" );
            }
        }
        return numInput;
    }

    /**
     * 
     */
    private void initGenbank() {
        genbank = ExternalDatabase.Factory.newInstance();
        genbank.setName( "Genbank" );
        genbank.setType( DatabaseType.SEQUENCE );
    }

    /**
     * 
     */
    private void initEnsembl() {
        genbank = ExternalDatabase.Factory.newInstance();
        genbank.setName( "Ensembl" );
        genbank.setType( DatabaseType.GENOME );
    }

    /**
     * @author paul
     * @version $Id$
     */
    private class BioSequenceMappingQuery extends MappingSqlQuery<BioSequence> {

        SequenceType type;

        public BioSequenceMappingQuery( DataSource ds, String table, SequenceType type, String limit ) {
            super( ds, "SELECT qName, qSize FROM " + table + " " + limit );
            this.type = type;
            compile();
        }

        @Override
        public BioSequence mapRow( ResultSet rs, int rowNumber ) throws SQLException {
            BioSequence bioSequence = BioSequence.Factory.newInstance();

            DatabaseEntry de = DatabaseEntry.Factory.newInstance();

            String name = rs.getString( "qName" );
            Long length = rs.getLong( "qSize" );
            bioSequence.setName( name );
            bioSequence.setLength( length );
            bioSequence.setIsApproximateLength( false );
            bioSequence.setPolymerType( PolymerType.DNA );
            bioSequence.setIsCircular( false );

            de.setAccession( name );
            de.setExternalDatabase( genbank );

            bioSequence.setType( type );
            bioSequence.setSequenceDatabaseEntry( de );

            return bioSequence;
        }

    }

    /**
     * @author paul
     * @version $Id$
     */
    private class BioSequenceRefseqMappingQuery extends MappingSqlQuery<BioSequence> {

        public BioSequenceRefseqMappingQuery( DataSource ds, String query ) {
            super( ds, query );
            compile();
        }

        @Override
        public BioSequence mapRow( ResultSet rs, int rowNumber ) throws SQLException {
            BioSequence bioSequence = BioSequence.Factory.newInstance();

            DatabaseEntry de = DatabaseEntry.Factory.newInstance();

            String name = rs.getString( "name" );
            bioSequence.setName( name );
            bioSequence.setPolymerType( PolymerType.DNA );
            bioSequence.setIsCircular( false );

            de.setAccession( name );
            de.setExternalDatabase( genbank );

            bioSequence.setType( SequenceType.REFSEQ );
            bioSequence.setSequenceDatabaseEntry( de );

            return bioSequence;
        }
    }

    /**
     * @author paul
     * @version $Id$
     */
    private class BioSequenceEnsemblMappingQuery extends MappingSqlQuery<BioSequence> {

        public BioSequenceEnsemblMappingQuery( DataSource ds, String query ) {
            super( ds, query );
            compile();
        }

        @Override
        public BioSequence mapRow( ResultSet rs, int rowNumber ) throws SQLException {
            BioSequence bioSequence = BioSequence.Factory.newInstance();

            DatabaseEntry de = DatabaseEntry.Factory.newInstance();

            String name = rs.getString( "name" );
            bioSequence.setName( name );
            bioSequence.setPolymerType( PolymerType.RNA );
            bioSequence.setIsCircular( false );

            de.setAccession( name );
            de.setExternalDatabase( ensembl );

            bioSequence.setType( SequenceType.mRNA );
            bioSequence.setSequenceDatabaseEntry( de );

            return bioSequence;
        }
    }

    /**
     * @param query
     * @return
     */
    private Collection<BioSequence> loadSequencesByQuery( String table, SequenceType type, String limit ) {
        BioSequenceMappingQuery bsQuery = new BioSequenceMappingQuery( this.jdbcTemplate.getDataSource(), table, type, limit );
        return bsQuery.execute();
    }

    /**
     * @param query
     * @return
     */
    private Collection<BioSequence> loadRefseqByQuery( String limitSuffix ) {
        String query = "SELECT name FROM " + REF_GENE_TABLE_NAME + " " + limitSuffix;
        BioSequenceRefseqMappingQuery bsQuery = new BioSequenceRefseqMappingQuery( this.jdbcTemplate.getDataSource(), query );
        return bsQuery.execute();
    }

    /**
     * @param query
     * @return
     */
    private Collection<BioSequence> loadEnsemblByQuery( String limitSuffix ) {
        String query = "SELECT name FROM " + ENSEMBL_TABLE_NAME + " " + limitSuffix;
        BioSequenceEnsemblMappingQuery bsQuery = new BioSequenceEnsemblMappingQuery( this.jdbcTemplate.getDataSource(), query );
        return bsQuery.execute();
    }

}
