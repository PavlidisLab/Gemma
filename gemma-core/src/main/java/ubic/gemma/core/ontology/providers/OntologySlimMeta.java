package ubic.gemma.core.ontology.providers;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Collection;
import java.util.HexFormat;
import java.util.List;

/**
 * Sidecar metadata for an {@code *Ontology-slim.owl} cache.
 *
 * <p>Written by a {@link SlimmableOntologyService}'s rebuild path after extraction;
 * read on boot to decide whether the existing slim still covers the current corpus
 * seeds. A mismatch between {@link #seedHash} and the freshly-computed hash of the
 * current corpus seeds means the slim is stale and must be re-extracted, regardless
 * of age.
 *
 * <p>Format is a single JSON object — small enough that humans can eyeball it in a deploy
 * directory and large diagnostic tooling isn't worth building.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class OntologySlimMeta {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .enable( SerializationFeature.INDENT_OUTPUT );

    /**
     * Bumped when the meaning of a field changes or the slim is built a materially different way.
     * A reader seeing an unknown or absent version treats the slim as stale rather than guessing,
     * so an old cache is rebuilt instead of being misread.
     */
    public static final int SCHEMA_VERSION = 2;

    @JsonProperty("schema_version")
    public int schemaVersion;

    /**
     * Which seeding rule produced this slim, e.g. {@code corpus} or {@code corpus+role:CHEBI_23888}.
     *
     * <p>Freshness cannot be decided by {@link #seedHash} alone. The hash covers the seed URIs the
     * resolver handed over; seeds the EXTRACTOR derives (role bearers) never enter it, so widening
     * the policy leaves the hash identical and a narrower slim looks perfectly fresh forever. This
     * field is what makes a policy change visible.
     */
    @JsonProperty("seed_policy")
    public String seedPolicy;

    @JsonProperty("source_url")
    public String sourceUrl;

    @JsonProperty("generated_at")
    public String generatedAt;

    @JsonProperty("seed_count")
    public int seedCount;

    /** SHA-256 of the sorted seed URIs joined by {@code \n}. */
    @JsonProperty("seed_hash")
    public String seedHash;

    @JsonProperty("slim_size_bytes")
    public long slimSizeBytes;

    @JsonProperty("class_count")
    public long classCount;

    @JsonProperty("axiom_count")
    public int axiomCount;

    public static OntologySlimMeta create( String sourceUrl, Collection<String> seeds,
                                           long slimSizeBytes, long classCount, int axiomCount ) {
        return create( sourceUrl, SEED_POLICY_CORPUS, seeds, slimSizeBytes, classCount, axiomCount );
    }

    /** Seeded only from CHEBI URIs already used in the corpus. */
    public static final String SEED_POLICY_CORPUS = "corpus";

    /** Seeded from corpus usage PLUS every bearer of the given role(s); see the extractor. */
    public static String seedPolicyWithRoles( Collection<String> roleUris ) {
        if ( roleUris == null || roleUris.isEmpty() ) {
            return SEED_POLICY_CORPUS;
        }
        return SEED_POLICY_CORPUS + "+role:" + roleUris.stream().sorted()
                .map( u -> u.substring( u.lastIndexOf( '/' ) + 1 ) )
                .collect( java.util.stream.Collectors.joining( "," ) );
    }

    public static OntologySlimMeta create( String sourceUrl, String seedPolicy, Collection<String> seeds,
                                           long slimSizeBytes, long classCount, int axiomCount ) {
        OntologySlimMeta meta = new OntologySlimMeta();
        meta.schemaVersion = SCHEMA_VERSION;
        meta.seedPolicy = seedPolicy;
        meta.sourceUrl = sourceUrl;
        meta.generatedAt = Instant.now().toString();
        meta.seedCount = seeds.size();
        meta.seedHash = hashSeeds( seeds );
        meta.slimSizeBytes = slimSizeBytes;
        meta.classCount = classCount;
        meta.axiomCount = axiomCount;
        return meta;
    }

    public void writeTo( File out ) throws IOException {
        MAPPER.writeValue( out, this );
    }

    public static OntologySlimMeta readFrom( File in ) throws IOException {
        return MAPPER.readValue( in, OntologySlimMeta.class );
    }

    /**
     * SHA-256 over the sorted seed list. Stable across runs given the same seed set, so two
     * processes computing it independently agree. Used to detect seed-set drift between
     * the meta's snapshot and the live corpus.
     */
    public static String hashSeeds( Collection<String> seeds ) {
        List<String> sorted = seeds.stream().sorted().toList();
        try {
            MessageDigest md = MessageDigest.getInstance( "SHA-256" );
            for ( String s : sorted ) {
                md.update( s.getBytes( StandardCharsets.UTF_8 ) );
                md.update( ( byte ) '\n' );
            }
            return HexFormat.of().formatHex( md.digest() );
        } catch ( NoSuchAlgorithmException e ) {
            // SHA-256 is a standard MessageDigest algorithm guaranteed by every JRE
            throw new IllegalStateException( "SHA-256 missing from JRE", e );
        }
    }

    @SuppressWarnings("unused")
    private static byte[] readAll( File f ) throws IOException {
        return Files.readAllBytes( f.toPath() );
    }
}
