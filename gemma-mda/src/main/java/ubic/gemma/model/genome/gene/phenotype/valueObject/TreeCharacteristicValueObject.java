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
import java.util.HashMap;
import java.util.HashSet;
import java.util.TreeSet;

import ubic.basecode.ontology.model.OntologyTerm;
import ubic.gemma.model.association.phenotype.service.PhenotypeAssociationService;

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

        output = output + getValue() + "   " + getPublicGeneCount() + "\n";

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

    /** the tree is built with many terms in the Ontology, this method removes all nodes not found in the database */
    public void removeUnusedPhenotypes( String rootValueUri ) {

        Collection<TreeCharacteristicValueObject> newRealChilds = new HashSet<TreeCharacteristicValueObject>();
        findRealChild( newRealChilds, rootValueUri );
        this.children = newRealChilds;

        for ( TreeCharacteristicValueObject tc : this.children ) {
            tc.removeUnusedPhenotypes( rootValueUri );
        }
    }

    /** counts gene on a TreeCharacteristicValueObject */
    public void countGeneOccurence( PhenotypeAssociationService associationService, boolean isAdmin, String username ) {

        // everyone can see public
        setPublicGeneCount( associationService.countGenesWithPublicPhenotype( getAllChildrenUri() ) );

        // admin see all private evidence
        if ( isAdmin ) {
            setPrivateGeneCount( associationService.countGenesWithPhenotype( getAllChildrenUri() )
                    - getPublicGeneCount() );
        }
        // user is logged in
        else if ( username != null ) {
            setPrivateGeneCount( associationService.countGenesWithPrivatePhenotype( getAllChildrenUri(), username ) );
        }

        if ( getPublicGeneCount() + getPrivateGeneCount() == 0 ) {
            this.getChildren().clear();
        } else {
            // count for each node of the tree
            for ( TreeCharacteristicValueObject tree : getChildren() ) {
                countGeneOccurence( associationService, tree );
            }
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

    /** counts gene on a TreeCharacteristicValueObject */
    private void countGeneOccurence( PhenotypeAssociationService associationService, TreeCharacteristicValueObject tc ) {

        // set the public gene count
        tc.setPublicGeneCount( associationService.countGenesWithPublicPhenotype( tc.getAllChildrenUri() ) );

        // count for each node of the tree
        for ( TreeCharacteristicValueObject tree : tc.getChildren() ) {
            countGeneOccurence( associationService, tree );
        }
    }

    /** step into the tree and keep tracks of all valueURI */
    private void findAllChildPhenotypeURI( Collection<String> phenotypesToFind ) {

        phenotypesToFind.add( this.getValueUri() );

        for ( TreeCharacteristicValueObject tree : this.getChildren() ) {
            tree.findAllChildPhenotypeURI( phenotypesToFind );
        }
    }

    /** Ontology term to TreeCharacteristicValueObject */
    public static TreeCharacteristicValueObject ontology2TreeCharacteristicValueObjects( OntologyTerm ontologyTerm,
            HashMap<String, TreeCharacteristicValueObject> phenotypeFoundInTree,
            TreeSet<TreeCharacteristicValueObject> treesPhenotypes ) {

        Collection<OntologyTerm> ontologyTerms = ontologyTerm.getChildren( true );

        Collection<TreeCharacteristicValueObject> childs = new HashSet<TreeCharacteristicValueObject>();

        for ( OntologyTerm ot : ontologyTerms ) {

            if ( phenotypeFoundInTree.get( ot.getUri() ) != null ) {

                childs.add( phenotypeFoundInTree.get( ot.getUri() ) );
                treesPhenotypes.remove( phenotypeFoundInTree.get( ot.getUri() ) );
            } else {
                TreeCharacteristicValueObject tree = ontology2TreeCharacteristicValueObjects( ot, phenotypeFoundInTree,
                        treesPhenotypes );
                phenotypeFoundInTree.put( tree.getValueUri(), tree );
                childs.add( tree );
            }
        }

        TreeCharacteristicValueObject treeCharacteristicVO = new TreeCharacteristicValueObject(
                ontologyTerm.getLabel(), ontologyTerm.getUri(), childs );

        return treeCharacteristicVO;
    }

}
