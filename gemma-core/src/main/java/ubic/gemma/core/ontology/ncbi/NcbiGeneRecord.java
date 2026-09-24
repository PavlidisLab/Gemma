package ubic.gemma.core.ontology.ncbi;

import javax.annotation.Nullable;
import java.io.Serializable;

/**
 * What NCBI says about one gene id, as much of it as validation needs.
 *
 * <h2>Why {@code live} is separate from {@code found}</h2>
 * A withdrawn record still has a summary: NCBI answers HTTP 200 with the record's old symbol and a
 * non-empty {@code status} (gene 918, {@code CD3W}, reads {@code status=2}). A fabricated id answers
 * HTTP 200 as well, with an {@code error} field on the uid object. Both are "not a usable gene" and they
 * are different findings, so they are different fields rather than one boolean.
 *
 * @author gemma
 */
public class NcbiGeneRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    public static NcbiGeneRecord notFound( int ncbiId ) {
        return new NcbiGeneRecord( ncbiId, null, null, null, false, false );
    }

    private final int ncbiId;
    @Nullable
    private final String symbol;
    @Nullable
    private final String name;
    @Nullable
    private final String organism;
    private final boolean found;
    private final boolean live;

    public NcbiGeneRecord( int ncbiId, @Nullable String symbol, @Nullable String name, @Nullable String organism,
            boolean found, boolean live ) {
        this.ncbiId = ncbiId;
        this.symbol = symbol;
        this.name = name;
        this.organism = organism;
        this.found = found;
        this.live = live;
    }

    public int getNcbiId() {
        return ncbiId;
    }

    /** NCBI's {@code name} field, which is the official symbol. */
    @Nullable
    public String getSymbol() {
        return symbol;
    }

    /** NCBI's {@code description} field, which is the official full name. */
    @Nullable
    public String getName() {
        return name;
    }

    /** Scientific name, e.g. {@code Mus musculus}. NCBI does not serve the common name Gemma displays. */
    @Nullable
    public String getOrganism() {
        return organism;
    }

    /** Whether NCBI has a record for this id at all. */
    public boolean isFound() {
        return found;
    }

    /** Whether that record is current, as opposed to withdrawn / secondary. */
    public boolean isLive() {
        return live;
    }

    @Override
    public String toString() {
        return "NcbiGeneRecord{" + ncbiId + ", symbol=" + symbol + ", found=" + found + ", live=" + live + '}';
    }
}
