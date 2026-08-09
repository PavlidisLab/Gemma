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
package ubic.gemma.core.loader.expression.geo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.lang.Nullable;
import ubic.gemma.core.loader.expression.geo.model.GeoChannel;
import ubic.gemma.core.loader.expression.geo.model.GeoSample;
import ubic.gemma.core.loader.expression.geo.model.GeoSeries;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Builds the schema-v1 {@code Investigation.sourceMetadata} document from a parsed {@link GeoSeries}.
 * <p>
 * This is a verbatim cache of what GEO said, not curation: it is rebuildable from the source at any
 * time and carries no judgement of ours. Its purpose is to give the curation agent the raw per-sample
 * view — submitter-written characteristic columns and per-sample titles as they were written — so it
 * can tell "the submitter copy-pasted this column into every row" from "this column is a real
 * experimental factor". Gemma's own converter flattens that view into
 * {@code BioMaterial.characteristics} and cannot answer the question.
 * <p>
 * The document is built from objects already parsed at import; nothing is re-fetched. Several fields
 * it carries are ones the converter discards outright ({@code dataProcessing}, {@code hybProtocol},
 * {@code scanProtocol}, {@code supplementaryFiles}, submission dates).
 * <p>
 * <b>Contract.</b> Schema v1 was agreed with CAB on 2026-08-09
 * ({@code GEMMA_REPLY_2026_08_09_SOURCE_METADATA_BLOB.md}). Rules that are not obvious from the
 * shape:
 * <ul>
 * <li>Keys are camelCase; the consumer normalizes once at ingestion.</li>
 * <li><b>Absent means absent.</b> A field GEO did not state is omitted, never written as null or "".</li>
 * <li>{@code characteristics} is always an object, never an array, even when empty.</li>
 * <li>Channel fields beyond the first are prefixed {@code ch{N}_}.</li>
 * <li>{@code pmids} / {@code dois}, not {@code pubMedIds} — {@code to_snake("pubMedIds")} yields
 * {@code pub_med_ids}, which nothing on the consuming side reads, so the field would silently arrive
 * empty.</li>
 * <li>{@code samples} is a list with {@code accession} on each element, not a GSM-keyed map.</li>
 * </ul>
 *
 * @author Gemma
 */
public class GeoSourceMetadataBuilder {

    private static final Log log = LogFactory.getLog( GeoSourceMetadataBuilder.class );

    /**
     * Schema version written into the document and into
     * {@code Investigation.sourceMetadataSchemaVersion}. A null version in the database marks the
     * older, smaller scrape-path payload written by {@code GeoScrapeServiceImpl}.
     */
    public static final int SCHEMA_VERSION = 1;

    /**
     * Byte ceiling for the serialized document. LONGTEXT holds far more; the cap exists so one
     * pathological submission cannot dominate the table.
     */
    static final int MAX_BYTES = 8 * 1024 * 1024;

    /**
     * Per-sample keys dropped, in order, when the document exceeds {@link #MAX_BYTES}.
     * <p>
     * {@code dataProcessing} goes first because CAB measured it at 25.3% of payload bytes across 500
     * experiments and found it the least useful of the large fields. The four the contract names as
     * never-evictable — {@code characteristics}, {@code title}, {@code sourceName}, {@code organism} —
     * are absent from this list by design; they are what the agent actually reads.
     */
    private static final List<String> EVICTION_ORDER = List.of(
            "dataProcessing", "extractProtocol", "treatmentProtocol", "growthProtocol",
            "hybProtocol", "scanProtocol", "labelProtocol", "supplementaryFiles", "description" );

    private final ObjectMapper objectMapper;

    public GeoSourceMetadataBuilder( ObjectMapper objectMapper ) {
        this.objectMapper = objectMapper;
    }

    /**
     * Identity of the experiment the document is being written for.
     * <p>
     * These cannot be read from the {@link GeoSeries}: {@code experimentId} does not exist until the
     * experiment is persisted, and {@code shortName} is the only thing distinguishing split
     * sub-series from one another — {@code SplitExperimentServiceImpl} puts the {@code .1} in
     * {@code shortName} alone and {@code cloneAccession} copies the GEO accession verbatim, so
     * siblings share both accession and series text.
     */
    public static class ExperimentIdentity {
        private final String shortName;
        @Nullable
        private final Long experimentId;
        private final boolean splitSubseries;
        @Nullable
        private final Set<String> sampleAccessions;

        /**
         * @param sampleAccessions GSM accessions actually belonging to this experiment, or {@code null}
         *                         for "all of the series". This MUST be supplied for a split
         *                         sub-series: the parsed {@link GeoSeries} holds every sample in the
         *                         GEO family, and siblings share both accession and series text, so
         *                         serializing the series wholesale would give each sibling a sample
         *                         list containing the other's samples.
         */
        public ExperimentIdentity( String shortName, @Nullable Long experimentId, boolean splitSubseries,
                @Nullable Set<String> sampleAccessions ) {
            this.shortName = shortName;
            this.experimentId = experimentId;
            this.splitSubseries = splitSubseries;
            this.sampleAccessions = sampleAccessions;
        }
    }

    /**
     * Build the document. Returns {@code null} when there is nothing worth storing, so the caller can
     * leave both columns null rather than storing an empty shell.
     *
     * @param series      the parsed series; its samples supply the per-sample view
     * @param identity    experiment identity, resolved after persist (see {@link ExperimentIdentity})
     * @param harvestedAt when Gemma built this document — our clock, not GEO's. GEO's own dates are
     *                    carried separately as {@code submissionDate} / {@code lastUpdateDate}.
     */
    @Nullable
    public String build( @Nullable GeoSeries series, ExperimentIdentity identity, Date harvestedAt ) {
        if ( series == null ) {
            return null;
        }
        Map<String, Object> doc = new LinkedHashMap<>();
        doc.put( "schemaVersion", SCHEMA_VERSION );
        doc.put( "source", "GEO" );
        doc.put( "sourceFormat", "SOFT" );
        doc.put( "harvestedAt", toIso8601( harvestedAt ) );

        putIfPresent( doc, "accession", series.getGeoAccession() );
        putIfPresent( doc, "shortName", identity.shortName );
        if ( identity.experimentId != null ) {
            doc.put( "experimentId", identity.experimentId );
        }
        if ( identity.splitSubseries ) {
            doc.put( "isSplitSubseries", true );
        }

        putIfPresent( doc, "title", series.getTitle() );
        putIfPresent( doc, "summary", joinNonBlank( series.getSummaries() ) );
        putIfPresent( doc, "overallDesign", series.getOverallDesign() );
        putIfNotEmpty( doc, "organisms", distinctOrganisms( series ) );
        putIfNotEmpty( doc, "pmids", trimmed( series.getPubmedIds() ) );
        // GEO's own dates, distinct from harvestedAt. Requested so downstream can reason about how
        // stale a record is and when the submitter last touched it.
        putIfPresent( doc, "submissionDate", series.getSubmissionDate() );
        putIfPresent( doc, "lastUpdateDate", series.getLastUpdateDate() );
        putIfPresent( doc, "status", series.getStatus() );
        putIfNotEmpty( doc, "supplementaryFiles", trimmed( series.getSupplementaryFiles() ) );
        if ( series.getGeoAccession() != null ) {
            doc.put( "url", "https://www.ncbi.nlm.nih.gov/geo/query/acc.cgi?acc=" + series.getGeoAccession() );
        }

        // sampleCount is GEO's SERIES count; samples[] is what belongs to THIS experiment. The two
        // differ exactly when the series was split — which is the case the contract's "may exceed
        // samples.length" is describing. Serializing the series wholesale would hand each split
        // sibling a sample list containing the other's samples, and since siblings share accession,
        // title, summary and overallDesign verbatim, nothing else in the document would reveal the
        // error.
        doc.put( "sampleCount", series.getSamples().size() );

        List<Map<String, Object>> samples = new ArrayList<>();
        for ( GeoSample s : series.getSamples() ) {
            if ( identity.sampleAccessions != null && !identity.sampleAccessions.contains( s.getGeoAccession() ) ) {
                continue;
            }
            samples.add( buildSample( s ) );
        }
        if ( identity.sampleAccessions != null && samples.size() != identity.sampleAccessions.size() ) {
            // Not fatal — the document is still truthful about what it contains — but it means the
            // experiment references a sample the parsed series does not have, which is worth knowing.
            log.warn( "Source metadata for " + identity.shortName + ": experiment claims "
                    + identity.sampleAccessions.size() + " samples but only " + samples.size()
                    + " were found in the parsed series." );
        }
        doc.put( "samples", samples );

        List<String> truncated = new ArrayList<>();
        String json = serializeWithinBudget( doc, samples, truncated );
        if ( json == null ) {
            return null;
        }
        return json;
    }

    private Map<String, Object> buildSample( GeoSample s ) {
        Map<String, Object> m = new LinkedHashMap<>();
        putIfPresent( m, "accession", s.getGeoAccession() );
        putIfPresent( m, "title", s.getTitle() );
        putIfPresent( m, "description", s.getDescription() );
        putIfPresent( m, "dataProcessing", s.getDataProcessing() );
        putIfPresent( m, "hybProtocol", s.getHybProtocol() );
        putIfPresent( m, "scanProtocol", s.getScanProtocol() );
        putIfPresent( m, "instrumentModel", s.getInstrumentModel() );
        putIfPresent( m, "librarySelection", s.getLibrarySelection() );
        if ( s.getLibStrategy() != null ) {
            m.put( "libraryStrategy", s.getLibStrategy().toString() );
        }
        if ( s.getLibSource() != null ) {
            m.put( "librarySource", s.getLibSource().toString() );
        }
        putIfPresent( m, "submissionDate", s.getSubmissionDate() );
        putIfPresent( m, "lastUpdateDate", s.getLastUpdateDate() );
        putIfNotEmpty( m, "supplementaryFiles", trimmed( s.getSupplementaryFiles() ) );

        // Channel fields. The first channel is unprefixed; subsequent ones carry ch{N}_ so a
        // two-channel sample does not silently overwrite its own first channel.
        Map<String, String> characteristics = new LinkedHashMap<>();
        List<String> unparsedCharacteristics = new ArrayList<>();
        for ( GeoChannel c : s.getChannels() ) {
            String prefix = c.getChannelNumber() > 1 ? "ch" + c.getChannelNumber() + "_" : "";
            putIfPresent( m, prefix + "sourceName", c.getSourceName() );
            putIfPresent( m, prefix + "organism", c.getOrganism() );
            putIfPresent( m, prefix + "growthProtocol", c.getGrowthProtocol() );
            putIfPresent( m, prefix + "treatmentProtocol", c.getTreatmentProtocol() );
            putIfPresent( m, prefix + "extractProtocol", c.getExtractProtocol() );
            putIfPresent( m, prefix + "label", c.getLabel() );
            putIfPresent( m, prefix + "labelProtocol", c.getLabelProtocol() );
            if ( c.getMolecule() != null ) {
                m.put( prefix + "molecule", c.getMolecule().toString() );
            }
            collectCharacteristics( c, prefix, characteristics, unparsedCharacteristics );
        }
        // Always an object, never an array, even when empty — the contract is explicit, because an
        // empty array and an empty object deserialize differently on the consuming side.
        m.put( "characteristics", characteristics );
        if ( !unparsedCharacteristics.isEmpty() ) {
            m.put( "characteristicsUnparsed", unparsedCharacteristics );
        }
        return m;
    }

    /**
     * Split GEO's raw {@code "tag: value"} characteristic strings into a tag-keyed object.
     * <p>
     * The keys are names the submitter wrote ({@code BioSource}, {@code Genetic modification}) and are
     * left exactly as written — this map is data-keyed, so rewriting its keys is corruption, not
     * normalization. Entries with no colon cannot be split without inventing a key, so they are kept
     * verbatim in a separate list rather than guessed at or dropped.
     */
    private void collectCharacteristics( GeoChannel c, String prefix, Map<String, String> into, List<String> unparsed ) {
        for ( String raw : c.getCharacteristics() ) {
            if ( raw == null || raw.trim().isEmpty() ) {
                continue;
            }
            int colon = raw.indexOf( ':' );
            if ( colon <= 0 ) {
                unparsed.add( raw.trim() );
                continue;
            }
            String tag = raw.substring( 0, colon ).trim();
            String value = raw.substring( colon + 1 ).trim();
            if ( tag.isEmpty() ) {
                unparsed.add( raw.trim() );
                continue;
            }
            into.put( prefix + tag, value );
        }
    }

    /**
     * Serialize, and if the result exceeds {@link #MAX_BYTES} drop per-sample fields in
     * {@link #EVICTION_ORDER} until it fits, recording what went in {@code truncated}. The sha256 is
     * computed over the final content, with its own placeholder empty, so it is reproducible by a
     * consumer that blanks the field and re-hashes.
     */
    @Nullable
    private String serializeWithinBudget( Map<String, Object> doc, List<Map<String, Object>> samples, List<String> truncated ) {
        for ( int i = 0; ; i++ ) {
            doc.put( "truncated", truncated );
            doc.put( "sha256", "" );
            String candidate;
            try {
                candidate = objectMapper.writeValueAsString( doc );
            } catch ( JsonProcessingException e ) {
                log.warn( "Failed to serialize source metadata; storing nothing.", e );
                return null;
            }
            if ( candidate.getBytes( StandardCharsets.UTF_8 ).length <= MAX_BYTES || i >= EVICTION_ORDER.size() ) {
                doc.put( "sha256", sha256( candidate ) );
                try {
                    return objectMapper.writeValueAsString( doc );
                } catch ( JsonProcessingException e ) {
                    log.warn( "Failed to serialize source metadata; storing nothing.", e );
                    return null;
                }
            }
            String victim = EVICTION_ORDER.get( i );
            boolean removedAny = false;
            for ( Map<String, Object> sample : samples ) {
                removedAny |= sample.remove( victim ) != null;
            }
            if ( removedAny ) {
                truncated.add( victim );
                log.info( "Source metadata over " + MAX_BYTES + " bytes; dropped per-sample '" + victim + "'." );
            }
        }
    }

    private static String sha256( String s ) {
        try {
            MessageDigest md = MessageDigest.getInstance( "SHA-256" );
            byte[] digest = md.digest( s.getBytes( StandardCharsets.UTF_8 ) );
            StringBuilder sb = new StringBuilder( digest.length * 2 );
            for ( byte b : digest ) {
                sb.append( Character.forDigit( ( b >> 4 ) & 0xF, 16 ) ).append( Character.forDigit( b & 0xF, 16 ) );
            }
            return sb.toString();
        } catch ( NoSuchAlgorithmException e ) {
            throw new IllegalStateException( "SHA-256 is required by the JLS and must be present.", e );
        }
    }

    private static Set<String> distinctOrganisms( GeoSeries series ) {
        Set<String> organisms = new LinkedHashSet<>();
        for ( GeoSample s : series.getSamples() ) {
            for ( GeoChannel c : s.getChannels() ) {
                if ( c.getOrganism() != null && !c.getOrganism().trim().isEmpty() ) {
                    organisms.add( c.getOrganism().trim() );
                }
            }
        }
        return organisms;
    }

    private static List<String> trimmed( @Nullable Collection<String> values ) {
        List<String> out = new ArrayList<>();
        if ( values == null ) {
            return out;
        }
        for ( String v : values ) {
            if ( v != null && !v.trim().isEmpty() ) {
                out.add( v.trim() );
            }
        }
        return out;
    }

    @Nullable
    private static String joinNonBlank( @Nullable Collection<String> values ) {
        List<String> parts = trimmed( values );
        return parts.isEmpty() ? null : String.join( "\n", parts );
    }

    /** Absent means absent: a blank or null value contributes no key at all. */
    private static void putIfPresent( Map<String, Object> m, String key, @Nullable String value ) {
        if ( value != null && !value.trim().isEmpty() ) {
            m.put( key, value.trim() );
        }
    }

    private static void putIfNotEmpty( Map<String, Object> m, String key, Collection<String> values ) {
        if ( !values.isEmpty() ) {
            m.put( key, values );
        }
    }

    private static String toIso8601( Date d ) {
        return java.time.Instant.ofEpochMilli( d.getTime() ).toString();
    }
}
