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

    private Map<Long, Integer> eeMap;

    /*
     * Gene-by-gene matrix
     */
    private Collection<Gene> genes;
    private CompressedNamedBitMatrix posLinkCounts = null;
    private CompressedNamedBitMatrix negLinkCounts = null;
    private int totalLinks = 0;
    private Set<Long> geneCoverage = new HashSet<Long>();

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
        if ( firstGeneId.equals( secondGeneId ) ) return;

        geneCoverage.add( firstGeneId );
        geneCoverage.add( secondGeneId );
        try {
            int rowIndex = posLinkCounts.getRowIndexByName( firstGeneId );
            int colIndex = posLinkCounts.getColIndexByName( secondGeneId );
            if ( geneLink.getScore() > 0 ) {
                posLinkCounts.set( rowIndex, colIndex, eeIndex );
            } else {
                negLinkCounts.set( rowIndex, colIndex, eeIndex );
            }
        } catch ( Exception e ) {
            throw new RuntimeException( " No Gene Definition " + firstGeneId + "," + secondGeneId );
            // Aligned Region and Predicted Gene
        }
    }

    /**
     * @return
     */
    public LinkConfirmationStatistics getLinkConfirmationStats() {

        int rows = posLinkCounts.rows();
        int cols = posLinkCounts.columns();

        LinkConfirmationStatistics results = new LinkConfirmationStatistics();

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
        Map<Long, String> geneId2Name = new HashMap<Long, String>();
        for ( Gene gene : genes ) {
            geneId2Name.put( gene.getId(), gene.getName() );
        }
        try {
            int rows = posLinkCounts.rows();
            int cols = posLinkCounts.columns();

            // header
            out.write( "Gene1\tGene2\tPosLinks\tNegLinks\n" );

            // The filling process only filled one item. So the matrix is not symmetric
            for ( int i = 0; i < rows; i++ ) {
                int[] positiveBits = posLinkCounts.getRowBitCount( i );
                int[] negativeBits = negLinkCounts.getRowBitCount( i );

                for ( int j = 0; j < cols; j++ ) {
                    int positiveBit = positiveBits[j];
                    int negativeBit = negativeBits[j];

                    String gene1Name = geneId2Name.get( posLinkCounts.getRowName( i ) );
                    String gene2Name = geneId2Name.get( posLinkCounts.getColName( j ) );

                    if ( linkStringency > 0 ) { // limit to links above this
                        if ( positiveBit >= linkStringency ) {
                            out.write( gene1Name + "\t" + gene2Name + "\t" + positiveBit + "\t" + "+" + "\n" );
                        }
                        if ( negativeBit >= linkStringency ) {
                            out.write( gene1Name + "\t" + gene2Name + "\t" + negativeBit + "\t" + "-" + "\n" );
                        }
                    } else { // print all links.
                        if ( positiveBit > 0 || negativeBit > 0 ) {
                            out.write( gene1Name + "\t" + gene2Name + "\t" + positiveBit + "\t" + negativeBit + "\n" );
                        }
                    }
                }
            }
            out.close();
        } catch ( Exception e ) {
            throw new RuntimeException( e );
        }
    }
}
