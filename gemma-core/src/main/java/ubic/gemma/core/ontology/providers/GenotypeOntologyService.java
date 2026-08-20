package ubic.gemma.core.ontology.providers;

/**
 * <a href="https://obofoundry.org/ontology/geno.html">Genotype Ontology</a> (GENO) — zygosity,
 * alleles and genotype structure.
 *
 * <p>Loaded because Gemma's corpus already uses it and could not resolve it: {@code GENO:0000135}
 * <em>Heterozygous</em> carries a usage count of 285, so curators committed those annotations while
 * {@code /annotations/term} returned 404 for every one of them. That is the same shape as a
 * half-imported dataset — the URI is real, the server just cannot say anything about it.</p>
 *
 * <p>Small: ~1.4 MB, 589 terms. It complements rather than competes with TGEMO, which dominates the
 * genotype category ({@code TGEMO_00001} <em>Homozygous negative</em>, usage 4,540) but carries no
 * zygosity vocabulary of its own.</p>
 *
 * <p>Disabled by default; {@code load.genotypeOntology} in {@code basecode.properties}.</p>
 */
public class GenotypeOntologyService extends AbstractBaseCodeOntologyService {

    public GenotypeOntologyService() {
        super( "Genotype Ontology", "genotypeOntology" );
    }
}
