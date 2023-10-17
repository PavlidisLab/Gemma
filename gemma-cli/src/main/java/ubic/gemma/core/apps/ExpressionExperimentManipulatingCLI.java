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

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.lang3.StringUtils;
import ubic.gemma.core.apps.GemmaCLI.CommandGroup;
import ubic.gemma.core.genome.gene.service.GeneService;
import ubic.gemma.core.search.SearchException;
import ubic.gemma.core.search.SearchResult;
import ubic.gemma.core.search.SearchService;
import ubic.gemma.core.util.AbstractCLI;
import ubic.gemma.core.util.AbstractCLIContextCLI;
import ubic.gemma.model.analysis.expression.ExpressionExperimentSet;
import ubic.gemma.model.common.search.SearchSettings;
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
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

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
    private boolean allowProcessingAll = true;

    public Set<BioAssaySet> getExpressionExperiments() {
        return expressionExperiments;
    }

    protected ExpressionExperimentService getEeService() {
        return eeService;
    }

    protected Taxon getTaxon() {
        return taxon;
    }

    protected void setTaxon( Taxon taxon ) {
        this.taxon = taxon;
    }

    protected TaxonService getTaxonService() {
        return taxonService;
    }

    protected GeneService getGeneService() {
        return geneService;
    }

    boolean force = false;
    private Taxon taxon = null;
    private TaxonService taxonService;
    private GeneService geneService;
    private SearchService searchService;

    protected ExpressionExperimentManipulatingCLI() {
        super();
    }

    @Override
    public CommandGroup getCommandGroup() {
        return CommandGroup.EXPERIMENT;
    }

    @SuppressWarnings("AccessStaticViaInstance") // Cleaner like this
    @Override
    protected void buildOptions( Options options ) {
        Option expOption = Option.builder( "e" ).hasArg().argName( "shortname" ).desc(
                        "Expression experiment short name. Most tools recognize comma-delimited values given on the command line, "
                                + "and if this option is omitted (and none other provided), the tool will be applied to all expression experiments." )
                .longOpt( "experiment" ).build();

        options.addOption( expOption );

        Option eeFileListOption = Option.builder( "f" ).hasArg().argName( "file" ).desc(
                        "File with list of short names or IDs of expression experiments (one per line; use instead of '-e')" )
                .longOpt( "eeListfile" ).build();
        options.addOption( eeFileListOption );

        Option eeSetOption = Option.builder( "eeset" ).hasArg().argName( "eeSetName" )
                .desc( "Name of expression experiment set to use" ).build();

        options.addOption( eeSetOption );

        Option taxonOption = Option.builder( "t" ).hasArg().argName( "taxon name" )
                .desc( "Taxon of the expression experiments and genes" ).longOpt( "taxon" )
                .build();
        options.addOption( taxonOption );

        Option excludeEeOption = Option.builder( "x" ).hasArg().argName( "file" )
                .desc( "File containing list of expression experiments to exclude" )
                .longOpt( "excludeEEFile" ).build();
        options.addOption( excludeEeOption );

        Option eeSearchOption = Option.builder( "q" ).hasArg().argName( "expressionQuery" )
                .desc( "Use a query string for defining which expression experiments to use" )
                .longOpt( "expressionQuery" ).build();
        options.addOption( eeSearchOption );

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
    protected void processOptions( CommandLine commandLine ) {
        eeService = this.getBean( ExpressionExperimentService.class );
        geneService = this.getBean( GeneService.class );
        taxonService = this.getBean( TaxonService.class );
        searchService = this.getBean( SearchService.class );
        this.auditEventService = this.getBean( AuditEventService.class );
        if ( commandLine.hasOption( 't' ) ) {
            this.taxon = this.setTaxonByName( commandLine, taxonService );
        }

        if ( commandLine.hasOption( "force" ) ) {
            this.force = true;
        }

        if ( commandLine.hasOption( "eeset" ) ) {
            this.experimentsFromEeSet( commandLine.getOptionValue( "eeset" ) );
        } else if ( commandLine.hasOption( 'e' ) ) {
            this.experimentsFromCliList( commandLine );
        } else if ( commandLine.hasOption( 'f' ) ) {
            String experimentListFile = commandLine.getOptionValue( 'f' );
            AbstractCLI.log.info( "Reading experiment list from " + experimentListFile );
            try {
                this.expressionExperiments = this.readExpressionExperimentListFile( experimentListFile );
            } catch ( IOException e ) {
                throw new RuntimeException( e );
            }
        } else if ( commandLine.hasOption( 'q' ) ) {
            AbstractCLI.log.info( "Processing all experiments that match query " + commandLine.getOptionValue( 'q' ) );
            try {
                this.expressionExperiments = this.findExpressionExperimentsByQuery( commandLine.getOptionValue( 'q' ) );
            } catch ( SearchException e ) {
                log.error( "Failed to retrieve EEs for the passed query via -q.", e );
            }
        } else if ( taxon != null ) {
            if ( !commandLine.hasOption( "dataFile" ) ) {
                AbstractCLI.log.info( "Processing all experiments for " + taxon.getCommonName() );
                this.expressionExperiments = new HashSet<BioAssaySet>( eeService.findByTaxon( taxon ) );
            }
        } else {
            if ( !commandLine.hasOption( "dataFile" ) && allowProcessingAll ) {
                AbstractCLI.log.info( "Processing all experiments (further filtering may modify)" );
                this.expressionExperiments = new HashSet<BioAssaySet>( eeService.loadAll() );
            }
        }

        if ( commandLine.hasOption( 'x' ) ) {
            this.excludeFromFile( commandLine );
        }

        if ( expressionExperiments != null && expressionExperiments.size() > 0 && !force ) {

            if ( isAutoSeek() ) {
                if ( this.getAutoSeekEventType() == null ) {
                    throw new IllegalStateException( "Programming error: there is no 'autoSeekEventType' set" );
                }
                AbstractCLI.log.info( "Filtering for experiments lacking a " + this.getAutoSeekEventType().getSimpleName()
                        + " event" );
                auditEventService.retainLackingEvent( this.expressionExperiments, this.getAutoSeekEventType() );
            }

            Set<BioAssaySet> troubledExpressionExperiments = this.getTroubledExpressionExperiments();

            // only retain non-troubled experiments
            expressionExperiments.removeAll( troubledExpressionExperiments );

            if ( troubledExpressionExperiments.size() == 1 ) {
                AbstractCLI.log.info( troubledExpressionExperiments.stream().findFirst().get().getName() + " has an active trouble flag" );
            } else if ( troubledExpressionExperiments.size() > 1 ) {
                AbstractCLI.log.info( "Removed " + troubledExpressionExperiments.size() + " experiments with 'trouble' flags, leaving "
                        + expressionExperiments.size() );
            }
        }

        if ( expressionExperiments != null && expressionExperiments.size() > 1 ) {
            AbstractCLI.log.info( "Final list: " + this.expressionExperiments.size()
                    + " expressionExperiments (futher filtering may modify)" );
        } else if ( expressionExperiments == null || expressionExperiments.size() == 0 ) {
            if ( commandLine.hasOption( "dataFile" ) ) {
                //    AbstractCLI.log.info( "Expression matrix from data file selected" );
            } else {
                //   AbstractCLI.log.info( "No experiments selected" );
            }
        }

    }

    void addForceOption( Options options ) {
        String desc = "Ignore other reasons for skipping experiments (e.g., trouble) and overwrite existing data (see documentation for this tool to see exact behavior if not clear)";
        @SuppressWarnings("static-access")
        Option forceOption = Option.builder( "force" ).longOpt( "force" ).desc( desc ).build();
        options.addOption( forceOption );
    }

    private void excludeFromFile( CommandLine commandLine ) {
        String excludeEeFileName = commandLine.getOptionValue( 'x' );
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

    private void experimentsFromCliList( CommandLine commandLine ) {
        String experimentShortNames = commandLine.getOptionValue( 'e' );
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
            throw new RuntimeException( "There were no valid experimnents specified" );
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
    private Set<BioAssaySet> findExpressionExperimentsByQuery( String query ) throws SearchException {
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

        Collection<SearchResult<ExpressionExperiment>> eeSearchResults = searchService
                .search( SearchSettings.expressionExperimentSearch( query ) )
                .getByResultObjectType( ExpressionExperiment.class );

        // Filter out all the ee that are not of correct taxon
        for ( SearchResult<ExpressionExperiment> sr : eeSearchResults ) {
            ExpressionExperiment ee = sr.getResultObject();
            if ( ee == null )
                continue; // ee no longer valid, could be an outdated compass hit
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
            addErrorObject( null, "Expression experiment short name must be provided" );
            return null;
        }

        ExpressionExperiment experiment = eeService.findByShortName( name );

        if ( experiment == null ) {
            throw new RuntimeException( "No experiment " + name + " found" );
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
     * Obtain EEs that are troubled among {@link ExpressionExperimentManipulatingCLI#expressionExperiments}.
     * @return a collection of troubled experiemnt, or an empty set of non are
     */
    private Set<BioAssaySet> getTroubledExpressionExperiments() {
        if ( expressionExperiments == null || expressionExperiments.size() == 0 ) {
            AbstractCLI.log.warn( "No experiments to remove troubled from" );
            return Collections.emptySet();
        }

        return expressionExperiments.stream()
                .map( ExpressionExperiment.class::cast )
                .filter( ee -> ee.getCurationDetails().getTroubled() )
                .collect( Collectors.toSet() );
    }

    /**
     * Disable the ability for this CLI to process all experiments when no other specification is given.
     * The user must explicitly define the experiments to be processed.
     */
    protected void suppressAllOption() {
        this.allowProcessingAll = false;
    }
}
