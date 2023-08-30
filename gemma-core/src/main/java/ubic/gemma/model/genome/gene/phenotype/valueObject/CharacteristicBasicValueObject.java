package ubic.gemma.model.genome.gene.phenotype.valueObject;

import lombok.Data;
import lombok.EqualsAndHashCode;
import ubic.gemma.model.IdentifiableValueObject;
import ubic.gemma.model.annotations.GemmaWebOnly;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.expression.experiment.Statement;

@Data
@EqualsAndHashCode(callSuper = true)
public class CharacteristicBasicValueObject extends IdentifiableValueObject<Characteristic> {

    private String value;
    private String valueUri;
    private String category;
    private String categoryUri;

    @GemmaWebOnly
    private String predicate;
    @GemmaWebOnly
    private String predicateUri;
    @GemmaWebOnly
    private CharacteristicBasicValueObject object;
    @GemmaWebOnly
    private String secondPredicate;
    @GemmaWebOnly
    private String secondPredicateUri;
    @GemmaWebOnly
    private CharacteristicBasicValueObject secondObject;

    /**
     * Required when using the class as a spring bean.
     */
    public CharacteristicBasicValueObject() {
        super();
    }

    public CharacteristicBasicValueObject( String value, String valueUri, String category,
            String categoryUri ) {
        super( ( Long ) null );
        this.value = value;
        this.valueUri = valueUri;
        this.category = category;
        this.categoryUri = categoryUri;
    }

    public CharacteristicBasicValueObject( Characteristic c ) {
        super( c );
        this.value = c.getValue();
        this.valueUri = c.getValueUri();
        this.category = c.getCategory();
        this.categoryUri = c.getCategoryUri();
    }

    public CharacteristicBasicValueObject( Statement s ) {
        this( ( Characteristic ) s );
        this.predicate = s.getPredicate();
        this.predicateUri = s.getPredicateUri();
        if ( s.getObject() != null ) {
            this.object = new CharacteristicBasicValueObject( s.getObject() );
        }
        this.secondPredicate = s.getSecondPredicate();
        this.secondPredicateUri = s.getSecondPredicateUri();
        if ( s.getSecondObject() != null ) {
            this.secondObject = new CharacteristicBasicValueObject( s.getSecondObject() );
        }
    }
}
