package ubic.gemma.model.genome;

import ubic.gemma.model.common.IdentifiableValueObject;

import java.util.Objects;

/**
 * A very simple value object to represent a physical location
 */
@SuppressWarnings("unused") // Getters used by JSON serializer in the REST API
public class PhysicalLocationValueObject extends IdentifiableValueObject<PhysicalLocation> {

    private Long nucleotide;
    private Integer nucleotideLength;
    private String strand;
    private Integer bin;

    private String chromosome;
    private TaxonValueObject taxon;

    public PhysicalLocationValueObject() {
        super();
    }

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

    @Override
    public int hashCode() {
        int hash = 0;
        int prime = 199;
        hash = prime * ( hash + nucleotide.intValue() + nucleotideLength + bin + strand.hashCode() + chromosome
                .hashCode() + taxon.getId().hashCode() );
        return hash;
    }

    @Override
    public boolean equals( Object obj ) {
        if ( obj == this )
            return true;
        if ( !( obj instanceof PhysicalLocationValueObject ) )
            return false;
        PhysicalLocationValueObject that = ( PhysicalLocationValueObject ) obj;
        return Objects.equals( that.nucleotideLength, this.nucleotideLength ) && Objects
                .equals( that.nucleotide, this.nucleotide ) && ( ( that.taxon == null && this.taxon == null ) || (
                that.taxon != null && this.taxon != null && that.taxon.getId().equals( this.taxon.getId() ) ) )
                && that.strand.equals( this.strand ) && that.chromosome.equals( this.chromosome ) && that.bin
                .equals( this.bin );
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

    public void setNucleotideLength( Integer nucleotideLength ) {
        this.nucleotideLength = nucleotideLength;
    }

    public void setStrand( String strand ) {
        this.strand = strand;
    }

    public void setBin( Integer bin ) {
        this.bin = bin;
    }

    public void setChromosome( String chromosome ) {
        this.chromosome = chromosome;
    }

    public void setTaxon( TaxonValueObject taxon ) {
        this.taxon = taxon;
    }

    public void setNucleotide( Long nucleotide ) {
        this.nucleotide = nucleotide;
    }
}
