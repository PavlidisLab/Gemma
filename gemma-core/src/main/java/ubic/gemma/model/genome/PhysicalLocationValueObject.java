package ubic.gemma.model.genome;

import lombok.EqualsAndHashCode;
import ubic.gemma.model.IdentifiableValueObject;

import java.util.Objects;

/**
 * A very simple value object to represent a physical location
 */
@SuppressWarnings("unused") // Getters used by JSON serializer in the REST API
@EqualsAndHashCode(of = { "nucleotide", "nucleotideLength", "bin", "strand", "chromosome", "taxon" }, callSuper = false)
public class PhysicalLocationValueObject extends IdentifiableValueObject<PhysicalLocation> {

    private Long nucleotide;
    private Integer nucleotideLength;
    private String strand;
    private Integer bin;

    private String chromosome;
    private TaxonValueObject taxon;

    public PhysicalLocationValueObject( PhysicalLocation location ) {
        super( location );
        this.nucleotide = location.getNucleotide();
        this.nucleotideLength = location.getNucleotideLength();
        this.strand = location.getStrand();
        this.bin = location.getBin();
        if ( location.getChromosome() != null ) {
            this.chromosome = location.getChromosome().getName();
            this.taxon = new TaxonValueObject( location.getChromosome().getTaxon() );
        }
    }

    public Long getNucleotide() {
        return nucleotide;
    }

    public Integer getNucleotideLength() {
        return nucleotideLength;
    }

    public String getStrand() {
        return strand;
    }

    public Integer getBin() {
        return bin;
    }

    public String getChromosome() {
        return chromosome;
    }

    public TaxonValueObject getTaxon() {
        return taxon;
    }
}
