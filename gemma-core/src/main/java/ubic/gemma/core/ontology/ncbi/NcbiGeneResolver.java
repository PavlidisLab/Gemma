package ubic.gemma.core.ontology.ncbi;

import javax.annotation.Nullable;

/**
 * Resolve an NCBI gene id against NCBI itself, for gene URIs Gemma's own gene table does not carry.
 * <p>
 * Gemma does not import every kind of NCBI gene record — QTLs, gene complexes and clusters, pseudogenes
 * and withdrawn entries are all absent — and a study may legitimately annotate a construct from a
 * species Gemma holds no genes for. Measured on production 2026-09-12: 41 characteristics carry a gene
 * id Gemma cannot resolve, and every one is of those kinds. Refusing them would refuse real curation;
 * asking NCBI separates them from a fabricated id.
 *
 * @author gemma
 */
public interface NcbiGeneResolver {

    /**
     * @param ncbiId an NCBI gene id, as taken from a {@code purl.org/commons/record/ncbi_gene/} URI.
     * @return what NCBI holds for the id — never null; check {@link NcbiGeneRecord#isFound()} and
     *         {@link NcbiGeneRecord#isLive()}.
     * @throws NcbiUnavailableException when NCBI could not be reached or answered unreadably. The id is
     *         then unverified, which the caller must not report as fabricated.
     */
    NcbiGeneRecord resolve( int ncbiId ) throws NcbiUnavailableException;
}
