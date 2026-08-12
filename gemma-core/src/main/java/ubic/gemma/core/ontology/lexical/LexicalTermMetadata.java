package ubic.gemma.core.ontology.lexical;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Descriptive metadata carried alongside a {@link LexicalTerm} — the flat-vocabulary equivalent of the
 * annotation properties (definition, species, category) that a real ontology term carries.
 * <p>
 * None of this drives annotation. It exists so a client resolving a cell line or strain can see WHAT it
 * resolved: a name alone cannot tell you that {@code B1} is a rat cell line rather than a human one, nor
 * that the line you just picked is a known misidentified one.
 * <p>
 * 🛑 <b>Gemma filters nothing by species.</b> Every cell line in Cellosaurus is indexed and searchable
 * regardless of taxon, deliberately — deciding which species are "in scope" is the caller's business, and
 * hard-coding a scope here would silently drop hits the moment the project widened. The species is
 * reported so the caller can make that decision with the facts; see the class javadoc of
 * {@link ubic.gemma.core.ontology.providers.CellosaurusOntologyService}.
 * <p>
 * Fields are source-dependent and any of them may be null: Cellosaurus fills the cell-line ones, MGI fills
 * {@link #strainType()}. A null means "this source does not say", never "no".
 *
 * @param species     every organism the entry derives from, in source order. A LIST because hybridomas and
 *                    hybrid cell lines genuinely derive from two or more — a mouse myeloma fused to a human
 *                    B cell is both, and collapsing that to one taxon invents a fact.
 * @param cellLineType Cellosaurus cell-line category (e.g. {@code Cancer cell line}, {@code Hybridoma},
 *                    {@code Induced pluripotent stem cell}).
 * @param sex         sex of the donor organism, where stated.
 * @param strainType  MGI strain category (e.g. {@code inbred strain}, {@code congenic}, {@code consomic}).
 * @param comment     free-text descriptive note; surfaced as the term's definition.
 * @param problematic non-null iff the source flags the entry as problematic, carrying the stated reason
 *                    (e.g. {@code Misidentified/contaminated}). Advisory metadata for a curator to see —
 *                    it is NOT a tag to annotate an experiment with.
 */
public record LexicalTermMetadata(
        List<Taxon> species,
        @Nullable String cellLineType,
        @Nullable String sex,
        @Nullable String strainType,
        @Nullable String comment,
        @Nullable String problematic
) {

    public static final LexicalTermMetadata EMPTY =
            new LexicalTermMetadata( List.of(), null, null, null, null, null );

    public LexicalTermMetadata {
        species = species == null ? List.of() : List.copyOf( species );
    }

    public boolean isEmpty() {
        return species.isEmpty() && cellLineType == null && sex == null && strainType == null
                && comment == null && problematic == null;
    }

    /**
     * An organism an entry derives from.
     * <p>
     * The NCBI taxon id is the part that matters and the reason this is not just a label: <i>Rattus
     * norvegicus</i> (10116) and <i>Rattus rattus</i> (10117) both read as "rat" in prose and are one letter
     * apart in any abbreviation, so a caller given only a name cannot reliably tell them apart. Given the id
     * it cannot get them wrong.
     *
     * @param ncbiTaxonId NCBI taxonomy id, e.g. 9606.
     * @param label       the source's own rendering, e.g. {@code Homo sapiens (Human)}. May be null.
     */
    public record Taxon( int ncbiTaxonId, @Nullable String label ) {
    }
}
