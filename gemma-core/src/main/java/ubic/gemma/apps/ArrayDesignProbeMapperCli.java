package ubic.gemma.apps;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;

import ubic.gemma.loader.expression.arrayDesign.ArrayDesignProbeMapperService;
import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.AlignmentBasedGeneMappingEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.AnnotationBasedGeneMappingEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.ArrayDesignAnalysisEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.ArrayDesignGeneMappingEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.ArrayDesignRepeatAnalysisEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.ArrayDesignSequenceAnalysisEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.ArrayDesignSequenceUpdateEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.AuditEventType;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.common.description.ExternalDatabaseService;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.TaxonService;

/**
 * Process the blat results for an array design to map them onto genes.
 * <p>
 * Typical workflow would be to run:
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
 * TODO : allow tuning of parameters from the command line.
 * 
 * @author pavlidis
 * @version $Id$
 */
public class ArrayDesignProbeMapperCli extends ArrayDesignSequenceManipulatingCli {
    ArrayDesignProbeMapperService arrayDesignProbeMapperService;
    private TaxonService taxonService;
    private String taxonName;
    private Taxon taxon;
    private String directAnnotationInputFileName = null;
    private ExternalDatabase sourceDatabase = null;

    /*
     * (non-Javadoc)
     * @see ubic.gemma.util.AbstractCLI#buildOptions()
     */
    @SuppressWarnings("static-access")
    @Override
    protected void buildOptions() {
        super.buildOptions();

        requireLogin();

        Option taxonOption = OptionBuilder
                .hasArg()
                .withArgName( "taxon" )
                .withDescription(
                        "Taxon common name (e.g., human); analysis will be run for all ArrayDesigns from that taxon (overrides -a)" )
                .create( 't' );

        addOption( taxonOption );

        Option force = OptionBuilder.withDescription( "Run no matter what" ).create( "force" );

        addOption( force );

        Option directAnnotation = OptionBuilder.withDescription(
                "Import annotations from a file rather than our own analysis. You must provide the taxon option" )
                .hasArg().withArgName( "file" ).create( "import" );

        addOption( directAnnotation );

        Option databaseOption = OptionBuilder.withDescription(
                "Source database name (GEO etc); required if using -import" ).hasArg().withArgName( "dbname" ).create(
                "source" );
        addOption( databaseOption );
    }

    public static void main( String[] args ) {
        ArrayDesignProbeMapperCli p = new ArrayDesignProbeMapperCli();
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
     * @see ubic.gemma.util.AbstractCLI#doWork(java.lang.String[])
     */
    @Override
    protected Exception doWork( String[] args ) {
        Exception err = processCommandLine( "Array design mapping of probes to genes", args );
        if ( err != null ) return err;

        Date skipIfLastRunLaterThan = getLimitingDate();

        if ( this.taxon != null && this.directAnnotationInputFileName == null ) {
            log.warn( "*** Running mapping for all " + taxon.getCommonName() + " Array designs *** " );
        }

        if ( arrayDesignName != null ) {
            // we've been given a specific array design; still use mdate to check.
            ArrayDesign arrayDesign = locateArrayDesign( arrayDesignName );

            if ( !needToRun( skipIfLastRunLaterThan, arrayDesign, ArrayDesignGeneMappingEvent.class ) ) {
                log.warn( arrayDesign + " not ready to run" );
                return null;
            }

            unlazifyArrayDesign( arrayDesign );
            if ( directAnnotationInputFileName != null ) {
                try {
                    File f = new File( this.directAnnotationInputFileName );
                    if ( !f.canRead() ) {
                        throw new IOException( "Cannot read from " + this.directAnnotationInputFileName );
                    }
                    arrayDesignProbeMapperService.processArrayDesign( arrayDesign, taxon, f, this.sourceDatabase );
                    audit( arrayDesign, "Imported from " + f, AnnotationBasedGeneMappingEvent.Factory.newInstance() );
                } catch ( IOException e ) {
                    return e;
                }
            } else {
                arrayDesignProbeMapperService.processArrayDesign( arrayDesign );
                audit( arrayDesign, "Run with default parameters", AlignmentBasedGeneMappingEvent.Factory.newInstance() );
            }

        } else if ( taxon != null || skipIfLastRunLaterThan != null || autoSeek ) {

            if ( directAnnotationInputFileName != null ) {
                throw new IllegalStateException(
                        "Sorry, you can't provide an input mapping file when doing multiple arrays at once" );
            }

            // look at all array designs.
            Collection<ArrayDesign> allArrayDesigns = arrayDesignService.loadAll();
            for ( ArrayDesign design : allArrayDesigns ) {

                if ( taxon != null && !taxon.equals( arrayDesignService.getTaxon( design.getId() ) ) ) {
                    continue;
                }

                if ( isSubsumedOrMerged( design ) ) {
                    log.warn( design + " is subsumed or merged into another design, it will not be run." );
                    // not really an error, but nice to get notification.
                    errorObjects.add( design + ": "
                            + "Skipped because it is subsumed by or merged into another design." );
                    continue;
                }

                if ( !needToRun( skipIfLastRunLaterThan, design, ArrayDesignGeneMappingEvent.class ) ) {
                    if ( skipIfLastRunLaterThan != null ) {
                        log.warn( design + " was last run more recently than " + skipIfLastRunLaterThan );
                        errorObjects.add( design + ": " + "Skipped because it was last run after "
                                + skipIfLastRunLaterThan );
                    } else {
                        log.warn( design + " seems to be up to date or is not ready to run" );
                        errorObjects.add( design + " seems to be up to date or is not ready to run" );
                    }
                    continue;
                }

                log.info( "============== Start processing: " + design + " ==================" );
                try {
                    arrayDesignService.thawLite( design );

                    arrayDesignProbeMapperService.processArrayDesign( design );
                    successObjects.add( design.getName() );
                    ArrayDesignGeneMappingEvent eventType = AlignmentBasedGeneMappingEvent.Factory.newInstance();
                    audit( design, "Part of a batch job", eventType );

                } catch ( Exception e ) {
                    errorObjects.add( design + ": " + e.getMessage() );
                    log.error( "**** Exception while processing " + design + ": " + e.getMessage() + " ****" );
                    log.error( e, e );
                }
            }
            summarizeProcessing();
        } else {
            return new IllegalArgumentException( "Seems you did not set options to get anything to happen." );
        }

        return null;
    }

    /*
     * Override to do additional checks to make sure the array design is in a state of readiness for probe mapping.
     */
    @Override
    protected boolean needToRun( Date skipIfLastRunLaterThan, ArrayDesign arrayDesign,
            Class<? extends ArrayDesignAnalysisEvent> eventClass ) {

        if ( this.hasOption( "force" ) ) {
            return true;
        }

        if ( this.directAnnotationInputFileName != null ) {
            return true;
        }

        if ( !super.needToRun( skipIfLastRunLaterThan, arrayDesign, eventClass ) ) {
            return false;
        }

        log.debug( "Re-Checking status of " + arrayDesign );

        this.auditTrailService.thaw( arrayDesign );
        List<AuditEvent> allEvents = ( List<AuditEvent> ) arrayDesign.getAuditTrail().getEvents();
        AuditEvent lastSequenceAnalysis = null;
        AuditEvent lastRepeatMask = null;
        AuditEvent lastSequenceUpdate = null;
        AuditEvent lastProbeMapping = null;

        log.debug( allEvents.size() + " to inspect" );
        for ( int j = allEvents.size() - 1; j >= 0; j-- ) {
            AuditEvent currentEvent = allEvents.get( j );

            if ( currentEvent.getEventType() == null ) continue;

            // we only care about ArrayDesignAnalysisEvent events.
            Class<? extends AuditEventType> currentEventClass = currentEvent.getEventType().getClass();

            log.debug( "Inspecting: " + currentEventClass );

            // Get the most recent event of each type.
            if ( lastRepeatMask == null && ArrayDesignRepeatAnalysisEvent.class.isAssignableFrom( currentEventClass ) ) {
                lastRepeatMask = currentEvent;
                log.debug( "Last repeat mask: " + lastRepeatMask.getDate() );
            } else if ( lastSequenceUpdate == null
                    && ArrayDesignSequenceUpdateEvent.class.isAssignableFrom( currentEventClass ) ) {
                lastSequenceUpdate = currentEvent;
                log.debug( "Last sequence update: " + lastSequenceUpdate.getDate() );
            } else if ( lastSequenceAnalysis == null
                    && ArrayDesignSequenceAnalysisEvent.class.isAssignableFrom( currentEventClass ) ) {
                lastSequenceAnalysis = currentEvent;
                log.debug( "Last sequence analysis: " + lastSequenceAnalysis.getDate() );
            } else if ( lastProbeMapping == null
                    && ArrayDesignGeneMappingEvent.class.isAssignableFrom( currentEventClass ) ) {
                lastProbeMapping = currentEvent;
                log.info( "Last probe mapping analysis: " + lastProbeMapping.getDate() );
            }

        }

        /*
         * make sure the last repeat mask and sequence analysis were done after the last sequence update. This is a
         * check that is not done by the default 'needToRun' implementation. Note that Repeatmasking can be done in any
         * order w.r.t. the sequence analysis, so we don't need to check that order.
         */
        if ( lastSequenceUpdate != null ) {
            if ( lastRepeatMask != null ) {
                if ( lastSequenceUpdate.getDate().after( lastRepeatMask.getDate() ) ) {
                    log.warn( arrayDesign + ": Sequences were updated more recently than the last repeat masking" );
                    return false;
                }
            }

            if ( lastSequenceAnalysis != null ) {
                if ( lastSequenceUpdate.getDate().after( lastSequenceAnalysis.getDate() ) ) {
                    if ( lastSequenceUpdate.getDate().after( lastSequenceAnalysis.getDate() ) ) {
                        log.warn( arrayDesign
                                + ": Sequences were updated more recently than the last sequence analysis" );
                    }
                    return false;
                }
            }
        }

        /*
         * This checks to make sure all the prerequsite steps have actually done. NOTE we don't check the sequence
         * update because this wasn't always filled in. Really we should.
         */
        if ( lastSequenceAnalysis == null ) {
            log.warn( arrayDesign + ": Must do sequence analysis before probe mapping" );
            // We return false because we're not in a state to run it.
            return false;
        }

        if ( lastRepeatMask == null ) {
            log.warn( arrayDesign + ": Must do repeat mask analysis before probe mapping" );
            return false;
        }

        if ( skipIfLastRunLaterThan != null && lastProbeMapping != null
                && lastProbeMapping.getDate().after( skipIfLastRunLaterThan ) ) {
            log.info( arrayDesign + " was probemapped since " + skipIfLastRunLaterThan + ", skipping." );
            return false;
        }

        // we've validated the super.needToRun result, so we pass it on.
        return true;
    }

    @Override
    protected void processOptions() {
        super.processOptions();
        arrayDesignProbeMapperService = ( ArrayDesignProbeMapperService ) this
                .getBean( "arrayDesignProbeMapperService" );
        this.taxonService = ( TaxonService ) this.getBean( "taxonService" );

        if ( this.hasOption( "import" ) ) {
            if ( !this.hasOption( 't' ) ) {
                throw new IllegalArgumentException( "You must provide the taxon when using the import option" );
            }
            if ( !this.hasOption( "source" ) ) {
                throw new IllegalArgumentException(
                        "You must provide source database name when using the import option" );
            }
            String sourceDBName = this.getOptionValue( "source" );

            ExternalDatabaseService eds = ( ExternalDatabaseService ) this.getBean( "externalDatabaseService" );

            this.sourceDatabase = eds.find( sourceDBName );

            this.directAnnotationInputFileName = this.getOptionValue( "import" );
        }
        if ( this.hasOption( 't' ) ) {
            this.taxonName = this.getOptionValue( 't' );
            this.taxon = taxonService.findByCommonName( this.taxonName );
            if ( taxon == null ) {
                throw new IllegalArgumentException( "No taxon named " + taxonName );
            }
        }

    }

    /**
     * @param arrayDesign
     */
    private void audit( ArrayDesign arrayDesign, String note, ArrayDesignGeneMappingEvent eventType ) {
        arrayDesignReportService.generateArrayDesignReport( arrayDesign.getId() );
        auditTrailService.addUpdateEvent( arrayDesign, eventType, note );
    }

    @Override
    public String getShortDesc() {
        return "Process the BLAT results for an array design to map them onto genes";
    }

}
