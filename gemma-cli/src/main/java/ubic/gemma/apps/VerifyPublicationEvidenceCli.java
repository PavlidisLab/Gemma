/*
 * The Gemma project
 *
 * Copyright (c) 2026 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 */
package ubic.gemma.apps;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import ubic.gemma.model.association.GOEvidenceCode;
import ubic.gemma.model.common.description.BibliographicReference;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.common.description.ExternalDatabases;
import ubic.gemma.model.common.description.PublicationAssociation;
import ubic.gemma.model.common.description.PublicationAssociationRole;
import ubic.gemma.model.common.description.PublicationAssociationSource;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.core.loader.entrez.pubmed.ExpressionExperimentBibRefFinder;
import ubic.gemma.persistence.service.common.description.BibliographicReferenceService;
import ubic.gemma.persistence.service.common.description.PublicationAssertion;
import ubic.gemma.persistence.service.common.description.PublicationAssociationConflictException;
import ubic.gemma.persistence.service.common.description.PublicationAssociationService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;

import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Check Gemma's GEO-sourced primary publications against what GEO says today, and fill in the ones
 * GEO has a paper for and Gemma does not.
 *
 * <p>V25 asserted a basis for every publication link that existed when it ran, and for a
 * GEO-accessioned dataset that basis is an <b>inference</b>: the GEO importer is the only writer that
 * sets a primary without a human in the loop, so a primary on a GEO dataset is <i>taken to be</i>
 * GEO's {@code !Series_pubmed_id}. That is why those 23,066 rows carry {@code IIA} — inferred from
 * imported annotation — and not {@code TAS}. This command is what turns the inference into a check.</p>
 *
 * <h2>Why acc.cgi and not a batched esummary</h2>
 *
 * <p>🛑 <b>An agreement with {@code esummary db=gds} would not be evidence of anything, and it is the
 * disagreements it hides that matter.</b> The gds index lags the live GEO record
 * ({@link ExpressionExperimentBibRefFinder#locatePubMedId} documents this, and chose {@code acc.cgi}
 * for the same reason). Follow a re-pointed series through:</p>
 *
 * <pre>
 * GEO moves GSE123 from paper A to paper B
 * Gemma still holds A          (imported before the change)
 * esummary still reports A     (lagging)
 *   =&gt; A == A, "verified", stamped TAS -- on the one link that has actually drifted
 * </pre>
 *
 * <p>A drifted link is exactly the shape of a false agreement, so re-checking only the disagreements
 * would never revisit it. Lag makes false MISMATCHES, which are harmless and self-correcting, and
 * false MATCHES, which are not. So every series is read from {@code acc.cgi} — one request each,
 * paced, and the run is resumable because at that rate it is measured in hours.</p>
 *
 * <h2>What it will and will not write</h2>
 *
 * <ul>
 * <li><b>Agreement</b> — the assertion is re-stated with {@code TAS} and evidence saying it was
 * checked against GEO on the day it ran. The link itself is not touched; only its basis.</li>
 * <li><b>Disagreement</b> — <b>nothing is written.</b> It splits two ways that no automated rule can
 * separate: a curator corrected GEO, or GEO is wrong (GSE227854, where the submitter cross-linked
 * the wrong one of their own two papers). Both land in the change log for a person to read.</li>
 * <li><b>Gemma has no primary and GEO has one</b> — added, under {@code --fill}. This is the case
 * that creates a link rather than describing one.</li>
 * <li><b>GEO lists more than one paper</b> — first is the primary, the rest are other-relevant.
 * {@code parseSeriesPubMedIds} has always read them; nothing could act on them before.</li>
 * </ul>
 *
 * <p>A curator's assertion outranks GEO, so {@code apply()} in
 * {@link PublicationAssociationService} silently declines a promotion over one. Silently is not good
 * enough for a run whose output is a record of what changed, so the held assertion is read first and
 * a curator-held row is reported as skipped rather than attempted.</p>
 *
 * @author claude
 */
public class VerifyPublicationEvidenceCli extends ExpressionExperimentManipulatingCLI {

    private static final String FILL_OPTION = "fill";
    private static final String VERIFY_OPTION = "verify";
    private static final String CHANGE_LOG_OPTION = "changeLog";
    private static final String PACE_OPTION = "paceMillis";

    /**
     * One request every 350 ms, ~3/s — NCBI's published ceiling without an API key is 3/s and this is
     * a plain web fetch of {@code acc.cgi} rather than a eutils call, so the key does not raise it.
     */
    private static final long DEFAULT_PACE_MILLIS = 350L;

    @Autowired
    private ExpressionExperimentService eeService;
    @Autowired
    private BibliographicReferenceService bibliographicReferenceService;
    @Autowired
    private PublicationAssociationService publicationAssociationService;

    @Value("${entrez.efetch.apikey}")
    private String ncbiApiKey;

    private ExpressionExperimentBibRefFinder finder;

    /**
     * Package-private so a test can supply a stub. Every branch below turns on what GEO answered, and
     * a test that reached the real {@code acc.cgi} to find out would be both slow and a claim about
     * NCBI's uptime rather than about this class.
     */
    void setFinder( ExpressionExperimentBibRefFinder finder ) {
        this.finder = finder;
    }

    private boolean fill;
    private boolean verify;
    private long paceMillis = DEFAULT_PACE_MILLIS;
    @Nullable
    private File changeLogFile;
    @Nullable
    private PrintWriter changeLog;

    /** Accessions already recorded by an earlier run of the same change log; skipped on resume. */
    private final Set<String> alreadyDone = new HashSet<>();

    @Override
    public void afterPropertiesSet() throws Exception {
        super.afterPropertiesSet();
        this.finder = new ExpressionExperimentBibRefFinder( ncbiApiKey );
    }

    @Nullable
    @Override
    public String getCommandName() {
        return "verifyPublicationEvidence";
    }

    @Nullable
    @Override
    public String getShortDesc() {
        return "Check GEO-sourced primary publications against GEO, and fill in missing ones";
    }

    @Override
    protected void buildExperimentOptions( Options options ) {
        options.addOption( null, VERIFY_OPTION, false,
                "Check existing GEO-sourced primaries against GEO and promote the agreements from IIA to"
                        + " TAS. Disagreements are reported and never written." );
        options.addOption( null, FILL_OPTION, false,
                "Add a primary publication where Gemma has none and GEO does, and add GEO's second and"
                        + " later papers as other-relevant. This one creates links; Gemma 1.32.x reads the"
                        + " same columns and will show them immediately." );
        options.addOption( null, CHANGE_LOG_OPTION, true,
                "TSV of every decision, one row per dataset, written as the run goes. Re-running with the"
                        + " same file resumes: accessions already in it are not re-fetched." );
        options.addOption( null, PACE_OPTION, true,
                "Milliseconds to wait between GEO requests. Default " + DEFAULT_PACE_MILLIS + "." );
    }

    @Override
    protected void processExperimentOptions( CommandLine commandLine ) throws ParseException {
        this.verify = commandLine.hasOption( VERIFY_OPTION );
        this.fill = commandLine.hasOption( FILL_OPTION );
        if ( !verify && !fill ) {
            throw new ParseException( "Nothing to do: pass --" + VERIFY_OPTION + ", --" + FILL_OPTION
                    + ", or both. There is no default, because one of them only reads GEO and restates a"
                    + " basis while the other creates publication links a live Gemma 1.32.x will display." );
        }
        if ( commandLine.hasOption( PACE_OPTION ) ) {
            this.paceMillis = Long.parseLong( commandLine.getOptionValue( PACE_OPTION ) );
        }
        if ( commandLine.hasOption( CHANGE_LOG_OPTION ) ) {
            this.changeLogFile = new File( commandLine.getOptionValue( CHANGE_LOG_OPTION ) );
            // 🛑 Checked HERE, not where the file is opened. Opening happens after the experiment
            // list is built, and building it loads the whole corpus -- so an unwritable path used to
            // cost a full scan before failing, and then failed as a bare NoSuchFileException that
            // named the file rather than the directory that was actually missing. An argument that
            // cannot work should be refused while it is still an argument.
            File parent = changeLogFile.getAbsoluteFile().getParentFile();
            if ( parent != null && !parent.isDirectory() ) {
                throw new ParseException( "Cannot write the change log to " + changeLogFile
                        + ": the directory " + parent + " does not exist. Note this path is resolved"
                        + " where the CLI RUNS, which for the published image is inside the container"
                        + " -- $HOME is mounted through, most other paths are not." );
            }
            if ( parent != null && !parent.canWrite() ) {
                throw new ParseException( "Cannot write the change log to " + changeLogFile
                        + ": " + parent + " is not writable by this user." );
            }
        }
    }

    @Override
    protected void processExpressionExperiments( Collection<ExpressionExperiment> ees ) {
        try {
            openChangeLog();
        } catch ( IOException e ) {
            throw new RuntimeException( "Could not open the change log at " + changeLogFile
                    + "; refusing to run a job whose record of what it changed would be lost.", e );
        }
        try {
            super.processExpressionExperiments( ees );
        } finally {
            if ( changeLog != null ) {
                changeLog.flush();
                changeLog.close();
            }
        }
    }

    @Override
    protected void processExpressionExperiment( ExpressionExperiment ee ) throws Exception {
        ee = eeService.thawLite( ee );

        String accession = geoSeriesAccession( ee );
        if ( accession == null ) {
            record( ee, null, "skipped_not_geo_series", null, null, null, "no GEO series accession" );
            return;
        }
        if ( alreadyDone.contains( accession ) ) {
            log.debug( accession + " is already in the change log; skipping." );
            return;
        }

        BibliographicReference primary = ee.getPrimaryPublication();

        if ( primary == null ) {
            // ~2,599 datasets, and GEO is the only thing that can answer for them
            handleMissingPrimary( ee, accession, fill ? fetchFromGeo( accession ) : NOT_FETCHED );
            return;
        }
        if ( !verify ) {
            return;
        }
        // 🛑 Ask the database before asking GEO. Every check below is local and can settle the row on
        // its own -- no assertion, someone who outranks GEO, or an already-verified TAS -- and asking
        // GEO first meant a round trip per dataset whose answer was then thrown away. That is ~23,000
        // needless fetches on any re-run, on a job already measured in hours, against a public NCBI
        // endpoint. GEO is consulted only where its answer can still change something.
        String gemmaPmid = pubMedIdOf( primary );
        PublicationAssociation held = publicationAssociationService.find( ee, primary );
        if ( held == null ) {
            record( ee, accession, "no_assertion_on_record", gemmaPmid, null, null,
                    "the link has no PUBLICATION_ASSOCIATION row; V25 should have created one" );
            return;
        }
        if ( held.getSource() != PublicationAssociationSource.GEO_SUBMITTER_LINK ) {
            record( ee, accession, "skipped_not_geo_asserted", gemmaPmid, null,
                    String.valueOf( held.getEvidenceCode() ),
                    "asserted by " + held.getSource() + "; GEO does not outrank it" );
            return;
        }
        if ( held.getEvidenceCode() == GOEvidenceCode.TAS ) {
            record( ee, accession, "already_verified", gemmaPmid, null, "TAS", null );
            return;
        }
        handleExistingPrimary( ee, accession, fetchFromGeo( accession ), primary, held, gemmaPmid );
    }

    /** Marks "GEO was deliberately not asked", which is different from "GEO said nothing". */
    private static final List<Integer> NOT_FETCHED = Collections.emptyList();

    /**
     * Gemma has no primary. This is the only branch that creates a link, and the ~2,599 datasets in
     * this state are the whole exposure set for the next GEO refresh, which sets a primary precisely
     * when there is none.
     */
    private void handleMissingPrimary( ExpressionExperiment ee, String accession, @Nullable List<Integer> geoIds ) {
        if ( !fill ) {
            // GEO is not consulted in a verify-only run: it cannot change anything here, and asking
            // would cost a request per dataset to write a note nobody can act on until --fill runs.
            record( ee, accession, "no_primary", null, null, null,
                    "Gemma has no primary; run with --" + FILL_OPTION + " to ask GEO for one" );
            return;
        }
        if ( geoIds == null ) {
            // unreadable, not empty: say nothing about what GEO lists, and fill nothing in
            record( ee, accession, "geo_unreadable", null, null, null,
                    "could not read " + accession + " from GEO; no conclusion drawn" );
            addWarningObject( ee, "Could not read " + accession + " from GEO." );
            return;
        }
        if ( geoIds.isEmpty() ) {
            record( ee, accession, "no_primary_geo_states_none", null, null, null,
                    "neither Gemma nor GEO has a publication for this series" );
            return;
        }
        BibliographicReference primaryRef = bibliographicReferenceService
                .findOrCreateByPubMedId( String.valueOf( geoIds.get( 0 ) ) );
        if ( primaryRef == null ) {
            record( ee, accession, "fill_failed_pubmed_unresolved", null, join( geoIds ), null,
                    "PubMed did not resolve " + geoIds.get( 0 ) );
            addErrorObject( ee, "Could not resolve PubMed id " + geoIds.get( 0 ) + " for " + accession );
            return;
        }
        List<PublicationAssertion> other = new ArrayList<>();
        for ( int i = 1; i < geoIds.size(); i++ ) {
            BibliographicReference ref = bibliographicReferenceService
                    .findOrCreateByPubMedId( String.valueOf( geoIds.get( i ) ) );
            if ( ref != null ) {
                other.add( new PublicationAssertion( ref, PublicationAssociationSource.GEO_SUBMITTER_LINK,
                        geoEvidence( accession, geoIds.get( i ), false ), null, GOEvidenceCode.TAS, null, null ) );
            }
        }
        try {
            // Rejections passed as null -- untouched. This command reads GEO and fills a gap; it has no
            // opinion about which papers a curator has ruled out, and clearing them here would delete
            // the very rejections the catch below depends on to refuse a bad fill on the next run.
            eeService.updatePublications( ee,
                    new PublicationAssertion( primaryRef, PublicationAssociationSource.GEO_SUBMITTER_LINK,
                            geoEvidence( accession, geoIds.get( 0 ), true ), null, GOEvidenceCode.TAS, null, null ),
                    other, null );
        } catch ( PublicationAssociationConflictException e ) {
            // ✅ The refusal the design exists to produce, and the reason this command can be pointed
            // at the whole corpus. GSE227854 is the case: it has no primary, GEO's !Series_pubmed_id
            // is the wrong one of the submitter's own two NAR papers, and --fill would otherwise
            // write that error in. A standing curator rejection outranks GEO_SUBMITTER_LINK and stops
            // it here rather than leaving it to be tidied up afterwards.
            record( ee, accession, "fill_refused_by_standing_rejection", null, join( geoIds ), null,
                    e.getMessage() );
            addWarningObject( ee, "GEO's publication for " + accession + " stands rejected; not added." );
            return;
        }
        record( ee, accession, other.isEmpty() ? "filled_primary" : "filled_primary_and_other",
                null, join( geoIds ), "TAS",
                "added primary " + geoIds.get( 0 )
                        + ( other.isEmpty() ? "" : " and " + other.size() + " other-relevant" ) );
        addSuccessObject( ee, "Added primary publication " + geoIds.get( 0 ) + " from GEO." );
    }

    /**
     * Gemma has a primary. Nothing here ever changes the link — only whether Gemma can say it checked.
     */
    private void handleExistingPrimary( ExpressionExperiment ee, String accession, @Nullable List<Integer> geoIds,
            BibliographicReference primary, PublicationAssociation held, @Nullable String gemmaPmid ) {
        if ( geoIds == null ) {
            // unreadable, not empty: the held evidence code stands, because nothing was verified
            record( ee, accession, "geo_unreadable", gemmaPmid, null, String.valueOf( held.getEvidenceCode() ),
                    "could not read " + accession + " from GEO; existing evidence left as-is" );
            addWarningObject( ee, "Could not read " + accession + " from GEO." );
            return;
        }
        if ( geoIds.isEmpty() ) {
            record( ee, accession, "geo_states_none", gemmaPmid, null, String.valueOf( held.getEvidenceCode() ),
                    "Gemma has a primary GEO no longer lists; not touched" );
            return;
        }
        // 🛑 Membership, not position. A series may list several papers -- GSE934 lists 15802019 and
        // 15867358, two 2005 papers from the same lab -- and Gemma holds the second. GEO does link
        // that paper to that series, so it is verified; what differs is which one is called primary,
        // and first-is-primary is GeoConverterImpl's convention rather than anything GEO asserts.
        // Comparing against geoIds.get(0) alone reported those as disagreements and would have filled
        // the review list with non-issues, burying the real ones. Found on the first live run.
        int position = gemmaPmid == null ? -1 : geoIds.indexOf( toInt( gemmaPmid ) );
        if ( position < 0 ) {
            // GEO does not link this paper to this series at all: the genuinely interesting set, and
            // deliberately not actionable by this command.
            record( ee, accession, "mismatch", gemmaPmid, join( geoIds ),
                    String.valueOf( held.getEvidenceCode() ),
                    "Gemma says " + gemmaPmid + ", GEO lists " + join( geoIds ) + "; needs a person" );
            addWarningObject( ee, "Primary publication is not among GEO's: Gemma " + gemmaPmid
                    + " vs GEO " + join( geoIds ) );
            return;
        }
        if ( position > 0 ) {
            // 🛑 In GEO's list but not first: reported, NOT promoted. TAS has to mean Gemma and GEO
            // agree on the primary, or it means nothing -- and this is precisely where the errors
            // live. GSE227854 is the worked case: the submitter cross-linked the wrong one of their
            // own two NAR papers, so GEO's own list contains a paper that is wrong for the dataset.
            // Comparing Gemma to GEO cannot detect that, which is exactly why a partial agreement
            // must stay visible instead of being certified and closed. Not a mismatch either -- GEO
            // does list this paper -- so it gets its own outcome and a curator decides which of the
            // two is primary.
            record( ee, accession, "in_geo_list_but_not_first", gemmaPmid, join( geoIds ),
                    String.valueOf( held.getEvidenceCode() ),
                    "GEO lists " + geoIds.size() + " papers and Gemma holds #" + ( position + 1 )
                            + "; left as " + held.getEvidenceCode() + " for a curator" );
            addWarningObject( ee, "Primary is in GEO's list but is not GEO's first: Gemma "
                    + gemmaPmid + " vs GEO " + join( geoIds ) );
            return;
        }
        publicationAssociationService.assertAccepted( ee,
                new PublicationAssertion( primary, PublicationAssociationSource.GEO_SUBMITTER_LINK,
                        geoEvidence( accession, geoIds.get( 0 ), true ), null,
                        GOEvidenceCode.TAS, null, null ),
                PublicationAssociationRole.PRIMARY );
        record( ee, accession, "promoted_iia_to_tas", gemmaPmid, join( geoIds ), "TAS",
                geoIds.size() > 1 ? "GEO also lists " + ( geoIds.size() - 1 ) + " further paper(s)" : null );
        addSuccessObject( ee, "Verified against GEO; IIA -> TAS." );
    }

    /**
     * @return the ids GEO lists, empty when GEO states there are none, or {@code null} when GEO could
     * not be read at all
     * <p>
     * The null is the point. This used to answer an unreadable GEO with an empty list, which every
     * caller below reads as "GEO states no publication" — so a GEO that was down, throttled, or (as
     * on 2026-09-02) serving a reCAPTCHA challenge produced a run that recorded
     * {@code geo_states_none} against the whole corpus, each with a dated {@link #geoEvidence} line
     * asserting the comparison had been made. Findings are quotable; a finding built on a failed
     * fetch is worse than no finding.
     */
    @Nullable
    private List<Integer> fetchFromGeo( String accession ) {
        try {
            if ( paceMillis > 0 ) {
                Thread.sleep( paceMillis );
            }
            return finder.locatePubMedIds( accession );
        } catch ( InterruptedException e ) {
            Thread.currentThread().interrupt();
            throw new RuntimeException( "Interrupted while pacing GEO requests.", e );
        } catch ( IOException e ) {
            log.warn( "Could not read " + accession + " from GEO: " + e.getMessage() );
            return null;
        }
    }

    /**
     * The one-line quotable basis. It names the day, because "checked against GEO" without a date is a
     * claim that quietly expires — GEO re-points series, and this is the only record of when the
     * comparison was true.
     */
    private String geoEvidence( String accession, int pubMedId, boolean isPrimary ) {
        String day = new SimpleDateFormat( "yyyy-MM-dd" ).format( new Date() );
        return "Checked against GEO on " + day + ": " + accession + " lists !Series_pubmed_id "
                + pubMedId + ( isPrimary ? " as its first (primary) publication." : " as a further publication." );
    }

    @Nullable
    private String geoSeriesAccession( ExpressionExperiment ee ) {
        DatabaseEntry acc = ee.getAccession();
        if ( acc == null || acc.getExternalDatabase() == null
                || !ExternalDatabases.GEO.equals( acc.getExternalDatabase().getName() ) ) {
            return null;
        }
        String value = acc.getAccession();
        return value != null && value.matches( "GSE\\d+" ) ? value : null;
    }

    @Nullable
    private String pubMedIdOf( BibliographicReference ref ) {
        return ref.getPubAccession() != null ? ref.getPubAccession().getAccession() : null;
    }

    @Nullable
    private Integer toInt( String s ) {
        try {
            return Integer.valueOf( s.trim() );
        } catch ( NumberFormatException e ) {
            // a primary whose accession is not a PubMed id cannot be found in GEO's list of them
            return null;
        }
    }

    private String join( @Nullable List<Integer> ids ) {
        return ids == null || ids.isEmpty() ? "" : StringUtils.join( ids, "," );
    }

    // ------------------------------------------------------------------
    // The change log. Every dataset the run looks at produces exactly one row, including the ones
    // nothing happened to -- "considered and left alone" is a different statement from "not reached",
    // and only one of them means the run can be resumed from here.
    // ------------------------------------------------------------------

    private void openChangeLog() throws IOException {
        if ( changeLogFile == null ) {
            return;
        }
        boolean resuming = changeLogFile.exists() && changeLogFile.length() > 0;
        if ( resuming ) {
            try ( BufferedReader r = new BufferedReader( new FileReader( changeLogFile ) ) ) {
                String line;
                while ( ( line = r.readLine() ) != null ) {
                    if ( line.startsWith( "#" ) || line.startsWith( "ee_id\t" ) ) {
                        continue;
                    }
                    String[] f = line.split( "\t", -1 );
                    if ( f.length > 2 && StringUtils.isNotBlank( f[2] ) ) {
                        alreadyDone.add( f[2] );
                    }
                }
            }
            log.info( "Resuming: " + alreadyDone.size() + " accessions already in " + changeLogFile );
        }
        changeLog = new PrintWriter( Files.newBufferedWriter( Paths.get( changeLogFile.getPath() ),
                StandardCharsets.UTF_8,
                resuming ? StandardOpenOption.APPEND : StandardOpenOption.CREATE,
                resuming ? StandardOpenOption.WRITE : StandardOpenOption.TRUNCATE_EXISTING ) );
        if ( !resuming ) {
            changeLog.println( "# command: verifyPublicationEvidence" );
            changeLog.println( "# source: GEO acc.cgi &targ=self&form=text !Series_pubmed_id" );
            changeLog.println( "# started: " + new Date() );
            changeLog.println( "# verify=" + verify + " fill=" + fill + " paceMillis=" + paceMillis );
            changeLog.println( "ee_id\tshort_name\tgeo_accession\toutcome\tgemma_pubmed_id"
                    + "\tgeo_pubmed_ids\tevidence_code\tnote" );
        }
    }

    private void record( ExpressionExperiment ee, @Nullable String accession, String outcome,
            @Nullable String gemmaPmid, @Nullable String geoPmids, @Nullable String evidenceCode,
            @Nullable String note ) {
        if ( changeLog == null ) {
            log.info( outcome + "\t" + ( accession != null ? accession : ee.getShortName() )
                    + ( note != null ? "\t" + note : "" ) );
            return;
        }
        changeLog.println( ee.getId() + "\t" + StringUtils.defaultString( ee.getShortName() ) + "\t"
                + StringUtils.defaultString( accession ) + "\t" + outcome + "\t"
                + StringUtils.defaultString( gemmaPmid ) + "\t" + StringUtils.defaultString( geoPmids ) + "\t"
                + StringUtils.defaultString( evidenceCode ) + "\t" + StringUtils.defaultString( note ) );
        // flushed per row on purpose: this file is what makes the run resumable, and a run measured in
        // hours must not lose its place to a buffer that was never written
        changeLog.flush();
    }
}
