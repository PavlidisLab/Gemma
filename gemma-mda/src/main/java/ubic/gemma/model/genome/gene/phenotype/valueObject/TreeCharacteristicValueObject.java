package ubic.gemma.model.genome.gene.phenotype.valueObject;

import java.util.Collection;

public class TreeCharacteristicValueObject extends CharacteristicValueObject {

    private Collection<TreeCharacteristicValueObject> childs = null;
    private boolean wasFound = false;

    public TreeCharacteristicValueObject( String value, String valueUri,
            Collection<TreeCharacteristicValueObject> childs) {
        super( value,"", valueUri,"" );
        this.childs = childs;
    }



    public Collection<TreeCharacteristicValueObject> getChilds() {
        return childs;
    }

    public void setWasFound( boolean wasFound ) {
        this.wasFound = wasFound;
    }

    public boolean isWasFound() {
        return wasFound;
    }

    
    

}
