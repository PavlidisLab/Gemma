package ubic.gemma.core.apps;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import ubic.gemma.core.analysis.sequence.ProbeMapperConfig;
import ubic.gemma.core.loader.expression.arrayDesign.ArrayDesignProbeMapperService;
import ubic.gemma.core.util.AbstractCLI;
import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.*;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.TechnologyType;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.sequenceAnalysis.BlatAssociation;
import ubic.gemma.persistence.service.common.description.ExternalDatabaseService;
import ubic.gemma.persistence.service.expression.designElement.CompositeSequenceService;
import ubic.gemma.persistence.service.genome.taxon.TaxonService;
import ubic.gemma.persistence.util.Settings;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.Callable;

/**
 * Process the blat results for an array design to map them onto genes. Typical workflow would be to run:
 * <ol>
 * <li>Create the array design, perhaps by loading a GPL or via a GSE.
 * <li>ArrayDesignSequenceAssociationCli - attach sequences to array design, fetching from BLAST database if necessary.
 * For affymetrix designs, get the probe sequences and pass them to the command line (also a web interface)
 * <li>ArrayDesignBlatCli - runs blat. You must start the appropriate server using the configuration in the properties
 * files.
 * <li>ArrayDesignProbeMapperCli (this class); you must have the correct GoldenPath database installed and available as
 * configured in your properties files.
 * </ol>
 * This can also allow directly associating probes with genes (via products) based on an input file, without any
 * sequence analysis.
 * <p>
 * In batch mode, platforms that are "children" (mergees or subsumees) of other platforms will be skipped. Platforms
 * which are themselves merged or subsumers, when run, will result in the child platforms being updated implicitly (via
 * an audit event and report update)
 *
 * @author pavlidis
 */
public class ArrayDesignProbeMapperCli extends ArrayDesignSequenceManipulatingCli {
    private static final String CONFIG_OPTION = "config";
    private static final String MIRNA_ONLY_MODE_OPTION = "mirna";
    private final static String OPTION_ENSEMBL = "n";
    private final static String OPTION_EST = "e"; // usually off
    private final static String OPTION_KNOWNGENE = "k";
    private final static String OPTION_MICRORNA = "i";
    private final static String OPTION_MRNA = "m";
    private final static String OPTION_REFSEQ = "r";

    private String[] probeNames = null;
    private ProbeMapperConfig config;
    private ArrayDesignProbeMapperService arrayDesignProbeMapperService;
    private String directAnnotationInputFileName = null;
    private boolean ncbiIds = false;
    private ExternalDatabase sourceDatabase = null;
    private Taxon taxon = null;
    private boolean useDB = true;
    private boolean force = false;
    private Double blatScoreThreshold = null;
    private boolean usePred;
    private String configOption = null;
    private boolean mirnaOnlyModeOption = false;
    private Double identityThreshold = null;
    private Double overlapThreshold = null;

    @Override
    public GemmaCLI.CommandGroup getCommandGroup() {
        return GemmaCLI.CommandGroup.PLATFORM;
    }

    @SuppressWarnings({ "AccessStaticViaInstance", "static-access", "deprecation" })
    @Override
    protected void buildOptions( Options options ) {
        super.buildOptions( options );

        options.addOption( Option.builder( "i" ).hasArg().argName( "value" ).desc(
                        "Sequence identity threshold, default = " + ProbeMapperConfig.DEFAULT_IDENTITY_THRESHOLD )
                .longOpt( "identityThreshold" ).build() );

        options.addOption( Option.builder( "s" ).hasArg().argName( "value" )
                .desc( "Blat score threshold, default = " + ProbeMapperConfig.DEFAULT_SCORE_THRESHOLD )
                .longOpt( "scoreThreshold" ).build() );

        options.addOption( Option.builder( "o" ).hasArg().argName( "value" ).desc(
                        "Minimum fraction of probe overlap with exons, default = "
                                + ProbeMapperConfig.DEFAULT_MINIMUM_EXON_OVERLAP_FRACTION )
                .longOpt( "overlapThreshold" )
                .build() );

        options.addOption( Option.builder( "usePred" ).desc(
                        "Allow mapping to predicted genes (overrides Acembly, Ensembl and Nscan; default="
                                + ProbeMapperConfig.DEFAULT_ALLOW_PREDICTED + ")" )
                .build() );

        options.addOption( Option.builder( ArrayDesignProbeMapperCli.CONFIG_OPTION ).hasArg().argName( "configstring" ).desc(
                        "String describing which tracks to search, for example 'rkenmias' for all, 'rm' to limit search to Refseq with mRNA evidence. If this option is not set,"
                                + " all will be used except as listed below and in combination with the 'usePred' option:\n "

                                + ArrayDesignProbeMapperCli.OPTION_REFSEQ
                                + " - search refseq track for genes (best to leave on)\n"

                                + ArrayDesignProbeMapperCli.OPTION_KNOWNGENE
                                + " - search knownGene track for genes (best to leave on, but track may be missing for some organisms)\n"

                                + ArrayDesignProbeMapperCli.OPTION_MICRORNA + " - search miRNA track for genes (Default = false)\n"

                                + ArrayDesignProbeMapperCli.OPTION_EST + " - search EST track for transcripts (Default=false)\n"

                                + ArrayDesignProbeMapperCli.OPTION_MRNA
                                + " - search mRNA track for transcripts (Default=false)\n"

                                + ArrayDesignProbeMapperCli.OPTION_ENSEMBL + " - search Ensembl track for predicted genes (Default=false) \n" )
                .build() );

        options.addOption( Option.builder( ArrayDesignProbeMapperCli.MIRNA_ONLY_MODE_OPTION ).desc(
                        "Only seek miRNAs; this is the same as '-config " + ArrayDesignProbeMapperCli.OPTION_MICRORNA
                                + "; overrides -config." )
                .build() );

        Option taxonOption = Option.builder( "t" ).hasArg().argName( "taxon" ).desc(
                        "Taxon common name (e.g., human); if using '-import', this taxon will be assumed; otherwise analysis will be run for all"
                                + " ArrayDesigns from that taxon (overrides -a)" )
                .build();

        options.addOption( taxonOption );

        Option force = Option.builder( "force" ).desc( "Run no matter what" ).build();

        options.addOption( force );

        Option directAnnotation = Option.builder( "import" ).desc(
                        "Import annotations from a file rather than our own analysis. You must provide the taxon option. "
                                + "File format: 2 columns with column 1= probe name in Gemma, "
                                + "column 2=sequence name (not required, and not used for direct gene-based annotation)"
                                + " column 3 = gene symbol (will be matched to that in Gemma)" )
                .hasArg().argName( "file" )
                .build();

        options.addOption( directAnnotation );

        options.addOption( "ncbi", "If set, it is assumed the direct annotation file is NCBI gene ids, not gene symbols (only valid with -import)" );

        Option databaseOption = Option.builder( "source" )
                .desc( "Source database name (GEO etc); required if using -import" ).hasArg()
                .argName( "dbname" ).build();
        options.addOption( databaseOption );

        Option noDatabaseOption = Option.builder( "nodb" )
                .desc( "Don't save the results to the database, print to stdout instead (not with -import)" )
                .build();

        options.addOption( noDatabaseOption );

        Option probesToDoOption = Option.builder( "probes" )
                .desc( "Comma-delimited list of probe names to process (for testing); implies -nodb" )
                .hasArg().argName( "probes" ).build();

        options.addOption( probesToDoOption );
    }

    @Override
    protected boolean requireLogin() {
        return true;
    }

    private TaxonService taxonService;

    /**
     * See 'configure' for how the other options are handled. (non-Javadoc)
     *
     * @see AbstractCLI#processOptions(CommandLine)
     */
    @Override
    protected void processOptions( CommandLine commandLine ) {
        super.processOptions( commandLine );
        arrayDesignProbeMapperService = this.getBean( ArrayDesignProbeMapperService.class );
        taxonService = this.getBean( TaxonService.class );

        if ( commandLine.hasOption( "import" ) ) {
            if ( !commandLine.hasOption( 't' ) ) {
                throw new IllegalArgumentException( "You must provide the taxon when using the import option" );
            }
            if ( !commandLine.hasOption( "source" ) ) {
                throw new IllegalArgumentException(
                        "You must provide source database name when using the import option" );
            }
            String sourceDBName = commandLine.getOptionValue( "source" );

            ExternalDatabaseService eds = this.getBean( ExternalDatabaseService.class );

            this.sourceDatabase = eds.findByName( sourceDBName );

            this.directAnnotationInputFileName = commandLine.getOptionValue( "import" );

            if ( commandLine.hasOption( "ncbi" ) ) {
                this.ncbiIds = true;
            }
        }
        if ( commandLine.hasOption( 't' ) ) {
            this.taxon = this.setTaxonByName( commandLine, taxonService );
        }

        if ( commandLine.hasOption( "nodb" ) ) {
            this.useDB = false;
        }

        if ( commandLine.hasOption( "probes" ) ) {

            if ( this.getArrayDesignsToProcess() == null || this.getArrayDesignsToProcess().size() > 1 ) {
                throw new IllegalArgumentException( "With '-probes' you must provide exactly one platform" );
            }

            this.useDB = false;
            probeNames = commandLine.getOptionValue( "probes" ).split( "," );

            if ( probeNames.length == 0 ) {
                throw new IllegalArgumentException( "You must provide at least one probe name when using '-probes'" );
            }
        }

        if ( commandLine.hasOption( 's' ) ) {
            double result;
            try {
                result = Double.parseDouble( commandLine.getOptionValue( 's' ) );
            } catch ( NumberFormatException e ) {
                String option1 = String.valueOf( 's' );
                throw new RuntimeException( "Invalid value '" + commandLine.getOptionValue( option1 ) + " for option " + option1 + ", not a valid double", e );
            }
            blatScoreThreshold = result;
            if ( blatScoreThreshold < 0 || blatScoreThreshold > 1 ) {
                throw new IllegalArgumentException( "BLAT score threshold must be between 0 and 1" );
            }
        }

        this.usePred = commandLine.hasOption( "usePred" );

        if ( commandLine.hasOption( ArrayDesignProbeMapperCli.CONFIG_OPTION ) ) {
            this.configOption = commandLine.getOptionValue( ArrayDesignProbeMapperCli.CONFIG_OPTION );
        }

        this.mirnaOnlyModeOption = commandLine.hasOption( ArrayDesignProbeMapperCli.MIRNA_ONLY_MODE_OPTION );

        if ( commandLine.hasOption( 'i' ) ) {
            double result;
            try {
                result = Double.parseDouble( commandLine.getOptionValue( 'i' ) );
            } catch ( NumberFormatException e ) {
                String option1 = String.valueOf( 'i' );
                throw new RuntimeException( "Invalid value '" + commandLine.getOptionValue( option1 ) + " for option " + option1 + ", not a valid double", e );
            }
            identityThreshold = result;
            if ( identityThreshold < 0 || identityThreshold > 1 ) {
                throw new IllegalArgumentException( "Identity threshold must be between 0 and 1" );
            }
        }

        if ( commandLine.hasOption( 'o' ) ) {
            double result;
            try {
                result = Double.parseDouble( commandLine.getOptionValue( 'o' ) );
            } catch ( NumberFormatException e ) {
                String option1 = String.valueOf( 'o' );
                throw new RuntimeException( "Invalid value '" + commandLine.getOptionValue( option1 ) + " for option " + option1 + ", not a valid double", e );
            }
            overlapThreshold = result;
            if ( overlapThreshold < 0 || overlapThreshold > 1 ) {
                throw new IllegalArgumentException( "Overlap threshold must be between 0 and 1" );
            }
        }

        this.force = commandLine.hasOption( "force" );
    }

    /**
     * Override to do additional checks to make sure the array design is in a state of readiness for probe mapping.
     */
    @Override
    boolean needToRun( Date skipIfLastRunLaterThan, ArrayDesign arrayDesign,
            Class<? extends ArrayDesignAnalysisEvent> eventClass ) {

        if ( this.force ) {
            return true;
        }

        if ( this.directAnnotationInputFileName != null ) {
            return true;
        }

        arrayDesign = getArrayDesignService().thawLite( arrayDesign );

        /*
         * Do not run this on "Generic" platforms or those which are loaded using a direct annotation input file!
         */
        if ( arrayDesign.getTechnologyType().equals( TechnologyType.GENELIST ) || arrayDesign.getTechnologyType().equals( TechnologyType.SEQUENCING )
                || arrayDesign.getTechnologyType().equals( TechnologyType.OTHER ) ) {
            AbstractCLI.log.info( "Skipping because it is not a microarray platform" );
            return false;
        }

        if ( !super.needToRun( skipIfLastRunLaterThan, arrayDesign, eventClass ) ) {
            return false;
        }

        AbstractCLI.log.debug( "Re-Checking status of " + arrayDesign );

        List<AuditEvent> allEvents = this.auditTrailService.getEvents( arrayDesign );
        AuditEvent lastSequenceAnalysis = null;
        AuditEvent lastRepeatMask = null;
        AuditEvent lastSequenceUpdate = null;
        AuditEvent lastProbeMapping = null;

        AbstractCLI.log.debug( allEvents.size() + " to inspect" );
        for ( int j = allEvents.size() - 1; j >= 0; j-- ) {
            AuditEvent currentEvent = allEvents.get( j );
            if ( currentEvent == null )
                continue; // legacy of ordered-list which could end up with gaps; should not be
            // needed any more
            if ( currentEvent.getEventType() == null )
                continue;

            // we only care about ArrayDesignAnalysisEvent events.
            Class<? extends AuditEventType> currentEventClass = currentEvent.getEventType().getClass();

            AbstractCLI.log.debug( "Inspecting: " + currentEventClass );

            // Get the most recent event of each type.
            if ( lastRepeatMask == null && ArrayDesignRepeatAnalysisEvent.class
                    .isAssignableFrom( currentEventClass ) ) {
                lastRepeatMask = currentEvent;
                AbstractCLI.log.debug( "Last repeat mask: " + lastRepeatMask.getDate() );
            } else if ( lastSequenceUpdate == null && ArrayDesignSequenceUpdateEvent.class
                    .isAssignableFrom( currentEventClass ) ) {
                lastSequenceUpdate = currentEvent;
                AbstractCLI.log.debug( "Last sequence update: " + lastSequenceUpdate.getDate() );
            } else if ( lastSequenceAnalysis == null && ArrayDesignSequenceAnalysisEvent.class
                    .isAssignableFrom( currentEventClass ) ) {
                lastSequenceAnalysis = currentEvent;
                AbstractCLI.log.debug( "Last sequence analysis: " + lastSequenceAnalysis.getDate() );
            } else if ( lastProbeMapping == null && ArrayDesignGeneMappingEvent.class
                    .isAssignableFrom( currentEventClass ) ) {
                lastProbeMapping = currentEvent;
                AbstractCLI.log.info( "Last probe mapping analysis: " + lastProbeMapping.getDate() );
            }

        }

        /*
         *
         * The issue addressed here is that we don't normally run sequence analysis etc. on merged platforms either, so
         * it looks like they are not ready for probemapping.
         */
        boolean isNotMerged = arrayDesign.getMergees().isEmpty();

        /*
         * make sure the last repeat mask and sequence analysis were done after the last sequence update. This is a
         * check that is not done by the default 'needToRun' implementation. Note that Repeatmasking can be done in any
         * order w.r.t. the sequence analysis, so we don't need to check that order.
         */
        if ( isNotMerged && lastSequenceUpdate != null ) {
            // FIXME: repeatmasking is currently not operable so we are waiving this requirement.
            if ( lastRepeatMask != null && lastSequenceUpdate.getDate().after( lastRepeatMask.getDate() ) ) {
//                AbstractCLI.log
//                        .warn( arrayDesign + ": Sequences were updated more recently than the last repeat masking" );
//                return false;
                AbstractCLI.log
                        .warn( arrayDesign + ": Sequences were updated more recently than the last repeat masking but requirement temporarily waived" );
                // return false;
            }

            if ( lastSequenceAnalysis != null && lastSequenceUpdate.getDate()
                    .after( lastSequenceAnalysis.getDate() ) ) {
                AbstractCLI.log
                        .warn( arrayDesign + ": Sequences were updated more recently than the last sequence analysis" );
                return false;
            }
        }

        /*
         * This checks to make sure all the prerequsite steps have actually done. NOTE we don't check the sequence
         * update because this wasn't always filled in. Really we should.
         */

        if ( isNotMerged && lastSequenceAnalysis == null ) {
            AbstractCLI.log.warn( arrayDesign + ": Must do sequence analysis before probe mapping" );
            // We return false because we're not in a state to run it.
            return false;
        }

        if ( isNotMerged && lastRepeatMask == null ) {
            AbstractCLI.log
                    .warn( arrayDesign + "Repeat masking missing but requirement temporarily waived" );
            // AbstractCLI.log.warn( arrayDesign + ": Must do repeat mask analysis before probe mapping" );
            // return false;
        }

        if ( skipIfLastRunLaterThan != null && lastProbeMapping != null && lastProbeMapping.getDate()
                .after( skipIfLastRunLaterThan ) ) {
            AbstractCLI.log.info( arrayDesign + " was probemapped since " + skipIfLastRunLaterThan + ", skipping." );
            return false;
        }

        // we've validated the super.needToRun result, so we pass it on.
        return true;
    }

    @Override
    public String getCommandName() {
        return "mapPlatformToGenes";
    }

    @Override
    protected void doWork() throws Exception {
        final Date skipIfLastRunLaterThan = this.getLimitingDate();

        if ( this.taxon != null && this.directAnnotationInputFileName == null && this.getArrayDesignsToProcess()
                .isEmpty() ) {
            AbstractCLI.log.warn( "*** Running mapping for all " + taxon.getCommonName()
                    + " Array designs, troubled platforms may be skipped *** " );
        }

        /*
         * This is a separate method from batchRun...processArrayDesign because there are more possibilities.
         */
        if ( !this.getArrayDesignsToProcess().isEmpty() ) {
            for ( ArrayDesign arrayDesign : this.getArrayDesignsToProcess() ) {
                if ( this.probeNames != null ) {
                    // only one platform possible
                    if ( getArrayDesignsToProcess().size() > 1 )
                        throw new IllegalArgumentException( "Can only use probe names when processing a single platform" );
                    this.processProbes( arrayDesign );
                    break;
                }

                AbstractCLI.log.info( "====== Start processing: " + arrayDesign + " (" + arrayDesign.getShortName() + ") =====" );

                if ( !shouldRun( skipIfLastRunLaterThan, arrayDesign, ArrayDesignGeneMappingEvent.class ) ) {
                    continue;
                }

                arrayDesign = getArrayDesignService().thaw( arrayDesign );
                if ( directAnnotationInputFileName != null ) {
                    if ( this.getArrayDesignsToProcess().size() > 1 )
                        throw new IllegalArgumentException( "Can only use direct annotation from a file when processing a single platform" );
                    try {
                        File f = new File( this.directAnnotationInputFileName );
                        if ( !f.canRead() ) {
                            throw new IOException( "Cannot read from " + this.directAnnotationInputFileName );
                        }
                        arrayDesignProbeMapperService
                                .processArrayDesign( arrayDesign, taxon, f, this.sourceDatabase, this.ncbiIds );
                        this.audit( arrayDesign, "Imported from " + f, AnnotationBasedGeneMappingEvent.class );
                    } catch ( IOException e ) {
                        addErrorObject( arrayDesign, e );
                    }
                } else {

                    if ( !this.useDB ) {
                        AbstractCLI.log.info( "**** Writing to STDOUT instead of the database ***" );
                        System.out.print( config );
                    }

                    try {
                        this.configure( arrayDesign );
                        if ( !getRelatedDesigns( arrayDesign ).isEmpty() ) {
                            log.info( getRelatedDesigns( arrayDesign ).size() + " subsumed or merged platforms will be implicitly updated" );
                        }
                        arrayDesignProbeMapperService.processArrayDesign( arrayDesign, config, this.useDB );
                        if ( useDB ) {

                            if ( this.mirnaOnlyModeOption ) {
                                this.audit( arrayDesign, "Run in miRNA-only mode.", AlignmentBasedGeneMappingEvent.class );
                            } else if ( configOption != null ) {
                                this.audit( arrayDesign, "Run with configuration=" + this.configOption,
                                        AlignmentBasedGeneMappingEvent.class );
                            } else {
                                this.audit( arrayDesign, "Run with default parameters",
                                        AlignmentBasedGeneMappingEvent.class );
                            }
                            updateMergedOrSubsumed( arrayDesign );
                        }

                        addSuccessObject( arrayDesign );
                    } catch ( Exception e ) {
                        addErrorObject( arrayDesign, e );
                    }
                }

            }
        } else if ( taxon != null || skipIfLastRunLaterThan != null || isAutoSeek() ) {

            if ( directAnnotationInputFileName != null ) {
                throw new IllegalStateException(
                        "Sorry, you can't provide an input mapping file when doing multiple arrays at once" );
            }
            this.configure( null );
            this.batchRun( skipIfLastRunLaterThan );

        } else {
            throw new IllegalArgumentException( "Seems you did not set options to get anything to happen." );
        }
    }

    @Override
    public String getShortDesc() {
        return "Process the BLAT results for an array design to map them onto genes";
    }

    private void audit( ArrayDesign arrayDesign, String note, Class<? extends ArrayDesignGeneMappingEvent> eventType ) {
        getArrayDesignReportService().generateArrayDesignReport( arrayDesign.getId() );
        auditTrailService.addUpdateEvent( arrayDesign, eventType, note );
    }

    private void batchRun( final Date skipIfLastRunLaterThan ) throws InterruptedException {
        Collection<ArrayDesign> allArrayDesigns;

        if ( this.taxon != null ) {
            allArrayDesigns = getArrayDesignService().findByTaxon( this.taxon );
        } else {
            allArrayDesigns = getArrayDesignService().loadAll();
        }

        // TODO: process array designs in order of how many experiments they use (most first)

        Collection<Callable<Void>> arrayDesigns = new ArrayList<>( allArrayDesigns.size() );
        for ( ArrayDesign ad : allArrayDesigns ) {
            arrayDesigns.add( new ProcessADProbeMapper( ad, skipIfLastRunLaterThan ) );
        }
        executeBatchTasks( arrayDesigns );
    }

    private void configure( ArrayDesign arrayDesign ) {
        this.config = new ProbeMapperConfig();

        /*
         * Hackery to work around rn6+ problems
         */
        boolean isRat;
        if ( this.taxon == null ) {
            assert arrayDesign != null;
            Taxon t = getArrayDesignService().getTaxon( arrayDesign.getId() );
            isRat = t.getCommonName().equals( "rat" );
        } else {
            isRat = taxon.getCommonName().equals( "rat" );
        }

        boolean isMissingTracks = isRat && Settings
                .getString( "gemma.goldenpath.db.rat" ).startsWith( "rn" );

        if ( mirnaOnlyModeOption ) {
            AbstractCLI.log.info( "Micro RNA only mode" );
            config.setAllTracksOff();
            config.setUseMiRNA( true );
        } else if ( this.configOption != null ) {

            String configString = this.configOption;

            if ( !configString.matches(
                    "[" + ArrayDesignProbeMapperCli.OPTION_REFSEQ + ArrayDesignProbeMapperCli.OPTION_KNOWNGENE
                            + ArrayDesignProbeMapperCli.OPTION_MICRORNA + ArrayDesignProbeMapperCli.OPTION_EST
                            + ArrayDesignProbeMapperCli.OPTION_MRNA
                            + ArrayDesignProbeMapperCli.OPTION_ENSEMBL
                            + "]+" ) ) {
                throw new IllegalArgumentException(
                        "Configuration string must only contain values [" + ArrayDesignProbeMapperCli.OPTION_REFSEQ
                                + ArrayDesignProbeMapperCli.OPTION_KNOWNGENE + ArrayDesignProbeMapperCli.OPTION_MICRORNA
                                + ArrayDesignProbeMapperCli.OPTION_EST + ArrayDesignProbeMapperCli.OPTION_MRNA
                                + ArrayDesignProbeMapperCli.OPTION_ENSEMBL
                                + "]" );
            }

            config.setAllTracksOff();

            config.setUseEsts( configString.contains( ArrayDesignProbeMapperCli.OPTION_EST ) );
            config.setUseMrnas( configString.contains( ArrayDesignProbeMapperCli.OPTION_MRNA ) );
            config.setUseMiRNA( configString.contains( ArrayDesignProbeMapperCli.OPTION_MICRORNA ) );
            config.setUseEnsembl( configString.contains( ArrayDesignProbeMapperCli.OPTION_ENSEMBL ) );
            config.setUseRefGene( configString.contains( ArrayDesignProbeMapperCli.OPTION_REFSEQ ) );
            config.setUseKnownGene( configString.contains( ArrayDesignProbeMapperCli.OPTION_KNOWNGENE ) );
        }

        if ( blatScoreThreshold != null ) {
            config.setBlatScoreThreshold( blatScoreThreshold );
        }

        if ( this.usePred ) {
            config.setAllowPredictedGenes( true );
        }

        if ( identityThreshold != null ) {
            config.setIdentityThreshold( identityThreshold );
        }

        if ( overlapThreshold != null ) {
            config.setMinimumExonOverlapFraction( overlapThreshold );
        }

        if ( isMissingTracks && config.isUseKnownGene() ) {
            AbstractCLI.log.warn( "Genome does not have knowngene track, turning option off" );
            config.setUseKnownGene( false );
        }

        AbstractCLI.log.info( config );

    }

    private void processArrayDesign( Date skipIfLastRunLaterThan, ArrayDesign design ) {
        if ( taxon != null && !getArrayDesignService().getTaxa( design ).contains( taxon ) ) {
            return;
        }

        if ( !shouldRun( skipIfLastRunLaterThan, design, ArrayDesignGeneMappingEvent.class ) ) {
            return;
        }

        AbstractCLI.log.info( "============== Start processing: " + design + " ==================" );
        try {

            design = getArrayDesignService().thaw( design );
            this.configure( design );
            if ( !getRelatedDesigns( design ).isEmpty() ) {
                log.info( getRelatedDesigns( design ).size() + " subsumed or merged platforms will be implicitly updated" );
            }
            arrayDesignProbeMapperService.processArrayDesign( design, this.config, this.useDB );
            addSuccessObject( design );
            this.audit( design, "Part of a batch job", AlignmentBasedGeneMappingEvent.class );

            updateMergedOrSubsumed( design );

        } catch ( Exception e ) {
            addErrorObject( design, e );
        }
    }

    /**
     * When we analyze a platform that has mergees or subsumed platforms, we can treat them as if they were analyzed as
     * well. We simply add an audit event, and update the report for the platform.
     *
     * @param design platform
     */
    private void updateMergedOrSubsumed( ArrayDesign design ) {
        /*
         * Update merged or subsumed platforms.
         */

        Collection<ArrayDesign> toUpdate = getRelatedDesigns( design );
        for ( ArrayDesign ad : toUpdate ) {
            log.info( "Marking subsumed or merged design as completed, updating report: " + ad );
            this.audit( ad, "Parent design was processed (merged or subsumed by this)", AlignmentBasedGeneMappingEvent.class );
            arrayDesignProbeMapperService.deleteOldFiles( ad );
            getArrayDesignReportService().generateArrayDesignReport( ad.getId() );
        }
    }

    private void processProbes( ArrayDesign arrayDesign ) {
        assert this.probeNames != null && this.probeNames.length > 0;
        arrayDesign = getArrayDesignService().thawLite( arrayDesign );
        this.configure( arrayDesign );
        CompositeSequenceService compositeSequenceService = this.getBean( CompositeSequenceService.class );

        for ( String probeName : this.probeNames ) {
            CompositeSequence probe = compositeSequenceService.findByName( arrayDesign, probeName );

            if ( probe == null ) {
                AbstractCLI.log.warn( "No such probe: " + probeName + " on " + arrayDesign.getShortName() );
                continue;
            }

            probe = compositeSequenceService.thaw( probe );

            Map<String, Collection<BlatAssociation>> results = this.arrayDesignProbeMapperService
                    .processCompositeSequence( this.config, taxon, null, probe );

            for ( Collection<BlatAssociation> col : results.values() ) {
                for ( BlatAssociation association : col ) {
                    if ( AbstractCLI.log.isDebugEnabled() )
                        AbstractCLI.log.debug( association );
                }

                arrayDesignProbeMapperService.printResult( probe, col );

            }
        }
    }

    private class ProcessADProbeMapper implements Callable<Void> {

        private ArrayDesign arrayDesign;
        private Date skipIfLastRunLaterThan;

        private ProcessADProbeMapper( ArrayDesign arrayDesign, Date skipIfLastRunLaterThan ) {
            this.arrayDesign = arrayDesign;
            this.skipIfLastRunLaterThan = skipIfLastRunLaterThan;
        }

        @Override
        public Void call() {

            if ( arrayDesign.getCurationDetails().getTroubled() ) {
                AbstractCLI.log.warn( "Skipping troubled platform: " + arrayDesign );
                addErrorObject( arrayDesign, "Skipped because it is troubled; run in non-batch-mode" );
                return null;
            }

            /*
             * Note that if the array design has multiple taxa, analysis will be run on all of the sequences, not
             * just the ones from the taxon specified.
             */
            ArrayDesignProbeMapperCli.this.processArrayDesign( skipIfLastRunLaterThan, arrayDesign );

            return null;
        }
    }
}
