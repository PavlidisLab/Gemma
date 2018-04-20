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
package ubic.gemma.core.apps;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.apache.commons.lang3.StringUtils;
import ubic.gemma.core.apps.GemmaCLI.CommandGroup;
import ubic.gemma.core.genome.gene.service.GeneService;
import ubic.gemma.core.search.SearchResult;
import ubic.gemma.core.search.SearchService;
import ubic.gemma.core.util.AbstractCLI;
import ubic.gemma.core.util.AbstractCLIContextCLI;
import ubic.gemma.model.analysis.expression.ExpressionExperimentSet;
import ubic.gemma.model.common.search.SearchSettingsImpl;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditEventService;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentSetService;
import ubic.gemma.persistence.service.genome.taxon.TaxonService;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

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
 * In addition, EEs can be excluded based on a list given in a separate file.
 *
 * @author Paul
 */
public abstract class ExpressionExperimentManipulatingCLI extends AbstractCLIContextCLI {
    ExpressionExperimentService eeService;
    Set<BioAssaySet> expressionExperiments = new HashSet<>();
    boolean force = false;
    Taxon taxon = null;
    TaxonService taxonService;
    private GeneService geneService;
    private SearchService searchService;

    @Override
    public CommandGroup getCommandGroup() {
        return CommandGroup.EXPERIMENT;
    }

    @SuppressWarnings("AccessStaticViaInstance") // Cleaner like this
    @Override
    protected void buildOptions() {
        Option expOption = OptionBuilder.hasArg().withArgName( "shortname" ).withDescription(
                "Expression experiment short name. Most tools recognize comma-delimited values given on the command line, "
                        + "and if this option is omitted (and none other provided), the tool will be applied to all expression experiments." )
                .withLongOpt( "experiment" ).create( 'e' );

        this.addOption( expOption );

        Option eeFileListOption = OptionBuilder.hasArg().withArgName( "file" ).withDescription(
                "File with list of short names or IDs of expression experiments (one per line; use instead of '-e')" )
                .withLongOpt( "eeListfile" ).create( 'f' );
        this.addOption( eeFileListOption );

        Option eeSetOption = OptionBuilder.hasArg().withArgName( "eeSetName" )
                .withDescription( "Name of expression experiment set to use" ).create( "eeset" );

        this.addOption( eeSetOption );

        Option taxonOption = OptionBuilder.hasArg().withDescription( "taxon name" )
                .withDescription( "Taxon of the expression experiments and genes" ).withLongOpt( "taxon" )
                .create( 't' );
        this.addOption( taxonOption );

        Option excludeEeOption = OptionBuilder.hasArg().withArgName( "file" )
                .withDescription( "File containing list of expression experiments to exclude" )
                .withLongOpt( "excludeEEFile" ).create( 'x' );
        this.addOption( excludeEeOption );

        Option eeSearchOption = OptionBuilder.hasArg().withArgName( "expressionQuery" )
                .withDescription( "Use a query string for defining which expression experiments to use" )
                .withLongOpt( "expressionQuery" ).create( 'q' );
        this.addOption( eeSearchOption );

    }

    @SuppressWarnings("unused") // Possible external use
    protected Gene findGeneByOfficialSymbol( String symbol, Taxon t ) {
        Collection<Gene> genes = geneService.findByOfficialSymbolInexact( symbol );
        for ( Gene gene : genes ) {
            if ( t.equals( gene.getTaxon() ) )
                return gene;
        }
        return null;
    }

    @Override
    protected void processOptions() {
        super.processOptions();

        eeService = this.getBean( ExpressionExperimentService.class );
        geneService = this.getBean( GeneService.class );
        taxonService = this.getBean( TaxonService.class );
        searchService = this.getBean( SearchService.class );
        this.auditEventService = this.getBean( AuditEventService.class );
        if ( this.hasOption( 't' ) ) {
            this.taxon = this.setTaxonByName( taxonService );
        }

        if ( this.hasOption( "force" ) ) {
            this.force = true;
        }

        if ( this.hasOption( "eeset" ) ) {
            this.experimentsFromEeSet( this.getOptionValue( "eeset" ) );
        } else if ( this.hasOption( 'e' ) ) {
            this.experimentsFromCliList();
        } else if ( this.hasOption( 'f' ) ) {
            String experimentListFile = this.getOptionValue( 'f' );
            AbstractCLI.log.info( "Reading experiment list from " + experimentListFile );
            try {
                this.expressionExperiments = this.readExpressionExperimentListFile( experimentListFile );
            } catch ( IOException e ) {
                throw new RuntimeException( e );
            }
        } else if ( this.hasOption( 'q' ) ) {
            AbstractCLI.log.info( "Processing all experiments that match query " + this.getOptionValue( 'q' ) );
            this.expressionExperiments = this.findExpressionExperimentsByQuery( this.getOptionValue( 'q' ) );
        } else if ( taxon != null ) {
            if ( !this.hasOption( "dataFile" ) ) {
                AbstractCLI.log.info( "Processing all experiments for " + taxon.getCommonName() );
                this.expressionExperiments = new HashSet<BioAssaySet>( eeService.findByTaxon( taxon ) );
            }
        } else {
            if ( !this.hasOption( "dataFile" ) ) {
                AbstractCLI.log.info( "Processing all experiments (further filtering may modify)" );
                this.expressionExperiments = new HashSet<BioAssaySet>( eeService.loadAll() );
            }
        }

        if ( this.hasOption( 'x' ) ) {
            this.excludeFromFile();
        }

        if ( expressionExperiments != null && expressionExperiments.size() > 0 && !force ) {

            if ( this.hasOption( AbstractCLI.AUTO_OPTION_NAME ) ) {
                this.autoSeek = true;
                if ( this.autoSeekEventType == null ) {
                    throw new IllegalStateException( "Programming error: there is no 'autoSeekEventType' set" );
                }
                AbstractCLI.log.info( "Filtering for experiments lacking a " + this.autoSeekEventType.getSimpleName()
                        + " event" );
                auditEventService.retainLackingEvent( this.expressionExperiments, this.autoSeekEventType );
            }

            this.removeTroubledEes( expressionExperiments );
        }

        if ( expressionExperiments != null && expressionExperiments.size() > 1 ) {
            AbstractCLI.log.info( "Final list: " + this.expressionExperiments.size()
                    + " expressionExperiments (futher filtering may modify)" );
        } else if ( ( expressionExperiments != null && expressionExperiments.size() == 0 )
                || expressionExperiments == null ) {
            if ( this.hasOption( "dataFile" ) ) {
                AbstractCLI.log.info( "Expression matrix from data file selected" );
            } else {
                AbstractCLI.log.info( "No experiments selected" );
            }
        }

    }

    void addForceOption() {
        String defaultExplanation = "Ignore other reasons for skipping experiments (e.g., trouble) and overwrite existing data (see documentation for this tool to see exact behavior if not clear)";
        @SuppressWarnings("static-access")
        Option forceOption = OptionBuilder.withArgName( "Force processing" )
                .withLongOpt( "force" ).withDescription( defaultExplanation ).create( "force" );
        this.addOption( forceOption );
    }

    private void excludeFromFile() {
        String excludeEeFileName = this.getOptionValue( 'x' );
        Collection<BioAssaySet> excludeExperiments;
        try {
            excludeExperiments = this.readExpressionExperimentListFile( excludeEeFileName );
        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }
        assert expressionExperiments.size() > 0;

        int before = expressionExperiments.size();

        expressionExperiments.removeAll( excludeExperiments );
        int removed = before - expressionExperiments.size();

        if ( removed > 0 )
            AbstractCLI.log.info( "Excluded " + removed + " expression experiments" );
    }

    private void experimentsFromCliList() {
        String experimentShortNames = this.getOptionValue( 'e' );
        String[] shortNames = experimentShortNames.split( "," );

        for ( String shortName : shortNames ) {
            ExpressionExperiment expressionExperiment = this.locateExpressionExperiment( shortName );
            if ( expressionExperiment == null ) {
                AbstractCLI.log.warn( shortName + " not found" );
                continue;
            }
            eeService.thawLite( expressionExperiment );
            expressionExperiments.add( expressionExperiment );
        }
        if ( expressionExperiments.size() == 0 ) {
            AbstractCLI.log.error( "There were no valid experimnents specified" );
            this.bail( ErrorCode.INVALID_OPTION );
        }
    }

    private void experimentsFromEeSet( String optionValue ) {

        if ( StringUtils.isBlank( optionValue ) ) {
            throw new IllegalArgumentException( "Please provide an eeset name" );
        }

        ExpressionExperimentSetService expressionExperimentSetService = this
                .getBean( ExpressionExperimentSetService.class );
        Collection<ExpressionExperimentSet> sets = expressionExperimentSetService.findByName( optionValue );
        if ( sets.size() > 1 ) {
            throw new IllegalArgumentException( "More than on EE set has name '" + optionValue + "'" );
        } else if ( sets.size() == 0 ) {
            throw new IllegalArgumentException( "No EE set has name '" + optionValue + "'" );
        }
        ExpressionExperimentSet set = sets.iterator().next();
        this.expressionExperiments = new HashSet<>( set.getExperiments() );

    }

    /**
     * Use the search engine to locate expression experiments.
     */
    private Set<BioAssaySet> findExpressionExperimentsByQuery( String query ) {
        Set<BioAssaySet> ees = new HashSet<>();

        // explicitly support one case
        if ( query.matches( "GPL[0-9]+" ) ) {
            ArrayDesign ad = this.getBean( ArrayDesignService.class ).findByShortName( query );
            if ( ad != null ) {
                Collection<ExpressionExperiment> ees2 = this.getBean( ArrayDesignService.class ).getExpressionExperiments( ad );
                ees.addAll( ees2 );
                log.info( ees.size() + " experiments matched to platform " + ad );
            }
            return ees;
        }

        Collection<SearchResult> eeSearchResults = searchService
                .search( SearchSettingsImpl.expressionExperimentSearch( query ) ).get( ExpressionExperiment.class );

        // Filter out all the ee that are not of correct taxon
        for ( SearchResult sr : eeSearchResults ) {
            ExpressionExperiment ee = ( ExpressionExperiment ) sr.getResultObject();
            Taxon t = eeService.getTaxon( ee );
            if ( t != null && t.getCommonName().equalsIgnoreCase( taxon.getCommonName() ) ) {
                ees.add( ee );
            }
        }

        AbstractCLI.log.info( ees.size() + " Expression experiments matched '" + query + "'" );

        return ees;

    }

    private ExpressionExperiment locateExpressionExperiment( String name ) {

        if ( name == null ) {
            errorObjects.add( "Expression experiment short name must be provided" );
            return null;
        }

        ExpressionExperiment experiment = eeService.findByShortName( name );

        if ( experiment == null ) {
            AbstractCLI.log.error( "No experiment " + name + " found" );
            this.bail( ErrorCode.INVALID_OPTION );
        }
        return experiment;
    }

    /**
     * Load expression experiments based on a list of short names or IDs in a file. Only the first column of the file is
     * used, comments (#) are allowed.
     */
    private Set<BioAssaySet> readExpressionExperimentListFile( String fileName ) throws IOException {
        Set<BioAssaySet> ees = new HashSet<>();
        for ( String eeName : AbstractCLIContextCLI.readListFileToStrings( fileName ) ) {
            ExpressionExperiment ee = eeService.findByShortName( eeName );
            if ( ee == null ) {

                try {
                    Long id = Long.parseLong( eeName );
                    ee = eeService.load( id );
                    if ( ee == null ) {
                        AbstractCLI.log.error( "No experiment " + eeName + " found" );
                        continue;
                    }
                } catch ( NumberFormatException e ) {
                    AbstractCLI.log.error( "No experiment " + eeName + " found" );
                    continue;

                }

            }
            ees.add( ee );
        }
        return ees;
    }

    /**
     * removes EEs that are troubled, or their parent Array design is troubled.
     */
    private void removeTroubledEes( Collection<BioAssaySet> ees ) {
        if ( ees == null || ees.size() == 0 ) {
            AbstractCLI.log.warn( "No experiments to remove troubled from" );
            return;
        }

        BioAssaySet theOnlyOne = null;
        if ( ees.size() == 1 ) {
            theOnlyOne = ees.iterator().next();
        }
        int size = ees.size();

        CollectionUtils.filter( ees, new Predicate() {
            @Override
            public boolean evaluate( Object object ) {
                return !( ( ExpressionExperiment ) object ).getCurationDetails().getTroubled();
            }
        } );
        int newSize = ees.size();
        if ( newSize != size ) {
            assert newSize < size;
            if ( size == 1 && theOnlyOne != null ) {
                AbstractCLI.log.info( theOnlyOne.getName() + " has an active trouble flag" );
            } else {
                AbstractCLI.log.info( "Removed " + ( size - newSize ) + " experiments with 'trouble' flags, leaving "
                        + newSize );
            }
        }
    }

}
