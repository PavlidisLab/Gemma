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

    // if we need to reconstruct part of the tree in the cache we need to know highest root parent
    private String rootOfTree = "";

    public TreeCharacteristicValueObject( String value, String valueUri,
            Collection<TreeCharacteristicValueObject> children ) {
        super( value, "", valueUri, "" );
        this.children = children;
    }

    public Collection<TreeCharacteristicValueObject> getChildren() {
        return this.children;
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

    public String getRootOfTree() {
        return this.rootOfTree;
    }

    public void setRootOfTree( String rootOfTree ) {
        this.rootOfTree = rootOfTree;
    }

    public String toString( int level ) {

        String output = "";

        for ( int i = 0; i < level; i++ ) {
            output = output + " ******* ";
        }

        output = output + getValue() + "   " + getOccurence() + "\n";

        int currentLevel = level + 1;
        for ( TreeCharacteristicValueObject treeVO : this.children ) {
            output = output + treeVO.toString( currentLevel );
        }

        return output;
    }

    @Override
    public int compareTo( CharacteristicValueObject c ) {

        if ( c instanceof TreeCharacteristicValueObject ) {
            return this.getValue().compareToIgnoreCase( c.getValue() );
        }
        return super.compareTo( c );
    }

    /** return all valueUri of children */
    public Collection<String> getAllChildrenUri() {

        Collection<String> childrenURI = new HashSet<String>();

        findAllChildPhenotypeURI( childrenURI );

        return childrenURI;
    }

    /** step into the tree and keep tracks of all valueURI */
    private void findAllChildPhenotypeURI( Collection<String> phenotypesToFind ) {

        phenotypesToFind.add( this.getValueUri() );

        for ( TreeCharacteristicValueObject tree : this.getChildren() ) {
            tree.findAllChildPhenotypeURI( phenotypesToFind );
        }
    }

    /** the tree is built with many terms in the Ontology, this method removes all nodes not found in the database*/
    public void removeUnusedPhenotypes( String rootValueUri ) {

        Collection<TreeCharacteristicValueObject> newRealChilds = new HashSet<TreeCharacteristicValueObject>();
        findRealChild( newRealChilds, rootValueUri );
        this.children = newRealChilds;

        for ( TreeCharacteristicValueObject tc : this.children ) {
            tc.removeUnusedPhenotypes( rootValueUri );
        }

    }

    private void findRealChild( Collection<TreeCharacteristicValueObject> newRealChilds, String rootValueUri ) {

        for ( TreeCharacteristicValueObject t : this.children ) {
            if ( t.isDbPhenotype() ) {
                t.setRootOfTree( rootValueUri );
                newRealChilds.add( t );
            } else {
                t.findRealChild( newRealChilds, rootValueUri );
            }
        }
    }

}
