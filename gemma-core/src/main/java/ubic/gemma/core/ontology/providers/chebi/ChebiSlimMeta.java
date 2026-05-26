package ubic.gemma.core.ontology.providers.chebi;

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
 * Sidecar metadata for {@code chebiOntology-slim.owl}.
 *
 * <p>Written by {@code ChebiOntologyService.rebuildSlim(...)} after extraction; read on
 * boot to decide whether the existing slim still covers the current corpus seeds. A
 * mismatch between {@link #seedHash} and the freshly-computed hash of the current corpus
 * seeds means the slim is stale and must be re-extracted, regardless of age.
 *
 * <p>Format is a single JSON object — small enough that humans can eyeball it in a deploy
 * directory and large diagnostic tooling isn't worth building.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class ChebiSlimMeta {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .enable( SerializationFeature.INDENT_OUTPUT );

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

    public static ChebiSlimMeta create( String sourceUrl, Collection<String> seeds,
                                        long slimSizeBytes, long classCount, int axiomCount ) {
        ChebiSlimMeta meta = new ChebiSlimMeta();
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

    public static ChebiSlimMeta readFrom( File in ) throws IOException {
        return MAPPER.readValue( in, ChebiSlimMeta.class );
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
