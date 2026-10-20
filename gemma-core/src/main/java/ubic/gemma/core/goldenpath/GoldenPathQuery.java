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
package ubic.gemma.core.goldenpath;

import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.object.MappingSqlQuery;
import ubic.gemma.core.loader.genome.BlatResultParser;
import ubic.gemma.core.util.SQLUtils;
import ubic.gemma.model.genome.Chromosome;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.model.genome.sequenceAnalysis.BlatResult;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * @author pavlidis
 */
public class GoldenPathQuery extends GoldenPath {

    private static final int TEST_PORT = 3306;
    private final EstQuery estQuery;
    private final MrnaQuery mrnaQuery;
    /** probe mapping queries from several threads, and each table is to be reported once */
    private final Set<String> missingTables = new CopyOnWriteArraySet<>();

    public GoldenPathQuery( Taxon taxon ) {
        super( taxon );
        estQuery = new EstQuery( getDataSource() );
        mrnaQuery = new MrnaQuery( getDataSource() );
    }

    /**
     * Locate the alignment for the given sequence, if it exists in the goldenpath database.
     * Implementation note: This queries the est and mrna tables only.
     *
     * @param accession The genbank accession of the sequence.
     * @return blat results
     */
    public Collection<BlatResult> findAlignments( String accession ) {
        Collection<BlatResult> results = this.execute( estQuery, "all_est", accession );
        if ( !results.isEmpty() ) {
            return results;
        }

        return this.execute( mrnaQuery, "all_mrna", accession );
    }

    /**
     * Run one of the alignment queries, treating the table not being there as "no alignment on record".
     * <p>
     * A goldenpath database built from a UCSC assembly hub has no {@code all_est} or {@code all_mrna} — those exist
     * only for full browser assemblies — and blatPlatform died on the first probe carrying an accession. Having no
     * precomputed alignment to reuse is what the caller already handles: it sends the sequence to a real BLAT. That
     * is also the right answer during an assembly migration, where UCSC's precomputed alignments are against the
     * assembly being migrated away from.
     */
    private Collection<BlatResult> execute( MappingSqlQuery<BlatResult> query, String table, String accession ) {
        if ( missingTables.contains( table ) ) {
            return Collections.emptyList();
        }
        try {
            return query.execute( accession );
        } catch ( BadSqlGrammarException e ) {
            if ( !GoldenPathQuery.isMissingTable( e ) ) {
                throw e;
            }
            if ( missingTables.add( table ) ) {
                GoldenPath.log.warn( String.format( "%s has no %s table, so no alignment will be read from it and "
                                + "sequences will be aligned with BLAT instead. A goldenpath database built from a "
                                + "UCSC assembly hub has neither all_est nor all_mrna.",
                        this.getSearchedDatabase().getName(), table ) );
            }
            return Collections.emptyList();
        }
    }

    /**
     * @return true if the exception is MySQL's "table doesn't exist" (1146 / 42S02) rather than a query this code got
     *         wrong, which must still fail
     */
    static boolean isMissingTable( BadSqlGrammarException e ) {
        SQLException cause = e.getSQLException();
        return cause != null && ( cause.getErrorCode() == 1146 || "42S02".equals( cause.getSQLState() ) );
    }

    private BlatResult convertResult( ResultSet rs ) throws SQLException {
        BlatResult result = BlatResult.Factory.newInstance();

        result.setQuerySequence( BioSequence.Factory.newInstance() );
        Long queryLength = rs.getLong( "qSize" );
        result.getQuerySequence().setLength( queryLength );

        result.setMatches( rs.getInt( "matches" ) );
        result.setMismatches( rs.getInt( "misMatches" ) );
        result.setRepMatches( rs.getInt( "repMatches" ) );
        result.setNs( rs.getInt( "nCount" ) );
        result.setQueryGapCount( rs.getInt( "qNumInsert" ) );
        result.setQueryGapBases( rs.getInt( "qBaseInsert" ) );
        result.setTargetGapCount( rs.getInt( "tNumInsert" ) );
        result.setTargetGapBases( rs.getInt( "tBaseInsert" ) );
        result.setStrand( rs.getString( "strand" ) );
        result.setQueryStart( rs.getInt( "qStart" ) );
        result.setQueryEnd( rs.getInt( "qEnd" ) );
        result.setTargetStart( rs.getLong( "tStart" ) );
        result.setTargetEnd( rs.getLong( "tEnd" ) );
        result.setBlockCount( rs.getInt( "blockCount" ) );

        result.setBlockSizes( SQLUtils.blobToString( rs.getBlob( "blockSizes" ), StandardCharsets.ISO_8859_1 ) );
        result.setQueryStarts( SQLUtils.blobToString( rs.getBlob( "qStarts" ), StandardCharsets.ISO_8859_1 ) );
        result.setTargetStarts( SQLUtils.blobToString( rs.getBlob( "tStarts" ), StandardCharsets.ISO_8859_1 ) );

        String queryName = rs.getString( "qName" );
        queryName = BlatResultParser.cleanUpQueryName( queryName );
        result.getQuerySequence().setName( queryName );

        String chrom = rs.getString( "tName" );
        if ( chrom.startsWith( "chr" ) ) {
            chrom = chrom.substring( chrom.indexOf( "chr" ) + 3 );
            if ( chrom.endsWith( ".fa" ) ) {
                chrom = chrom.substring( 0, chrom.indexOf( ".fa" ) );
            }
        }

        result.setTargetChromosome( Chromosome.Factory.newInstance( chrom, null, BioSequence.Factory.newInstance(), this.getTaxon() ) );
        result.getTargetChromosome().getSequence().setName( chrom );
        result.getTargetChromosome().getSequence().setLength( rs.getLong( "tSize" ) );
        result.getTargetChromosome().getSequence().setTaxon( this.getTaxon() );
        result.setSearchedDatabase( this.getSearchedDatabase() );

        return result;
    }

    private class EstQuery extends MappingSqlQuery<BlatResult> {

        EstQuery( DataSource dataSource ) {
            super( dataSource, "SELECT * FROM all_est WHERE qName = ?" );
            super.declareParameter( new SqlParameter( "accession", Types.VARCHAR ) );
            this.compile();
        }

        @Override
        protected BlatResult mapRow( ResultSet rs, int rowNum ) throws SQLException {
            return GoldenPathQuery.this.convertResult( rs );

        }

    }

    private class MrnaQuery extends MappingSqlQuery<BlatResult> {

        MrnaQuery( DataSource dataSource ) {
            super( dataSource, "SELECT * FROM all_mrna WHERE qName = ?" );
            super.declareParameter( new SqlParameter( "accession", Types.VARCHAR ) );
            this.compile();
        }

        @Override
        protected BlatResult mapRow( ResultSet rs, int rowNum ) throws SQLException {
            return GoldenPathQuery.this.convertResult( rs );
        }

    }

}
