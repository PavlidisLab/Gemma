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

public class TreeCharacteristicValueObject extends CharacteristicValueObject {

    private TreeSet<TreeCharacteristicValueObject> children = new TreeSet<TreeCharacteristicValueObject>();
    /** phenotype present in the database */
    private boolean dbPhenotype = false;

    // if we need to reconstruct part of the tree in the cache we need to know highest root parent
    private String rootOfTree = "";

    private String _id = "";
    private String _parent = null;
    private boolean _is_leaf = false;

    public TreeCharacteristicValueObject( String value, String valueUri, TreeSet<TreeCharacteristicValueObject> children ) {
        super( value, "", valueUri, "" );
        this.children = children;
        this._id = this.urlId;
    }

    public TreeCharacteristicValueObject( String value, String valueUri ) {
        super( value, "", valueUri, "" );
        this._id = this.urlId;
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

    public String get_id() {
        return this._id;
    }

    public void set_id( String _id ) {
        this._id = _id;
    }

    public String get_parent() {
        return this._parent;
    }

    public void set_parent( String _parent ) {
        this._parent = _parent;
    }

    public boolean is_is_leaf() {
        return this._is_leaf;
    }

    public void set_is_leaf( boolean _is_leaf ) {
        this._is_leaf = _is_leaf;
    }

    public String toString( int level ) {

        String output = "";

        for ( int i = 0; i < level; i++ ) {
            output = output + " ******* ";
        }

        output = output + getValue() + "   " + getPublicGeneCount() + " (" + getPrivateGeneCount() + ")\n";

        int currentLevel = level + 1;
        for ( TreeCharacteristicValueObject treeVO : this.children ) {
            output = output + treeVO.toString( currentLevel );
        }

        return output;
    }

    /** return all valueUri of children */
    public Collection<String> getAllChildrenUri() {

        Collection<String> childrenURI = new HashSet<String>();

        findAllChildPhenotypeURI( childrenURI );

        return childrenURI;
    }

    /** the tree is built with many terms in the Ontology, this method removes all nodes not found in the database */
    public void removeUnusedPhenotypes( String rootValueUri ) {

        TreeSet<TreeCharacteristicValueObject> newRealChilds = new TreeSet<TreeCharacteristicValueObject>();
        findRealChild( newRealChilds, rootValueUri );
        this.children = newRealChilds;

        if ( this.children.isEmpty() ) {
            this.set_is_leaf( true );
        }

        for ( TreeCharacteristicValueObject tc : this.children ) {
            tc.set_parent( this.getUrlId() );
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

        TreeSet<TreeCharacteristicValueObject> childs = new TreeSet<TreeCharacteristicValueObject>();

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

    /** counts each public occurrence of genes for a phenotype */
    public void countPublicGeneForEachNode( HashMap<String, HashSet<Integer>> phenotypesGenesAssociations ) {

        HashSet<Integer> allGenes = new HashSet<Integer>();

        for ( TreeCharacteristicValueObject tc : this.children ) {

            tc.countPublicGeneForEachNode( phenotypesGenesAssociations );

            if ( phenotypesGenesAssociations.get( tc.getValueUri() ) != null ) {
                allGenes.addAll( phenotypesGenesAssociations.get( tc.getValueUri() ) );

                if ( phenotypesGenesAssociations.get( this.valueUri ) != null ) {
                    phenotypesGenesAssociations.get( this.valueUri ).addAll(
                            phenotypesGenesAssociations.get( tc.getValueUri() ) );
                } else {
                    HashSet<Integer> genesNBCI = new HashSet<Integer>();
                    genesNBCI.addAll( phenotypesGenesAssociations.get( tc.getValueUri() ) );
                    phenotypesGenesAssociations.put( this.valueUri, genesNBCI );
                }
            }
        }

        if ( phenotypesGenesAssociations.get( this.valueUri ) != null ) {
            allGenes.addAll( phenotypesGenesAssociations.get( this.valueUri ) );
        }

        this.publicGeneCount = allGenes.size();
    }

    /** counts each private occurrence of genes for a phenotype */
    public void countPrivateGeneForEachNode( HashMap<String, HashSet<Integer>> phenotypesGenesAssociations ) {

        HashSet<Integer> allGenes = new HashSet<Integer>();

        for ( TreeCharacteristicValueObject tc : this.children ) {

            tc.countPrivateGeneForEachNode( phenotypesGenesAssociations );

            if ( phenotypesGenesAssociations.get( tc.getValueUri() ) != null ) {
                allGenes.addAll( phenotypesGenesAssociations.get( tc.getValueUri() ) );

                if ( phenotypesGenesAssociations.get( this.valueUri ) != null ) {
                    phenotypesGenesAssociations.get( this.valueUri ).addAll(
                            phenotypesGenesAssociations.get( tc.getValueUri() ) );
                } else {
                    HashSet<Integer> genesNBCI = new HashSet<Integer>();
                    genesNBCI.addAll( phenotypesGenesAssociations.get( tc.getValueUri() ) );
                    phenotypesGenesAssociations.put( this.valueUri, genesNBCI );
                }
            }
        }

        if ( phenotypesGenesAssociations.get( this.valueUri ) != null ) {
            allGenes.addAll( phenotypesGenesAssociations.get( this.valueUri ) );
        }
        this.privateGeneCount = allGenes.size();
    }

}
