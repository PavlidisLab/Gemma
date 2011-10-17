/*
 * The Gemma project
 * 
 * Copyright (c) 2008 University of British Columbia
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

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.apache.commons.collections.Transformer;
import org.apache.commons.lang.StringUtils;

import ubic.gemma.model.analysis.expression.ExpressionExperimentSet;
import ubic.gemma.model.analysis.expression.ExpressionExperimentSetService;
import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.common.auditAndSecurity.AuditEventService;
import ubic.gemma.model.expression.experiment.BioAssaySet;
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
 * Base class for CLIs that needs one or more expression experiment as an input. It offers the following ways of reading
 * them in:
 * <ul>
 * <li>All EEs
 * <li>All EEs for a particular taxon.
 * <li>A specific ExpressionExperimentSet, identified by name</li>
 * <li>A comma-delimited list of one or more EEs identified by short name given on the command line
 * <li>From a file, with one short name per line.
 * <li>EEs matching a query string (e.g., 'brain')
 * <li>(Optional) 'Auto' mode, in which experiments to analyze are selected automatically based on their workflow state.
 * This can be enabled and modified by subclasses who override the "needToRun" method.
 * <li>All EEs that were last processed after a given date, similar to 'auto' otherwise.
 * </ul>
 * Some of these options can be (or should be) combined, and modified by a (optional) "force" option, and will have
 * customized behavior.
 * <p>
 * In addition, EEs can be excluded based on a list given in a separate file.
 * 
 * @author Paul
 * @version $Id$
 */
public abstract class ExpressionExperimentManipulatingCLI extends AbstractSpringAwareCLI {

    protected ExpressionExperimentService eeService;

    protected GeneService geneService;

    protected SearchService searchService;

    protected TaxonService taxonService;

    protected Taxon taxon = null;

    protected Set<BioAssaySet> expressionExperiments = new HashSet<BioAssaySet>();

    protected Collection<BioAssaySet> excludeExperiments;

    protected boolean force = false;

    protected ExpressionExperimentSet expressionExperimentSet;

    protected AuditEventService auditEventService;

    protected void addForceOption() {
        this.addForceOption( null );
    }

    /**
     * 
     */
    @SuppressWarnings("static-access")
    protected void addForceOption( String explanation ) {
        String defaultExplanation = "Ignore other reasons for skipping experiments (e.g., trouble) and overwrite existing data (see documentation for this tool to see exact behavior if not clear)";
        String usedExpl = explanation == null ? defaultExplanation : explanation;
        Option forceOption = OptionBuilder.withArgName( "Force processing" ).withLongOpt( "force" ).withDescription(
                usedExpl ).create( "force" );
        addOption( forceOption );
    }

    @Override
    @SuppressWarnings("static-access")
    protected void buildOptions() {
        Option expOption = OptionBuilder
                .hasArg()
                .withArgName( "Expression experiment name" )
                .withDescription(
                        "Expression experiment short name. Most tools recognize comma-delimited values given on the command line, "
                                + "and if this option is omitted (and none other provided), the tool will be applied to all expression experiments." )
                .withLongOpt( "experiment" ).create( 'e' );

        addOption( expOption );

        Option eeFileListOption = OptionBuilder
                .hasArg()
                .withArgName( "Expression experiment list file" )
                .withDescription(
                        "File with list of short names or IDs of expression experiments (one per line; use instead of '-e')" )
                .withLongOpt( "eeListfile" ).create( 'f' );
        addOption( eeFileListOption );

        Option eeSetOption = OptionBuilder.hasArg().withArgName( "eeSetName" ).withDescription(
                "Name of expression experiment set to use" ).create( "eeset" );

        addOption( eeSetOption );

        Option taxonOption = OptionBuilder.hasArg().withDescription( "taxon name" ).withDescription(
                "Taxon of the expression experiments and genes" ).withLongOpt( "taxon" ).create( 't' );
        addOption( taxonOption );

        Option excludeEeOption = OptionBuilder.hasArg().withArgName( "Expression experiment list file" )
                .withDescription( "File containing list of expression experiments to exclude" ).withLongOpt(
                        "excludeEEFile" ).create( 'x' );
        addOption( excludeEeOption );

        Option eeSearchOption = OptionBuilder.hasArg().withArgName( "expressionQuery" ).withDescription(
                "Use a query string for defining which expression experiments to use" ).withLongOpt( "expressionQuery" )
                .create( 'q' );
        addOption( eeSearchOption );

    }

    /**
     * @param symbol
     * @param t
     * @return
     */
    protected Gene findGeneByOfficialSymbol( String symbol, Taxon t ) {
        Collection<Gene> genes = geneService.findByOfficialSymbolInexact( symbol );
        for ( Gene gene : genes ) {
            if ( t.equals( gene.getTaxon() ) ) return gene;
        }
        return null;
    }

    /**
     * @param ee
     * @return true if the expression experiment has an active 'trouble' flag
     */
    protected boolean isTroubled( BioAssaySet ee ) {
        Collection<BioAssaySet> eec = new HashSet<BioAssaySet>();
        eec.add( ee );
        removeTroubledEes( eec );
        if ( eec.size() == 0 ) {
            return true;
        }
        return false;
    }

    /**
     * @param short name of the experiment to find.
     * @return
     */
    protected ExpressionExperiment locateExpressionExperiment( String name ) {

        if ( name == null ) {
            errorObjects.add( "Expression experiment short name must be provided" );
            return null;
        }

        ExpressionExperiment experiment = eeService.findByShortName( name );

        if ( experiment == null ) {
            log.error( "No experiment " + name + " found" );
            bail( ErrorCode.INVALID_OPTION );
        }
        return experiment;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void processOptions() {
        super.processOptions();

        eeService = ( ExpressionExperimentService ) this.getBean( "expressionExperimentService" );
        geneService = ( GeneService ) this.getBean( "geneService" );
        taxonService = ( TaxonService ) getBean( "taxonService" );
        this.auditEventService = ( AuditEventService ) getBean( "auditEventService" );
        if ( hasOption( 't' ) ) {
            String taxonName = getOptionValue( 't' );
            this.taxon = taxonService.findByCommonName( taxonName );
            if ( taxon == null ) {
                log.error( "ERROR: Cannot find taxon " + taxonName );
            }
        }

        if ( hasOption( "force" ) ) {
            this.force = true;
        }

        if ( this.hasOption( "eeset" ) ) {
            experimentsFromEeSet( getOptionValue( "eeset" ) );
        } else if ( this.hasOption( 'e' ) ) {
            experimentsFromCliList();
        } else if ( hasOption( 'f' ) ) {
            String experimentListFile = getOptionValue( 'f' );
            log.info( "Reading experiment list from " + experimentListFile );
            try {
                this.expressionExperiments = readExpressionExperimentListFile( experimentListFile );
            } catch ( IOException e ) {
                throw new RuntimeException( e );
            }
        } else if ( hasOption( 'q' ) ) {
            log.info( "Processing all experiments that match query " + getOptionValue( 'q' ) );
            this.expressionExperiments = this.findExpressionExperimentsByQuery( getOptionValue( 'q' ) );
        } else if ( taxon != null ) {
            if ( !hasOption( "dataFile" ) ) {
                log.info( "Processing all experiments for " + taxon.getCommonName() );
                this.expressionExperiments = new HashSet( eeService.findByTaxon( taxon ) );
            }
        } else {
            if ( !hasOption( "dataFile" ) ) {
                log.info( "Processing all experiments (futher filtering may modify)" );
                this.expressionExperiments = new HashSet( eeService.loadAll() );
            }
        }

        if ( hasOption( 'x' ) ) {
            excludeFromFile();
        }

        if ( expressionExperiments != null && expressionExperiments.size() > 0 && !force ) {

            if ( hasOption( AUTO_OPTION_NAME ) ) {
                this.autoSeek = true;
                if ( this.autoSeekEventType == null ) {
                    throw new IllegalStateException( "Programming error: there is no 'autoSeekEventType' set" );
                }
                log.info( "Filtering for experiments lacking a " + this.autoSeekEventType.getSimpleName() + " event" );
                auditEventService.retainLackingEvent( this.expressionExperiments, this.autoSeekEventType );
            }

            if ( expressionExperiments.size() > 1 )
                log.info( "Thawing " + expressionExperiments.size() + " experiments ..." );
            int count = 0;
            for ( BioAssaySet ee : expressionExperiments ) {
                if ( ee instanceof ExpressionExperiment ) {
                    ee = eeService.thawLite( ( ExpressionExperiment ) ee );
                    if ( ++count % 25 == 0 ) {
                        log.info( "Thawed: " + count );
                    }
                } else {
                    throw new UnsupportedOperationException( "Can't handle non-EE BioAssaySets yet" );
                }
            }

            if ( expressionExperiments.size() > 1 ) log.info( "Done thawing" );

            removeTroubledEes( expressionExperiments );
        }

        if ( expressionExperiments.size() > 1 ) {
            log.info( "Final list: " + this.expressionExperiments.size()
                    + " expressionExperiments (futher filtering may modify)" );
        } else if ( expressionExperiments.size() == 0 ) {
            if ( hasOption( "dataFile" ) ) {
                log.info( "Expression matrix from data file selected" );
            } else {
                log.info( "No experiments selected" );
            }
        }

    }

    /**
     * Read in a list of genes
     * 
     * @param inFile - file name to read
     * @param t
     * @return collection of genes
     * @throws IOException
     */
    protected Collection<Gene> readGeneListFile( String inFile, Taxon t ) throws IOException {
        log.info( "Reading " + inFile );

        Collection<Gene> genes = new ArrayList<Gene>();
        BufferedReader in = new BufferedReader( new FileReader( inFile ) );
        String line;
        while ( ( line = in.readLine() ) != null ) {
            if ( line.startsWith( "#" ) ) continue;
            String s = line.trim();
            Gene gene = findGeneByOfficialSymbol( s, t );
            if ( gene == null ) {
                log.error( "ERROR: Cannot find genes for " + s );
                continue;
            }
            genes.add( gene );
        }
        return genes;
    }

    /**
     * 
     */
    private void excludeFromFile() {
        String excludeEeFileName = getOptionValue( 'x' );
        try {
            this.excludeExperiments = readExpressionExperimentListFile( excludeEeFileName );
        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }
        assert expressionExperiments.size() > 0;
        int count = 0;
        expressionExperiments.removeAll( excludeExperiments );
        if ( count > 0 ) log.info( "Excluded " + excludeExperiments.size() + " expression experiments" );
    }

    /**
     * 
     */
    private void experimentsFromCliList() {
        String experimentShortNames = this.getOptionValue( 'e' );
        String[] shortNames = experimentShortNames.split( "," );

        for ( String shortName : shortNames ) {
            ExpressionExperiment expressionExperiment = locateExpressionExperiment( shortName );
            if ( expressionExperiment == null ) {
                log.warn( shortName + " not found" );
                continue;
            }
            expressionExperiments.add( eeService.thawLite( expressionExperiment ) );
        }
        if ( expressionExperiments.size() == 0 ) {
            log.error( "There were no valid experimnents specified" );
            bail( ErrorCode.INVALID_OPTION );
        }
    }

    private void experimentsFromEeSet( String optionValue ) {

        if ( StringUtils.isBlank( optionValue ) ) {
            throw new IllegalArgumentException( "Please provide an eeset name" );
        }

        ExpressionExperimentSetService expressionExperimentSetService = ( ExpressionExperimentSetService ) this
                .getBean( "expressionExperimentSetService" );
        Collection<ExpressionExperimentSet> sets = expressionExperimentSetService.findByName( optionValue );
        if ( sets.size() > 1 ) {
            throw new IllegalArgumentException( "More than on EE set has name '" + optionValue + "'" );
        } else if ( sets.size() == 0 ) {
            throw new IllegalArgumentException( "No EE set has name '" + optionValue + "'" );
        }
        ExpressionExperimentSet set = sets.iterator().next();
        this.expressionExperimentSet = set;
        this.expressionExperiments = new HashSet<BioAssaySet>( set.getExperiments() );

    }

    /**
     * Use the search engine to locate expression experiments.
     * 
     * @param query
     */
    private Set<BioAssaySet> findExpressionExperimentsByQuery( String query ) {
        Set<BioAssaySet> ees = new HashSet<BioAssaySet>();
        Collection<SearchResult> eeSearchResults = searchService.search(
                SearchSettings.expressionExperimentSearch( query ) ).get( ExpressionExperiment.class );

        log.info( ees.size() + " Expression experiments matched '" + query + "'" );

        // Filter out all the ee that are not of correct taxon
        for ( SearchResult sr : eeSearchResults ) {
            ExpressionExperiment ee = ( ExpressionExperiment ) sr.getResultObject();
            Taxon t = eeService.getTaxon( ee.getId() );
            if ( t.getCommonName().equalsIgnoreCase( taxon.getCommonName() ) ) {
                ees.add( ee );
            }
        }
        return ees;

    }

    /**
     * Load expression experiments based on a list of short names or IDs in a file.
     * 
     * @param fileName
     * @return
     * @throws IOException
     */
    private Set<BioAssaySet> readExpressionExperimentListFile( String fileName ) throws IOException {
        Set<BioAssaySet> ees = new HashSet<BioAssaySet>();
        for ( String eeName : readExpressionExperimentListFileToStrings( fileName ) ) {
            ExpressionExperiment ee = eeService.findByShortName( eeName );
            if ( ee == null ) {

                try {
                    Long id = Long.parseLong( eeName );
                    ee = eeService.load( id );
                    if ( ee == null ) {
                        log.error( "No experiment " + eeName + " found" );
                        continue;
                    }
                } catch ( NumberFormatException e ) {
                    log.error( "No experiment " + eeName + " found" );
                    continue;

                }

            }
            ees.add( ee );
        }
        return ees;
    }

    /**
     * @param fileName
     * @return
     * @throws IOException
     */
    private Collection<String> readExpressionExperimentListFileToStrings( String fileName ) throws IOException {
        Collection<String> eeNames = new HashSet<String>();
        BufferedReader in = new BufferedReader( new FileReader( fileName ) );
        while ( in.ready() ) {
            String eeName = in.readLine().trim();
            if ( eeName.startsWith( "#" ) ) {
                continue;
            }
            eeNames.add( eeName );
        }
        return eeNames;
    }

    /**
     * TODO: replace with call to AuditableUtil.removeTroubledEes.
     * 
     * @param ees
     */
    @SuppressWarnings("unchecked")
    private void removeTroubledEes( Collection<BioAssaySet> ees ) {
        if ( ees == null || ees.size() == 0 ) {
            log.warn( "No experiments to remove troubled from" );
            return;
        }
        BioAssaySet theOnlyOne = null;
        if ( ees.size() == 1 ) {
            theOnlyOne = ees.iterator().next();
        }
        int size = ees.size();
        final Map<Long, AuditEvent> trouble = eeService.getLastTroubleEvent( CollectionUtils.collect( ees,
                new Transformer() {
                    @Override
                    public Object transform( Object input ) {
                        return ( ( ExpressionExperiment ) input ).getId();
                    }
                } ) );
        CollectionUtils.filter( ees, new Predicate() {
            @Override
            public boolean evaluate( Object object ) {
                boolean hasTrouble = trouble.containsKey( ( ( ExpressionExperiment ) object ).getId() );
                if ( hasTrouble ) {
                    log.info( "Troubled: " + object );
                }
                return !hasTrouble;
            }
        } );
        int newSize = ees.size();
        if ( newSize != size ) {
            assert newSize < size;
            if ( size == 0 && theOnlyOne != null ) {
                log.info( theOnlyOne.getName() + " has an active trouble flag" );
            } else {
                log.info( "Removed " + ( size - newSize ) + " experiments with 'trouble' flags, leaving " + newSize );
            }
        }
    }

}
