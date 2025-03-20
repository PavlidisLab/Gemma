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

import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.object.MappingSqlQuery;
import ubic.basecode.util.SQLUtils;
import ubic.gemma.core.loader.genome.BlatResultParser;
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

/**
 * @author pavlidis
 */
public class GoldenPathQuery extends GoldenPath {

    private static final int TEST_PORT = 3306;
    private final EstQuery estQuery;
    private final MrnaQuery mrnaQuery;

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
        Collection<BlatResult> results = estQuery.execute( accession );
        if ( results.size() > 0 ) {
            return results;
        }

        return mrnaQuery.execute( accession );
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

        result.setTargetChromosome( new Chromosome( chrom, null, BioSequence.Factory.newInstance(), this.getTaxon() ) );
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
