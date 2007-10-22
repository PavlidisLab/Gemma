/*
 * The Gemma project
 * 
 * Copyright (c) 2007 University of British Columbia
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
package ubic.gemma.analysis.linkAnalysis;

import java.io.Writer;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.basecode.dataStructure.matrix.CompressedNamedBitMatrix;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Gene;

/**
 * Holds results of link statistics collection and perform some simple manipulations/counting.
 * 
 * @author paul
 * @version $Id$
 */
public class LinkStatistics {

    private static Log log = LogFactory.getLog( LinkStatistics.class.getName() );

    /*
     * Map of EE to location in the bitstrings.
     */
    private Map<Long, Integer> eeMap;

    /*
     * Gene-by-gene matrix
     */
    private Collection<Gene> genes;
    private CompressedNamedBitMatrix posLinkCounts = null;
    private CompressedNamedBitMatrix negLinkCounts = null;
    // private int totalLinks = 0;
    private Set<Long> geneCoverage = new HashSet<Long>();

    /**
     * @param ees
     * @param genes Needed so output contains gene symbols, not just IDs.
     */
    public LinkStatistics( Collection<ExpressionExperiment> ees, Collection<Gene> genes ) {
        this.eeMap = new HashMap<Long, Integer>();
        int index = 0;
        for ( ExpressionExperiment eeIter : ees ) {
            eeMap.put( eeIter.getId(), new Integer( index ) );
            index++;
        }
        this.genes = genes;
        this.posLinkCounts = this.initMatrix( ees, genes );
        this.negLinkCounts = this.initMatrix( ees, genes );
    }

    /**
     * @param ees
     * @param genes
     * @return
     */
    public CompressedNamedBitMatrix initMatrix( Collection<ExpressionExperiment> ees, Collection<Gene> genes ) {
        CompressedNamedBitMatrix linkCount = new CompressedNamedBitMatrix( genes.size(), genes.size(), ees.size() );
        for ( Gene geneIter : genes ) {
            linkCount.addRowName( geneIter.getId() );
        }
        for ( Gene geneIter : genes ) {
            linkCount.addColumnName( geneIter.getId() );
        }
        return linkCount;
    }

    /**
     * @param geneLinks
     * @param ee
     */
    public void addLinks( Collection<GeneLink> geneLinks, ExpressionExperiment ee ) {
        assert eeMap != null;
        int eeIndex = eeMap.get( ee.getId() );
        for ( GeneLink geneLink : geneLinks ) {
            addLinks( eeIndex, geneLink );
        }
    }

    /**
     * @param eeIndex
     * @param link
     * @param firstGeneId
     * @param secondGeneId
     */
    private void addLinks( int eeIndex, GeneLink geneLink ) {

        Long firstGeneId = geneLink.getFirstGene();
        Long secondGeneId = geneLink.getSecondGene();

        // skip self-links
        if ( firstGeneId.equals( secondGeneId ) ) {
            if ( log.isTraceEnabled() ) log.trace( "Skipping self link for gene=" + firstGeneId );
            return;
        }

        geneCoverage.add( firstGeneId );
        geneCoverage.add( secondGeneId );

        if ( !posLinkCounts.containsRowName( firstGeneId ) || !posLinkCounts.containsRowName( secondGeneId ) ) {
            // this is okay, it happens if we are limiting to 'known genes' but we gathered links for all genes.
            return;
            // throw new IllegalStateException( "Link matrix does not contain rows for one or both of " + firstGeneId
            // + "," + secondGeneId );
        }

        int rowIndex = posLinkCounts.getRowIndexByName( firstGeneId );
        int colIndex = posLinkCounts.getColIndexByName( secondGeneId );
        if ( geneLink.getScore() > 0 ) {
            posLinkCounts.set( rowIndex, colIndex, eeIndex );
        } else {
            negLinkCounts.set( rowIndex, colIndex, eeIndex );
        }

    }

    /**
     * @return
     */
    public LinkConfirmationStatistics getLinkConfirmationStats() {

        int rows = posLinkCounts.rows();
        int cols = posLinkCounts.columns();

        LinkConfirmationStatistics results = new LinkConfirmationStatistics();

        log.info( "Summarizing ... " );
        // The filling process only filled one item. So the matrix is not symmetric
        for ( int i = 0; i < rows; i++ ) {
            int[] positiveBits = this.posLinkCounts.getRowBitCount( i );
            int[] negativeBits = this.negLinkCounts.getRowBitCount( i );
            for ( int j = 0; j < cols; j++ ) {
                int positiveBit = positiveBits[j];
                int negativeBit = negativeBits[j];
                if ( positiveBit > 0 ) {
                    results.addPos( positiveBit );
                }
                if ( negativeBit > 0 ) {
                    results.addNeg( negativeBit );
                }
            }
            if ( i > 0 && i % 10000 == 0 ) {
                log.info( "Summarized results for " + i + " genes" );
            }
        }
        return results;
    }

    /**
     * Write the link data. Format is the genes, then the number of positively correlated links, then the number of
     * negatively correlated links. If linkStringency is greater than zero, only links meeting the criteria are output,
     * with pos and neg on separate lines.
     * 
     * @param out
     * @param genes
     * @param linkStringency
     */
    public void writeLinks( Writer out, int linkStringency ) {
        log.info( "Writing links with support >=" + linkStringency );
        Map<Long, String> geneId2Name = new HashMap<Long, String>();
        for ( Gene gene : genes ) {
            geneId2Name.put( gene.getId(), gene.getName() );
        }
        int count = 0;
        try {
            int rows = posLinkCounts.rows();
            int cols = posLinkCounts.columns();

            // header
            if ( linkStringency == 0 ) {
                out.write( "Gene1\tGene2\tPosLinks\tNegLinks\n" );
            } else {
                out.write( "Gene1\tGene2\tSupport\tCorrSign\n" );
            }

            // The filling process only filled one item. So the matrix is not symmetric
            for ( int i = 0; i < rows; i++ ) {
                int[] positiveBits = posLinkCounts.getRowBitCount( i );
                int[] negativeBits = negLinkCounts.getRowBitCount( i );
                String gene1Name = geneId2Name.get( posLinkCounts.getRowName( i ) );

                for ( int j = 0; j < cols; j++ ) {
                    int positiveBit = positiveBits[j];
                    int negativeBit = negativeBits[j];

                    String gene2Name = geneId2Name.get( posLinkCounts.getColName( j ) );

                    if ( linkStringency > 0 ) { // limit to links above this
                        if ( positiveBit >= linkStringency ) {
                            out.write( gene1Name + "\t" + gene2Name + "\t" + positiveBit + "\t" + "+" + "\n" );
                            count++;
                        }
                        if ( negativeBit >= linkStringency ) {
                            out.write( gene1Name + "\t" + gene2Name + "\t" + negativeBit + "\t" + "-" + "\n" );
                            count++;
                        }
                    } else { // print all links.
                        if ( positiveBit > 0 || negativeBit > 0 ) {
                            out.write( gene1Name + "\t" + gene2Name + "\t" + positiveBit + "\t" + negativeBit + "\n" );
                            count++;
                        }
                    }
                    if ( count > 0 && count % 100000 == 0 ) {
                        log.info( count + " links written" );
                    }
                }
                if ( i > 0 && i % 1000 == 0 ) {
                    log.info( "Links for " + i + " genes written" );
                }
            }
            log.info( count + " links written" );
            out.close();
        } catch ( Exception e ) {
            throw new RuntimeException( e );
        }
    }

    /**
     * @return Collection of genes used as an "axis" in the gene x gene link matrix.
     */
    public Collection<Gene> getGenes() {
        return genes;
    }

    /**
     * @return Collection of gene ids used as an "axis" in the gene x gene link matrix.
     */
    public Collection<Long> getGeneIds() {
        Collection<Long> result = new HashSet<Long>();
        for ( Gene g : this.genes ) {
            result.add( g.getId() );
        }
        return result;
    }
}
