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
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import ubic.basecode.ontology.model.OntologyTerm;

/**
 * TODO Document Me
 * 
 * @author Paul
 * @version $Id$
 */
public class TreeCharacteristicValueObject extends CharacteristicValueObject {

    /**
     * Ontology term to TreeCharacteristicValueObject
     * 
     * @param ontologyTerm
     * @param phenotypeFoundInTree
     * @return
     */
    public static TreeCharacteristicValueObject ontology2TreeCharacteristicValueObjects( OntologyTerm ontologyTerm,
            Map<String, TreeCharacteristicValueObject> phenotypeFoundInTree ) {

        Collection<OntologyTerm> directChildTerms = ontologyTerm.getChildren( true );

        TreeSet<TreeCharacteristicValueObject> children = new TreeSet<>();

        for ( OntologyTerm ot : directChildTerms ) {
            if ( phenotypeFoundInTree.containsKey( ot.getUri() ) ) {
                TreeCharacteristicValueObject child = phenotypeFoundInTree.get( ot.getUri() );
                children.add( child );

                // See bug 4102. Removing wreaks havoc and I cannot see why it would be necessary.
                // treesPhenotypes.remove( child );
            } else {
                TreeCharacteristicValueObject tree = ontology2TreeCharacteristicValueObjects( ot, phenotypeFoundInTree );
                phenotypeFoundInTree.put( tree.getValueUri(), tree );
                children.add( tree );
            }
        }

        TreeCharacteristicValueObject treeCharacteristicVO = new TreeCharacteristicValueObject(
                ontologyTerm.getLabel(), ontologyTerm.getUri(), children );

        return treeCharacteristicVO;
    }

    private String _id = "";

    private TreeSet<TreeCharacteristicValueObject> children = new TreeSet<>();

    /** phenotype present in the database */
    private boolean dbPhenotype = false;

    // all geneNBCI associtaed with the node or children from publicEvidence, used to write dump file ermineJ way
    private HashSet<Integer> publicGenesNBCI = new HashSet<Integer>();

    // if we need to reconstruct part of the tree in the cache we need to know highest root parent
    private String rootOfTree = "";

    public TreeCharacteristicValueObject( String value, String valueUri ) {
        super( value, "", valueUri, "" );
        this._id = this.urlId;
    }

    public TreeCharacteristicValueObject( String value, String valueUri, TreeSet<TreeCharacteristicValueObject> children ) {
        super( value, "", valueUri, "" );
        this.children = children;
        this._id = this.urlId;
    }

    /**
     * counts each private occurrence of genes for a phenotype
     */
    public void countPrivateGeneForEachNode( Map<String, Set<Integer>> phenotypesGenesAssociations ) {

        Set<Integer> allGenes = new HashSet<>();

        for ( TreeCharacteristicValueObject tc : this.children ) {

            tc.countPrivateGeneForEachNode( phenotypesGenesAssociations );

            if ( phenotypesGenesAssociations.get( tc.getValueUri() ) != null ) {
                allGenes.addAll( phenotypesGenesAssociations.get( tc.getValueUri() ) );

                if ( phenotypesGenesAssociations.get( getValueUri() ) != null ) {
                    phenotypesGenesAssociations.get( getValueUri() ).addAll(
                            phenotypesGenesAssociations.get( tc.getValueUri() ) );
                } else {
                    HashSet<Integer> genesNBCI = new HashSet<Integer>();
                    genesNBCI.addAll( phenotypesGenesAssociations.get( tc.getValueUri() ) );
                    phenotypesGenesAssociations.put( getValueUri(), genesNBCI );
                }
            }
        }

        if ( phenotypesGenesAssociations.get( getValueUri() ) != null ) {
            allGenes.addAll( phenotypesGenesAssociations.get( getValueUri() ) );
        }
        this.setPrivateGeneCount( allGenes.size() );
    }

    /** counts each public occurrence of genes for a phenotype */
    public void countPublicGeneForEachNode( Map<String, Set<Integer>> phenotypesGenesAssociations ) {

        for ( TreeCharacteristicValueObject tc : this.children ) {

            tc.countPublicGeneForEachNode( phenotypesGenesAssociations );

            if ( phenotypesGenesAssociations.get( tc.getValueUri() ) != null ) {
                this.publicGenesNBCI.addAll( phenotypesGenesAssociations.get( tc.getValueUri() ) );

                if ( phenotypesGenesAssociations.get( getValueUri() ) != null ) {
                    phenotypesGenesAssociations.get( getValueUri() ).addAll(
                            phenotypesGenesAssociations.get( tc.getValueUri() ) );
                } else {
                    Set<Integer> genesNBCI = new HashSet<>();
                    genesNBCI.addAll( phenotypesGenesAssociations.get( tc.getValueUri() ) );
                    phenotypesGenesAssociations.put( getValueUri(), genesNBCI );
                }
            }
        }

        if ( phenotypesGenesAssociations.get( getValueUri() ) != null ) {
            this.publicGenesNBCI.addAll( phenotypesGenesAssociations.get( getValueUri() ) );
        }

        this.setPublicGeneCount( this.publicGenesNBCI.size() );
    }

    public String get_id() {
        return this._id;
    }

    /** return all valueUri of children */
    public Collection<String> getAllChildrenUri() {

        Collection<String> childrenURI = new HashSet<String>();

        findAllChildPhenotypeURI( childrenURI );

        return childrenURI;
    }

    public Collection<TreeCharacteristicValueObject> getChildren() {
        return this.children;
    }

    public Set<Integer> getPublicGenesNBCI() {
        return publicGenesNBCI;
    }

    public String getRootOfTree() {
        return this.rootOfTree;
    }

    public boolean isDbPhenotype() {
        return this.dbPhenotype;
    }

    /** the tree is built with many terms in the Ontology, this method removes all nodes not found in the database */
    public void removeUnusedPhenotypes() {

        TreeSet<TreeCharacteristicValueObject> newChildren = new TreeSet<>();

        for ( TreeCharacteristicValueObject child : this.children ) {

            long count = child.getPrivateGeneCount() + child.getPublicGeneCount();

            if ( count != 0 ) {
                newChildren.add( child );
            }
        }

        this.children = newChildren;

        for ( TreeCharacteristicValueObject child : this.children ) {
            child.removeUnusedPhenotypes();
        }
    }

    public void set_id( String _id ) {
        this._id = _id;
    }

    public void setChildren( TreeSet<TreeCharacteristicValueObject> children ) {
        this.children = children;
    }

    public void setDbPhenotype( boolean dbPhenotype ) {
        this.dbPhenotype = dbPhenotype;
    }

    public void setPublicGenesNBCI( HashSet<Integer> publicGenesNBCI ) {
        this.publicGenesNBCI = publicGenesNBCI;
    }

    public void setRootOfTree( String rootOfTree ) {
        this.rootOfTree = rootOfTree;
    }

    @Override
    public String toString() {
        return toString( 0 );
    }

    public String toString( int level ) {

        String output = "";

        for ( int i = 0; i < level; i++ ) {
            output = output + "    ";
        }

        output = output + getValue() + " " + getPublicGeneCount() + " (" + getPrivateGeneCount() + ")\n";

        int currentLevel = level + 1;
        for ( TreeCharacteristicValueObject treeVO : this.children ) {
            output = output + treeVO.toString( currentLevel );
        }

        return output;
    }

    /** step into the tree and keep tracks of all valueURI */
    private void findAllChildPhenotypeURI( Collection<String> phenotypesToFind ) {

        phenotypesToFind.add( this.getValueUri() );

        for ( TreeCharacteristicValueObject tree : this.getChildren() ) {
            tree.findAllChildPhenotypeURI( phenotypesToFind );
        }
    }

}
