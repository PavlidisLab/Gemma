package edu.columbia.gemma.tools;

import java.sql.Blob;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import baseCode.io.ByteArrayConverter;
import edu.columbia.gemma.genome.Gene;
import edu.columbia.gemma.genome.PhysicalLocation;
import edu.columbia.gemma.genome.gene.GeneProduct;

/**
 * This is temporary, until we have this in our own database.
 * <hr>
 * <p>
 * Copyright (c) 2004-2005 Columbia University
 * 
 * @author pavlidis
 * @version $Id$
 */
public class GoldenPath {
    private static final Log log = LogFactory.getLog( GoldenPath.class );

    private QueryRunner qr;
    private Connection conn;

    /**
     * @param databaseName
     * @param host
     * @param user
     * @param password
     * @throws SQLException
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws ClassNotFoundException
     */
    public GoldenPath( int port, String databaseName, String host, String user, String password ) throws SQLException,
            InstantiationException, IllegalAccessException, ClassNotFoundException {
        Class.forName( "com.mysql.jdbc.Driver" ).newInstance();
        String url = "jdbc:mysql://" + host + ":" + port + "/" + databaseName + "?relaxAutoCommit=true";
        conn = DriverManager.getConnection( url, user, password );
        qr = new QueryRunner();
    }

    /**
     * Find genes contained in or overlapping a region.
     * 
     * @param chromosome
     * @param start
     * @param end
     */
    public List findRefGenesByLocation( String chromosome, int start, int end ) {
        Integer starti = new Integer( start );
        Integer endi = new Integer( end );
        String searchChrom = trimChromosomeName( chromosome );

        try {

            // Cases:
            // 1. gene is contained within the region: txStart > start & txEnd < end;
            // 2. region is conained within the gene: txStart < start & txEnd > end;
            // 3. region overlaps start of gene: txStart > start & txStart < end.
            // 4. region overlaps end of gene: txEnd > start & txEnd < end
            //           
            return ( List ) qr
                    .query(
                            conn,
                            "SELECT name, geneName, txStart, txEnd, strand, exonStarts, exonEnds FROM refFlat WHERE "
                                    + "((txStart > ? AND txEnd < ?) OR (txStart < ? AND txEnd > ?) OR "
                                    + "(txStart > ?  AND txStart < ?) OR  (txEnd > ? AND  txEnd < ? )) and chrom = ? order by txStart ",
                            new Object[] { starti, endi, starti, endi, starti, endi, starti, endi, searchChrom },
                            new ResultSetHandler() {

                                public Object handle( ResultSet rs ) throws SQLException {
                                    Collection r = new ArrayList();
                                    while ( rs.next() ) {

                                        Gene gene = Gene.Factory.newInstance();

                                        gene.setNcbiId( rs.getString( "name" ) );

                                        gene.setOfficialSymbol( rs.getString( "geneName" ) );
                                        gene.setId( new Long( gene.getNcbiId().hashCode() ) );

                                        PhysicalLocation pl = PhysicalLocation.Factory.newInstance();
                                        pl.setNucleotide( new Integer( rs.getInt( "txStart" ) ) );
                                        pl.setNucleotideLength( new Integer( rs.getInt( "txEnd" )
                                                - rs.getInt( "txStart" ) ) );
                                        pl.setStrand( rs.getString( "strand" ) );

                                        // note that we aren't setting the chromosome here; we already know that.
                                        gene.setPhysicalLocation( pl );
                                        r.add( gene );

                                        Blob exonStarts = rs.getBlob( "exonStarts" );
                                        Blob exonEnds = rs.getBlob( "exonEnds" );

                                        setExons( gene, exonStarts, exonEnds );
                                    }
                                    return r;
                                }

                                /**
                                 * @param gene
                                 * @param exonStarts
                                 * @param exonEnds
                                 * @throws SQLException
                                 */
                                private void setExons( Gene gene, Blob exonStarts, Blob exonEnds ) throws SQLException {

                                    String exonStartLocations = blobToString( exonStarts );
                                    String exonEndLocations = blobToString( exonEnds );

                                    int[] exonStartsInts = SequenceManipulation
                                            .blatLocationsToIntArray( exonStartLocations );
                                    int[] exonEndsInts = SequenceManipulation
                                            .blatLocationsToIntArray( exonEndLocations );

                                    assert exonStartsInts.length == exonEndsInts.length;

                                    GeneProduct gp = GeneProduct.Factory.newInstance();
                                    Collection exons = new ArrayList();
                                    for ( int i = 0; i < exonEndsInts.length; i++ ) {
                                        int exonStart = exonStartsInts[i];
                                        int exonEnd = exonEndsInts[i];
                                        PhysicalLocation exon = PhysicalLocation.Factory.newInstance();
                                        // FIXME set the chromosome for the location.
                                        exon.setNucleotide( new Integer( exonStart ) );
                                        exon.setNucleotideLength( new Integer( exonEnd - exonStart ) );
                                        exons.add( exon );
                                    }
                                    gp.setExons( exons );
                                    gp.setName( gene.getNcbiId() );
                                    Collection products = new HashSet();
                                    products.add( gp );
                                    gene.setProducts( products );
                                }

                                private String blobToString( Blob exonStarts ) throws SQLException {
                                    byte[] bytes = exonStarts.getBytes( 1L, ( int ) exonStarts.length() );
                                    ByteArrayConverter bac = new ByteArrayConverter();
                                    return bac.byteArrayToAsciiString( bytes );
                                }
                            } );

        } catch ( SQLException e ) {
            throw new RuntimeException( e );
        }
    }

    /**
     * @param chromosome
     * @return
     */
    private String trimChromosomeName( String chromosome ) {
        String searchChrom = chromosome;
        if ( !searchChrom.startsWith( "chr" ) ) searchChrom = "chr" + searchChrom;
        return searchChrom;
    }

    /**
     * Given a physical location, find how close it is to the 3' end of a gene it is in.
     * 
     * @param chromosome The chromosome name (the organism is set by the constructor)
     * @param start The start base of the region to query.
     * @param end The end base of the region to query.
     * @param starts Locations of alignment starts. (comma-delimited from blat)
     * @param sizes Sizes of alignment blocks (comma-delimited from blat)
     * @return A list of ThreePrimeData objects. The distance stored by a ThreePrimeData will be 0 if the sequence
     *         overhangs (rather than providing a negative distance). If no genes are found, the result is null;
     */
    public List getThreePrimeDistances( String chromosome, int start, int end, String starts, String sizes ) {

        if ( end < start ) throw new IllegalArgumentException( "End must not be less than start" );

        Collection genes = findRefGenesByLocation( chromosome, start, end );
        if ( genes.size() == 0 ) return null;

        List results = new ArrayList();
        for ( Iterator iter = genes.iterator(); iter.hasNext(); ) {
            Gene gene = ( Gene ) iter.next();
            ThreePrimeData tpd = new ThreePrimeData( gene );

            PhysicalLocation geneLoc = gene.getPhysicalLocation();
            int geneStart = geneLoc.getNucleotide().intValue();
            int geneEnd = geneLoc.getNucleotide().intValue() + geneLoc.getNucleotideLength().intValue();

            int exonOverlap = 0;
            if ( starts != null & sizes != null ) {
                exonOverlap = SequenceManipulation.getGeneExonOverlaps( chromosome, starts, sizes, gene );
            }

            assert exonOverlap <= end - start;

            tpd.setExonOverlap( exonOverlap );
            tpd.setInIntron( exonOverlap == 0 );

            if ( geneLoc.getStrand().equals( "+" ) ) {
                // then the 3' end is at the 'end'. : >>>>>>>>>>>>>>>>>>>>>*>>>>> (* is where we might be)
                tpd.setDistance( Math.max( 0, geneEnd - end ) );
            } else if ( gene.getPhysicalLocation().getStrand().equals( "-" ) ) {
                // then the 3' end is at the 'start'. : <<<*<<<<<<<<<<<<<<<<<<<<<<<
                tpd.setDistance( Math.max( 0, start - geneStart ) );
            } else {
                throw new IllegalArgumentException( "Strand wasn't '+' or '-'" );
            }

            results.add( tpd );
        }

        return results;
    }

    /**
     * Helper data transfer object.
     */
    public class ThreePrimeData {

        private boolean inIntron = false;
        private int exonOverlap = 0;
        private int distance;
        private Gene gene;

        public ThreePrimeData( Gene gene ) {
            this.gene = gene;
        }

        public void setDistance( int i ) {
            this.distance = i;
        }

        public int getDistance() {
            return this.distance;
        }

        public Gene getGene() {
            return this.gene;
        }

        public boolean isInIntron() {
            return this.inIntron;
        }

        public void setInIntron( boolean inIntron ) {
            this.inIntron = inIntron;
        }

        /**
         * @return Returns the exonOverlap.
         */
        public int getExonOverlap() {
            return this.exonOverlap;
        }

        /**
         * @param exonOverlap The exonOverlap to set.
         */
        public void setExonOverlap( int exonOverlap ) {
            this.exonOverlap = exonOverlap;
        }

    }

}
