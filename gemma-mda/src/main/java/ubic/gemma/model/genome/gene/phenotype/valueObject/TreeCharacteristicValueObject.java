package ubic.gemma.model.genome.gene.phenotype.valueObject;

import java.util.Collection;
import java.util.HashSet;

public class TreeCharacteristicValueObject extends CharacteristicValueObject {

    private Collection<TreeCharacteristicValueObject> childs = null;
    private boolean wasFound = false;

    private int deep = 0;

    public TreeCharacteristicValueObject( String value, String valueUri,
            Collection<TreeCharacteristicValueObject> childs ) {
        super( value, "", valueUri, "" );
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

    public int getDeep() {
        return deep;
    }

    public void setDeep( int deep ) {
        this.deep = deep;
    }

    public String toString() {
        return toString( 0 );
    }

    public String toString( int level ) {

        String output = "";

        for ( int i = 0; i < level; i++ ) {
            output = output + " ******* ";
        }

        // output = output + getValue() + deep + "\n";

        output = output + getValue() + "\n";

        level++;
        for ( TreeCharacteristicValueObject treeVO : childs ) {
            output = output + treeVO.toString( level );

        }

        return output;
    }

    @Override
    public int compareTo( CharacteristicValueObject c ) {

        if ( c instanceof TreeCharacteristicValueObject ) {

            TreeCharacteristicValueObject t = ( TreeCharacteristicValueObject ) c;
            if ( this.deep > t.deep ) {
                return -1;
            } else {
                return 1;
            }

        } else {
            return super.compareTo( c );
        }
    }

    public void removeUnused() {

        Collection<TreeCharacteristicValueObject> newRealChilds = new HashSet<TreeCharacteristicValueObject>();
        findRealChild( newRealChilds );
        childs = newRealChilds;

        for ( TreeCharacteristicValueObject tc : childs ) {
            tc.removeUnused();
        }

    }

    private void findRealChild( Collection<TreeCharacteristicValueObject> newRealChilds ) {

        for ( TreeCharacteristicValueObject t : childs ) {
            if ( t.isWasFound() ) {
                newRealChilds.add( t );
            } else {
                t.findRealChild( newRealChilds );
            }
        }
    }

}
