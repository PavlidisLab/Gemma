package ubic.gemma.apps;

import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;

import ubic.gemma.loader.expression.arrayDesign.ArrayDesignProbeMapperService;
import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.ArrayDesignAnalysisEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.ArrayDesignGeneMappingEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.ArrayDesignRepeatAnalysisEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.ArrayDesignSequenceAnalysisEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.ArrayDesignSequenceUpdateEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.AuditEventType;
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

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.util.AbstractCLI#buildOptions()
     */
    @SuppressWarnings("static-access")
    @Override
    protected void buildOptions() {
        super.buildOptions();

        Option taxonOption = OptionBuilder
                .hasArg()
                .withArgName( "taxon" )
                .withDescription(
                        "Taxon common name (e.g., human); analysis will be run for all ArrayDesigns from that taxon (overrides -a)" )
                .create( 't' );

        addOption( taxonOption );

        Option force = OptionBuilder.withDescription( "Run no matter what" ).create( "force" );

        addOption( force );

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
     * 
     * @see ubic.gemma.util.AbstractCLI#doWork(java.lang.String[])
     */
    @SuppressWarnings("unchecked")
    @Override
    protected Exception doWork( String[] args ) {
        Exception err = processCommandLine( "Array design mapping of probes to genes", args );
        if ( err != null ) return err;

        Date skipIfLastRunLaterThan = getLimitingDate();

        if ( this.taxon != null ) {
            log.warn( "*** Running mapping for all " + taxon.getCommonName() + " Array designs *** " );
        }

        if ( taxon != null || skipIfLastRunLaterThan != null || autoSeek ) {
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
                    arrayDesignService.thaw( design );
                    arrayDesignProbeMapperService.processArrayDesign( design );
                    successObjects.add( design.getName() );
                    audit( design, "Part of a batch job" );
                } catch ( Exception e ) {
                    errorObjects.add( design + ": " + e.getMessage() );
                    log.error( "**** Exception while processing " + design + ": " + e.getMessage() + " ****" );
                    log.error( e, e );
                }
            }
            summarizeProcessing();
        } else {
            // we've been given a specific array design; still use mdate to check.
            ArrayDesign arrayDesign = locateArrayDesign( arrayDesignName );

            if ( !needToRun( skipIfLastRunLaterThan, arrayDesign, ArrayDesignGeneMappingEvent.class ) ) {
                log.warn( arrayDesign + " not ready to run" );
                return null;
            }

            unlazifyArrayDesign( arrayDesign );
            arrayDesignProbeMapperService.processArrayDesign( arrayDesign );
            audit( arrayDesign, "Run with default parameters" );
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

        if ( !super.needToRun( skipIfLastRunLaterThan, arrayDesign, eventClass ) ) {
            return false;
        }

        log.debug( "Re-Checking status of " + arrayDesign );

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
                log.debug( "Last probe mapping analysis: " + lastProbeMapping.getDate() );
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

        // we've validated the super.needToRun result, so we pass it on.
        return true;
    }

    @Override
    protected void processOptions() {
        super.processOptions();
        arrayDesignProbeMapperService = ( ArrayDesignProbeMapperService ) this
                .getBean( "arrayDesignProbeMapperService" );
        this.taxonService = ( TaxonService ) this.getBean( "taxonService" );

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
    private void audit( ArrayDesign arrayDesign, String note ) {
        arrayDesignReportService.generateArrayDesignReport( arrayDesign.getId() );
        AuditEventType eventType = ArrayDesignGeneMappingEvent.Factory.newInstance();
        auditTrailService.addUpdateEvent( arrayDesign, eventType, note );
    }

}
