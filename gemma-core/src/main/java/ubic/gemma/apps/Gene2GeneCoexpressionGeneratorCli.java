/*
 * The Gemma project.
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

package ubic.gemma.apps;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;

import ubic.basecode.dataStructure.BitUtil;
import ubic.gemma.model.analysis.GeneCoexpressionAnalysis;
import ubic.gemma.model.analysis.GeneCoexpressionAnalysisService;
import ubic.gemma.model.association.coexpression.Gene2GeneCoexpression;
import ubic.gemma.model.association.coexpression.Gene2GeneCoexpressionService;
import ubic.gemma.model.association.coexpression.HumanGeneCoExpression;
import ubic.gemma.model.association.coexpression.MouseGeneCoExpression;
import ubic.gemma.model.association.coexpression.OtherGeneCoExpression;
import ubic.gemma.model.association.coexpression.RatGeneCoExpression;
import ubic.gemma.model.coexpression.CoexpressionCollectionValueObject;
import ubic.gemma.model.coexpression.CoexpressionValueObject;
import ubic.gemma.model.common.protocol.Protocol;
import ubic.gemma.model.common.protocol.ProtocolService;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.TaxonService;
import ubic.gemma.model.genome.gene.GeneService;
import ubic.gemma.search.SearchResult;
import ubic.gemma.search.SearchService;
import ubic.gemma.search.SearchSettings;
import ubic.gemma.util.AbstractSpringAwareCLI; 

/**
 * @author klc
 * @version $Id$
 */
public class Gene2GeneCoexpressionGeneratorCli extends AbstractSpringAwareCLI {

    private static final int DEFAULT_STRINGINCY = 1;
    private static final int BATCH_SIZE = 500;
    // Used Services
    ExpressionExperimentService eeS;
    GeneService geneS;
    TaxonService taxonS;
    Gene2GeneCoexpressionService gene2geneS;
    GeneCoexpressionAnalysisService analysisS;
    ProtocolService protocolS;
    ExpressionExperimentService expExpS;
    SearchService searchS;

    Collection<ExpressionExperiment> expressionExperiments;
    Collection<Gene> toUseGenes;
    Taxon taxon;
    GeneCoexpressionAnalysis analysis;
    int toUseStringency;
    String toUseAnalysisName;

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.util.AbstractCLI#buildOptions()
     */

    @SuppressWarnings("static-access")
    @Override
    protected void buildOptions() {

        Option geneFileOption = OptionBuilder.hasArg().withArgName( "Gene List File Name" ).withDescription(
                "A text file that contains a list of gene symbols.  a new gene symbon on each line" ).withLongOpt(
                "geneFile" ).create( 'g' );

        Option expExperimentFileOption = OptionBuilder
                .hasArg()
                .withArgName( "Expression Experiment List File Name" )
                .withDescription(
                        "A text file that contains a list of expression experiments. Each line of the file contains the short name or the name of the expressionExperiment" )
                .withLongOpt( "eeFile" ).create( 'e' );

        Option taxonOption = OptionBuilder.hasArg().withArgName( "Taxon" ).withDescription( "The taxon to use" )
                .withLongOpt( "taxon.  Use the common name." ).create( 't' );

        Option stringencyOption = OptionBuilder.hasArg().withArgName( "Stringency" ).withDescription(
                "The stringency value: Defaults to 2" ).withLongOpt( "stringency" ).create( 's' );

        Option analysisNameOption = OptionBuilder.hasArg().withArgName( "Analysis" ).withDescription(
                "The name of the anaylis to create. Defaults to a date stamp" ).withLongOpt( "analysis" ).create( 'a' );

        Option eeSearchOption = OptionBuilder.hasArg().withArgName( "expressionQuerry" ).withDescription(
                "Use a querry string for defining what expression experiments to use" )
                .withLongOpt( "expressionQuerry" ).create( 'q' );

        // geneFileOption.setRequired( true );
        taxonOption.setRequired( true );

        addOption( geneFileOption );
        addOption( expExperimentFileOption );
        addOption( taxonOption );
        addOption( stringencyOption );
        addOption( analysisNameOption );
        addOption( eeSearchOption );

    }

    public static void main( String[] args ) {
        Gene2GeneCoexpressionGeneratorCli p = new Gene2GeneCoexpressionGeneratorCli();
        try {
            Exception ex = p.doWork( args );
            if ( ex != null ) {
                ex.printStackTrace();
            }
        } catch ( Exception e ) {
            throw new RuntimeException( e );
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.util.AbstractCLI#doWork(java.lang.String[])
     */
    @Override
    protected Exception doWork( String[] args ) {
        Exception err = processCommandLine( "Gene 2 Gene Coexpression Caching tool ", args );
        if ( err != null ) return err;

        Collection<Gene> processedGenes = new HashSet<Gene>();

        log.info( "Using " + expressionExperiments.size() + " Expression Experiments." );
        log.info( displayEEs() );
        for ( Gene gene : toUseGenes ) {

            CoexpressionCollectionValueObject coexpressions = ( CoexpressionCollectionValueObject ) geneS
                    .getCoexpressedGenes( gene, expressionExperiments, toUseStringency );
            persistCoexpressions( gene, coexpressions, processedGenes );
            processedGenes.add( gene );
        }

        return null;
    }

    /**
     * @param toPersist
     * @param alreadyPersisted
     */
    protected void persistCoexpressions( Gene firstGene, CoexpressionCollectionValueObject toPersist,
            final Collection<Gene> alreadyPersisted ) {

        Gene2GeneCoexpression g2gCoexpression = getNewGGCOInstance( taxon );

        log.info( "Persisting Gene2Gene coexpression data to the " + taxon.getCommonName()
                + "GeneCoexpression table" );
        g2gCoexpression.setSourceAnalysis( analysis );

        Collection<ExpressionExperiment> experimentsAnalyzed = analysis.getExperimentsAnalyzed();
        Map<Long, Integer> eeIdOrder = getOrderingMap( experimentsAnalyzed );

        int persistedCount = 0;

        Collection<Gene2GeneCoexpression> batch = new ArrayList<Gene2GeneCoexpression>();
        for ( CoexpressionValueObject co : toPersist.getCoexpressionData() ) {
            Gene secondGene = geneS.load( co.getGeneId() );
            if ( alreadyPersisted.contains( secondGene ) ) continue;

            g2gCoexpression.setFirstGene( firstGene );
            g2gCoexpression.setSecondGene( secondGene );
            g2gCoexpression.setPvalue( co.getCollapsedPValue() );

            Collection<Long> contributing2NegativeLinks = co.getEEContributing2NegativeLinks();
            Collection<Long> contributing2PositiveLinks = co.getEEContributing2NegativeLinks();

            if ( co.getNegativeLinkCount() >= toUseStringency ) {
                byte[] supportVector = computeSupportingDatasetVector( contributing2NegativeLinks, eeIdOrder );
                g2gCoexpression.setNumDataSets( co.getNegativeLinkCount() );
                g2gCoexpression.setEffect( co.getNegativeScore() );
                g2gCoexpression.setDatasetsSupportingVector( supportVector );
                batch.add( g2gCoexpression );
                if ( batch.size() == BATCH_SIZE ) {
                    this.gene2geneS.create( batch );
                    batch.clear();
                }
                persistedCount++;
            }

            if ( co.getPositiveLinkCount() >= toUseStringency ) {
                byte[] supportVector = computeSupportingDatasetVector( contributing2PositiveLinks, eeIdOrder );
                g2gCoexpression.setNumDataSets( co.getPositiveLinkCount() );
                g2gCoexpression.setEffect( co.getPositiveScore() );
                g2gCoexpression.setDatasetsSupportingVector( supportVector );
                batch.add( g2gCoexpression );
                if ( batch.size() == BATCH_SIZE ) {
                    this.gene2geneS.create( batch );
                    batch.clear();
                }
                persistedCount++;
            }

            log.debug( "Persisted: " + firstGene.getOfficialSymbol() + " --> " + secondGene.getOfficialSymbol() + " ( "
                    + co.getNegativeScore() + " , +" + co.getPositiveScore() + " )" );
        }

        if ( batch.size() == BATCH_SIZE ) {
            this.gene2geneS.create( batch );
            batch.clear();
        }
        log.info( "Persited " + persistedCount + " gene2geneCoexpressions for analysis: " + analysis.getName() );

    }

    /**
     * @param toUseTaxon2
     * @return
     */
    private Gene2GeneCoexpression getNewGGCOInstance( Taxon toUseTaxon2 ) {
        Gene2GeneCoexpression g2gCoexpression;
        if ( taxon.getCommonName().equalsIgnoreCase( "mouse" ) )
            g2gCoexpression = MouseGeneCoExpression.Factory.newInstance();
        else if ( taxon.getCommonName().equalsIgnoreCase( "rat" ) )
            g2gCoexpression = RatGeneCoExpression.Factory.newInstance();
        else if ( taxon.getCommonName().equalsIgnoreCase( "human" ) )
            g2gCoexpression = HumanGeneCoExpression.Factory.newInstance();
        else
            g2gCoexpression = OtherGeneCoExpression.Factory.newInstance();
        return g2gCoexpression;
    }

    /**
     * @param experimentsAnalyzed
     * @return Map of EE IDs to the location in the vector.
     */
    private Map<Long, Integer> getOrderingMap( Collection<ExpressionExperiment> experimentsAnalyzed ) {
        List<Long> eeIds = new ArrayList<Long>();
        for ( ExpressionExperiment ee : experimentsAnalyzed ) {
            eeIds.add( ee.getId() );
        }
        Collections.sort( eeIds );
        Map<Long, Integer> eeIdOrder = new HashMap<Long, Integer>();
        int location = 0;
        for ( Long id : eeIds ) {
            eeIdOrder.put( id, ++location );
        }
        return eeIdOrder;
    }

    /**
     * @param experimentsAnalyzed
     * @return Map of location in the vector to EE ID.
     */
    public Map<Integer, Long> getLocationMap( Collection<ExpressionExperiment> experimentsAnalyzed ) {
        List<Long> eeIds = new ArrayList<Long>();
        for ( ExpressionExperiment ee : experimentsAnalyzed ) {
            eeIds.add( ee.getId() );
        }
        Collections.sort( eeIds );
        Map<Integer, Long> eeOrderId = new HashMap<Integer, Long>();
        int location = 0;
        for ( Long id : eeIds ) {
            eeOrderId.put( ++location, id );
        }
        return eeOrderId;
    }

    /**
     * Algorithm:
     * <ol>
     * <li>Initialize byte array large enough to hold all the EE information (ceil(numeeids / 8))
     * <li>Flip the bit at the right location.
     * </ol>
     * 
     * @param idsToFlip
     * @param eeIdOrder
     * @return
     */
    private byte[] computeSupportingDatasetVector( Collection<Long> idsToFlip, Map<Long, Integer> eeIdOrder ) {
        byte[] supportVector = new byte[( int ) Math.ceil( eeIdOrder.keySet().size() / Byte.SIZE )];
        for ( int i = 0, j = supportVector.length; i < j; i++ ) {
            supportVector[i] = 0x0;
        }

        for ( Long id : idsToFlip ) {
            BitUtil.set( supportVector, eeIdOrder.get( id ) );
        }

        return supportVector;
    }

    /**
     * @param ggc
     * @param eePositionToIdMap
     * @return
     */
    public List<ExpressionExperiment> getSupportingExperiments( Gene2GeneCoexpression ggc,
            Map<Integer, Long> eePositionToIdMap ) {

        byte[] datasetsSupportingVector = ggc.getDatasetsSupportingVector();
        List<ExpressionExperiment> result = new ArrayList<ExpressionExperiment>();
        for ( int i = 0; i < datasetsSupportingVector.length * Byte.SIZE; i++ ) {
            if ( BitUtil.get( datasetsSupportingVector, i ) ) {
                Long supportingEE = eePositionToIdMap.get( i );
                result.add( eeS.load( supportingEE ) );
            }
        }
        return result;
    }

    /**
     * 
     */

    @SuppressWarnings("unchecked")
    @Override
    protected void processOptions() {

        super.processOptions();

        initSpringBeans();

        taxon = taxonS.findByCommonName( this.getOptionValue( 't' ) );

        if ( this.hasOption( 'q' ) ) {
            processQuery( this.getOptionValue( 'q' ) );
        }

        if ( this.hasOption( 'e' ) ) {
            processEEFile( this.getOptionValue( 'e' ) );
        } else {
            this.expressionExperiments = eeS.findByTaxon( taxon );
        }

        if ( this.hasOption( 'g' ) ) {
            processGeneFile( this.getOptionValue( 'g' ) );
        } else {
            toUseGenes = geneS.loadGenes( taxon );
        }

        toUseStringency = DEFAULT_STRINGINCY;
        if ( this.hasOption( 's' ) ) {
            toUseStringency = Integer.parseInt( this.getOptionValue( 's' ) );
        }

        if ( this.hasOption( 'a' ) ) {
            toUseAnalysisName = this.getOptionValue( 'a' );
        }

        initAttributes();

    }

    /**
     * @param query
     */
    private void processQuery( String query ) {

        List<SearchResult> ees = searchS.search( SearchSettings.ExpressionExperimentSearch( query ) ).get(
                ExpressionExperiment.class );

        log.info( ees.size() + "Expresion expreiments matched the search criteria" );

        // Filter out all the ee that are not of correct taxon
        for ( SearchResult sr : ees ) {
            ExpressionExperiment ee = ( ExpressionExperiment ) sr.getResultObject();
            Taxon t = eeS.getTaxon( ee.getId() );
            if ( t.getCommonName().equalsIgnoreCase( taxon.getCommonName() ) )
                expressionExperiments.add( ee );
            else
                log.info( "Wrong taxon. Not using expression experiment: " + ee.getShortName() );
        }

    }

    private void processEEFile( String fileName ) {

        Collection<String> eeIds = processFile( fileName );

        for ( String id : eeIds ) {
            ExpressionExperiment ee = eeS.findByName( id );

            if ( ee == null ) ee = eeS.findByShortName( id );

            if ( ee == null ) {
                log.info( "Couldn't find Expression Experiment: " + id );
                continue;
            }

            if ( taxon.getId().longValue() != expExpS.getTaxon( ee.getId() ).getId().longValue() ) {
                log.info( "Expression Experiment: " + id + " not included. Wrong Taxon" );
                continue;
            }

            expressionExperiments.add( ee );
            log.info( "Expression Expreiment: " + ee.getShortName() + " added to processing list " );
        }

    }

    @SuppressWarnings("unchecked")
    private void processGeneFile( String fileName ) {

        Collection<String> geneIds = processFile( fileName );

        if ( ( geneIds == null ) || ( geneIds.isEmpty() ) ) {
            log.warn( "No valid genes found.  Unable to process" );
            return;
        }

        for ( String id : geneIds ) {

            Collection<Gene> genes = geneS.findByOfficialSymbol( id );

            if ( ( genes == null ) || ( genes.isEmpty() ) ) genes = geneS.findByOfficialName( id );

            if ( ( genes == null ) || ( genes.isEmpty() ) ) {
                log.info( "Gene with id: " + id + " not found.  Removed from processing list" );
                continue;
            }
            // What to do with a search results that returns more than one gene?
            for ( Gene g : genes ) {

                if ( !taxon.equals( g.getTaxon() ) ) {
                    log.info( "Gene " + g.getOfficialSymbol() + " with id: " + g.getId()
                            + " removed from processing list. Wrong Taxon. " );
                    continue;
                }

                toUseGenes.add( g );
                log.info( "Gene " + g.getOfficialSymbol() + " with id: " + g.getId() + " added to processing list " );

            }
        }
    }

    private void initSpringBeans() {

        geneS = ( GeneService ) this.getBean( "geneService" );
        eeS = ( ExpressionExperimentService ) this.getBean( "expressionExperimentService" );
        taxonS = ( TaxonService ) this.getBean( "taxonService" );
        gene2geneS = ( Gene2GeneCoexpressionService ) this.getBean( "gene2GeneCoexpressionService" );
        analysisS = ( GeneCoexpressionAnalysisService ) this.getBean( "geneCoexpressionAnalysisService" );
        protocolS = ( ProtocolService ) this.getBean( "protocolService" );
        expExpS = ( ExpressionExperimentService ) this.getBean( "expressionExperimentService" );
        searchS = ( SearchService ) this.getBean( "searchService" );

        expressionExperiments = new HashSet<ExpressionExperiment>();
        toUseGenes = new HashSet<Gene>();
    }

    private void initAttributes() {

        analysis = GeneCoexpressionAnalysis.Factory.newInstance();

        analysis.setDescription( "Coexpression analysis for " + taxon.getCommonName() + "using " + expressionExperiments.size()
                + " expression experiments" );

        Calendar cal = new GregorianCalendar();

        if ( toUseAnalysisName == null ) toUseAnalysisName = "Generated on:";

        analysis.setName( toUseAnalysisName + cal.get( Calendar.YEAR ) + " " + cal.get( Calendar.MONTH ) + " "
                + cal.get( Calendar.DAY_OF_MONTH ) + " " + cal.get( Calendar.HOUR_OF_DAY ) + ":"
                + cal.get( Calendar.MINUTE ) );

        Protocol protocol = Protocol.Factory.newInstance();
        protocol.setName( "Stored Gene2GeneCoexpressions" );
        protocol.setDescription( "Using: " + this.expressionExperiments.size() + " Expression Experiments,  " + toUseGenes.size()
                + " Genes" );
        protocol = protocolS.findOrCreate( protocol );

        analysis.setProtocol( protocol );
        analysis.setExperimentsAnalyzed( this.expressionExperiments );

        analysis = ( GeneCoexpressionAnalysis ) analysisS.create( analysis );

    }

    private String displayEEs() {

        String results = " ";
        for ( ExpressionExperiment ee : expressionExperiments ) {
            results += ee.getShortName() + "  ";
        }
        return results;
    }

}