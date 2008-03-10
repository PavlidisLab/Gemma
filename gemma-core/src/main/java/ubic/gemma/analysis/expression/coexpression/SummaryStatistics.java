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
package ubic.gemma.analysis.expression.coexpression;

import java.math.BigInteger;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.basecode.dataStructure.matrix.CompressedSparseDoubleMatrix2DNamed;
import ubic.gemma.analysis.service.CompositeSequenceGeneMapperService;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.designElement.CompositeSequenceService;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.TaxonService;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.model.genome.gene.GeneService;
import ubic.gemma.util.AbstractSpringAwareCLI;

/**
 * Computing different statistics about the database to assist in computing probabilities
 * 
 * @author pavlidis
 * @version $Id$
 */
public class SummaryStatistics extends AbstractSpringAwareCLI {

    private static final int MAX_EXPS = 5;

    private static final int MAX_GENES = 100000;

    private static Log log = LogFactory.getLog( SummaryStatistics.class.getName() );

    ExpressionExperimentService expressionExperimentService;

    private String taxonName;
    TaxonService taxonService;
    ArrayDesignService arrayDesignService;
    CompositeSequenceService compositeSequenceService;

    /**
     * For each gene, count how many expression experiments it appears in.
     * 
     * @param taxon
     */
    @SuppressWarnings("unchecked")
    public void geneOccurrenceDistributions( Taxon taxon ) {

        Map<Long, Integer> counts = new HashMap<Long, Integer>();

        // get all expression experiments

        Collection<ExpressionExperiment> eeColl = expressionExperimentService.loadAll();

        int i = 0;
        for ( ExpressionExperiment experiment : eeColl ) {
            if ( i > MAX_EXPS ) break;
            Taxon eeTax = expressionExperimentService.getTaxon( experiment.getId() );
            if ( !eeTax.equals( taxon ) ) continue;
            Collection<ArrayDesign> ads = expressionExperimentService.getArrayDesignsUsed( experiment );

            // only count each gene once per data set.
            Collection<Long> seenids = new HashSet<Long>();

            for ( ArrayDesign design : ads ) {
                log.info( i + " " + design );
                Collection<Object[]> vals = compositeSequenceService.getRawSummary( design, null );
                log.info( "Got " + vals.size() + " reports" );
                for ( Object[] objects : vals ) {

                    BigInteger geneidi = ( BigInteger ) objects[10];
                    if ( geneidi == null ) {
                        continue;
                    }
                    Long geneid = geneidi.longValue();

                    if ( seenids.contains( geneid ) ) continue;

                    if ( counts.get( geneid ) == null ) {
                        counts.put( geneid, 0 );
                    }
                    counts.put( geneid, counts.get( geneid ) + 1 );
                    seenids.add( geneid );
                }
            }
            i++;
        }

        for ( Long l : counts.keySet() ) {
            System.out.println( l + "\t" + counts.get( l ) );
        }
    }

    /**
     * For each gene, count how many microarray probes there are.
     * 
     * @param taxon
     */
    @SuppressWarnings("unchecked")
    public void probesPerGene( Taxon taxon ) {
        GeneService geneService = ( GeneService ) this.getBean( "geneService" );
        CompositeSequenceGeneMapperService compositeSequenceGeneMapperService = ( CompositeSequenceGeneMapperService ) this
                .getBean( "compositeSequenceGeneMapperService" );
        Collection<Gene> genes = geneService.getGenesByTaxon( taxon );
        Map<Long, Integer> counts = new HashMap<Long, Integer>();
        int i = 0;
        for ( Gene gene : genes ) {
            Collection<CompositeSequence> compositeSequences = geneService.getCompositeSequencesById( gene.getId() );
            counts.put( gene.getId(), compositeSequences.size() );
            if ( ++i % 1000 == 0 ) {
                log.info( "Processed " + i + " genes" );
            }
        }
        for ( Long l : counts.keySet() ) {
            System.out.println( l + "\t" + counts.get( l ) );
        }
    }

    /**
     * For each composites sequence, count how many genes there are. This does not take into account multiple occurrence
     * os the same sequence in different probes!
     * 
     * @param taxon
     */
    @SuppressWarnings("unchecked")
    public void genesPerProbe( Taxon taxon ) {
        ArrayDesignService adService = ( ArrayDesignService ) this.getBean( "arrayDesignService" );
        Collection<ArrayDesign> ads = adService.loadAll();

        Map<Integer, Integer> counts = new HashMap<Integer, Integer>();
        int i = 0;
        Collection<BioSequence> seenSeqs = new HashSet<BioSequence>();
        for ( ArrayDesign design : ads ) {
            log.info( design );
            adService.thawLite( design );

            for ( CompositeSequence cs : design.getCompositeSequences() ) {

                BioSequence bs = cs.getBiologicalCharacteristic();

                if ( bs == null ) continue; // these don't count.

                if ( seenSeqs.contains( bs ) ) continue;

                Integer numGenes = 0;
                Collection<Gene> genes = compositeSequenceService.getGenes( cs );

                if ( genes == null )
                    numGenes = 0;
                else
                    numGenes = genes.size();

                if ( !counts.containsKey( numGenes ) ) {
                    counts.put( numGenes, 1 );
                } else {
                    counts.put( numGenes, counts.get( numGenes ) + 1 );
                }
                if ( ++i % 1000 == 0 ) {
                    log.info( "Processed " + i + " compositeSequences" );
                }

                seenSeqs.add( bs );
            }

        }

        for ( Integer l : counts.keySet() ) {
            System.out.println( l + "\t" + counts.get( l ) );
        }
    }

    /**
     * For each pair of genes, count how many expression experiments both appear in.
     * 
     * @param taxon
     */
    @SuppressWarnings("unchecked")
    public void genePairOccurrenceDistributions( Taxon taxon ) {

        Collection<ExpressionExperiment> eeColl = expressionExperimentService.loadAll();

        CompressedSparseDoubleMatrix2DNamed<Long, Long> mat = new CompressedSparseDoubleMatrix2DNamed<Long, Long>(
                MAX_GENES, MAX_GENES );

        int numEEs = 0;
        for ( ExpressionExperiment experiment : eeColl ) {
            if ( numEEs > MAX_EXPS ) break;
            Taxon eeTax = expressionExperimentService.getTaxon( experiment.getId() );
            if ( !eeTax.equals( taxon ) ) continue;
            Collection<ArrayDesign> ads = expressionExperimentService.getArrayDesignsUsed( experiment );

            // only count each gene once per data set.
            Collection<Long> seenids = new HashSet<Long>();

            for ( ArrayDesign design : ads ) {

                Collection<Object[]> vals = compositeSequenceService.getRawSummary( design, null );
                log.info( numEEs + " " + design + "Got " + vals.size() + " reports" );

                for ( Object[] objects : vals ) {

                    BigInteger geneidi = ( BigInteger ) objects[10];
                    if ( geneidi == null ) {
                        continue;
                    }
                    Long geneid = geneidi.longValue();

                    if ( seenids.contains( geneid ) ) continue;

                    if ( !mat.containsRowName( geneid ) ) {
                        mat.addRowName( geneid );
                    }

                    int outerIndex = mat.getRowIndexByName( geneid );

                    int j = 0;
                    for ( Object[] ojbB : vals ) {

                        BigInteger geneBidi = ( BigInteger ) ojbB[10];
                        if ( geneBidi == null || geneBidi.equals( geneidi ) ) {
                            continue;
                        }
                        Long geneBid = geneBidi.longValue();
                        if ( seenids.contains( geneBid ) ) continue;
                        int innerIndex;
                        if ( !mat.containsColumnName( geneBid ) ) {
                            mat.addColumnName( geneBid );
                            innerIndex = mat.getColIndexByName( geneBid );
                            mat.set( outerIndex, innerIndex, 0.0 ); // initialize
                        }

                        innerIndex = mat.getColIndexByName( geneBid );
                        mat.set( outerIndex, innerIndex, mat.get( outerIndex, innerIndex ) + 1 );

                        if ( mat.columns() > MAX_GENES ) {
                            log.warn( "Too many genes!" );
                            break;
                        }
                        j++;
                        if ( j > 1000 ) break;
                    }
                    seenids.add( geneid );

                    if ( mat.rows() > MAX_GENES ) {
                        break;
                    }

                }

            }
            numEEs++;
        }

        // print the histogram.
        int[] counts = new int[MAX_EXPS + 1];
        for ( Long outer : mat.getRowNames() ) {
            double[] row = mat.getRowByName( outer );
            for ( double d : row ) {
                counts[( int ) d]++;
            }
        }

        for ( int j = 0; j < counts.length; j++ ) {
            System.out.println( j + "\t" + counts[j] );
        }
    }

    public static void main( String[] args ) {
        SummaryStatistics sc = new SummaryStatistics();
        try {
            Exception ex = sc.doWork( args );
            if ( ex != null ) {
                ex.printStackTrace();
            }
        } catch ( Exception e ) {
            throw new RuntimeException( e );
        }
    }

    public void setArrayDesignService( ArrayDesignService adserv ) {
        this.arrayDesignService = adserv;
    }

    public void setExpressionExperimentService( ExpressionExperimentService ees ) {
        this.expressionExperimentService = ees;
    }

    @SuppressWarnings("static-access")
    @Override
    protected void buildOptions() {
        Option taxonOption = OptionBuilder.hasArg().withArgName( "taxon" ).withDescription(
                "Taxon common name (e.g., human)" ).create( 't' );

        addOption( taxonOption );

    }

    @Override
    protected Exception doWork( String[] args ) {
        Exception err = processCommandLine( "Sequence associator", args );
        if ( err != null ) return err;
        Taxon taxon = taxonService.findByCommonName( taxonName );
        genesPerProbe( taxon );
        return null;
    }

    public void setTaxonService( TaxonService taxonService ) {
        this.taxonService = taxonService;
    }

    @Override
    protected void processOptions() {
        super.processOptions();
        if ( this.hasOption( 't' ) ) {
            this.taxonName = this.getOptionValue( 't' );
        }
        this.taxonService = ( TaxonService ) getBean( "taxonService" );
        this.arrayDesignService = ( ArrayDesignService ) getBean( "arrayDesignService" );
        this.expressionExperimentService = ( ExpressionExperimentService ) getBean( "expressionExperimentService" );
        this.compositeSequenceService = ( CompositeSequenceService ) getBean( "compositeSequenceService" );
    }

    /**
     * @param compositeSequenceService the compositeSequenceService to set
     */
    public void setCompositeSequenceService( CompositeSequenceService compositeSequenceService ) {
        this.compositeSequenceService = compositeSequenceService;
    }
}
