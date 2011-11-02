/*
 * The Gemma project
 * 
 * Copyright (c) 2007 University of British Columbia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package ubic.gemma.model.genome.gene.phenotype.valueObject;

import java.util.Collection;
import java.util.HashSet;

public class TreeCharacteristicValueObject extends CharacteristicValueObject {

    private Collection<TreeCharacteristicValueObject> children = null;
    /** phenotype present in the database */
    private boolean dbPhenotype = false;

    private int deep = 0;

    public TreeCharacteristicValueObject( String value, String valueUri,
            Collection<TreeCharacteristicValueObject> children ) {
        super( value, "", valueUri, "" );
        this.children = children;
    }

    public Collection<TreeCharacteristicValueObject> getChildren() {
        return this.children;
    }

    public int getDeep() {
        return this.deep;
    }

    public void setDeep( int deep ) {
        this.deep = deep;
    }

    @Override
    public String toString() {
        return toString( 0 );
    }

    public boolean isDbPhenotype() {
        return this.dbPhenotype;
    }

    public void setDbPhenotype( boolean dbPhenotype ) {
        this.dbPhenotype = dbPhenotype;
    }

    public String toString( int level ) {

        String output = "";

        for ( int i = 0; i < level; i++ ) {
            output = output + " ******* ";
        }

        // output = output + getValue() + deep + "\n";

        output = output + getValue() + "\n";

        int currentLevel = level + 1;
        for ( TreeCharacteristicValueObject treeVO : this.children ) {
            output = output + treeVO.toString( currentLevel );

        }

        return output;
    }

    @Override
    /** order TreeCharacteristicValueObjects by deep */
    public int compareTo( CharacteristicValueObject c ) {

        if ( c instanceof TreeCharacteristicValueObject ) {

            TreeCharacteristicValueObject t = ( TreeCharacteristicValueObject ) c;

            // same deep, lets order by value
            if ( this.deep == t.deep ) {
                return this.getValue().compareToIgnoreCase( c.getValue() );
            } else if ( this.deep > t.deep ) {
                return -1;
            } else {
                return 1;
            }

        }
        return super.compareTo( c );

    }

    /** remove all nodes in the trees found in the Ontology but not in db */
    public void removeUnusedPhenotypes() {

        // the new childs nodes, all node between root and flag children were removed
        this.children = findNewChildren();

        // for the new childs found remove all nodes that were not flag between the childs and the next flagged child if
        // any
        for ( TreeCharacteristicValueObject tc : this.children ) {
            tc.removeUnusedPhenotypes();
        }
    }

    /** remove all nodes in the trees found between the root and a child node that was flag */
    private Collection<TreeCharacteristicValueObject> findNewChildren() {

        Collection<TreeCharacteristicValueObject> newChildren = new HashSet<TreeCharacteristicValueObject>();

        for ( TreeCharacteristicValueObject t : this.children ) {
            if ( t.isDbPhenotype() ) {
                newChildren.add( t );
            } else {
                t.findNewChildren();
            }
        }

        return newChildren;
    }

}
