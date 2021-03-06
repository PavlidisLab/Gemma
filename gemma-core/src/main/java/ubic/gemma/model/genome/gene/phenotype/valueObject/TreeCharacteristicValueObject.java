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

import ubic.basecode.ontology.model.OntologyTerm;

import java.util.*;

/**
 * @author Paul
 */
@SuppressWarnings({ "unused", "WeakerAccess" }) // Possibly used in front end
public class TreeCharacteristicValueObject extends CharacteristicValueObject {

    private String _id = "";
    private TreeSet<TreeCharacteristicValueObject> children = new TreeSet<>();
    /**
     * phenotype present in the database
     */
    private boolean dbPhenotype = false;
    // all geneNBCI associated with the node or children from publicEvidence, used to write dump file ermineJ way
    private HashSet<Integer> publicGenesNBCI = new HashSet<>();
    // if we need to reconstruct part of the tree in the cache we need to know highest root parent
    private String rootOfTree = "";

    /**
     * Required when using the class as a spring bean.
     */
    public TreeCharacteristicValueObject() {
    }

    public TreeCharacteristicValueObject( Long id, String value, String valueUri ) {
        super( id, value, "", valueUri, "" );
        this._id = this.urlId;
    }

    public TreeCharacteristicValueObject( Long id, String value, String valueUri,
            TreeSet<TreeCharacteristicValueObject> children ) {
        super( id, value, "", valueUri, "" );
        this.children = children;
        this._id = this.urlId;
    }

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
                TreeCharacteristicValueObject tree = ontology2TreeCharacteristicValueObjects( ot,
                        phenotypeFoundInTree );
                phenotypeFoundInTree.put( tree.getValueUri(), tree );
                children.add( tree );
            }
        }

        return new TreeCharacteristicValueObject( -1L, ontologyTerm.getLabel(), ontologyTerm.getUri(), children );
    }

    /**
     * counts each private occurrence of genes for a phenotype
     *
     * @param phenotypesGenesAssociations map
     */
    public void countPrivateGeneForEachNode( Map<String, Set<Integer>> phenotypesGenesAssociations ) {

        Set<Integer> allGenes = new HashSet<>();

        for ( TreeCharacteristicValueObject tc : this.children ) {

            tc.countPrivateGeneForEachNode( phenotypesGenesAssociations );

            if ( phenotypesGenesAssociations.get( tc.getValueUri() ) != null ) {
                allGenes.addAll( phenotypesGenesAssociations.get( tc.getValueUri() ) );

                if ( phenotypesGenesAssociations.get( getValueUri() ) != null ) {
                    phenotypesGenesAssociations.get( getValueUri() )
                            .addAll( phenotypesGenesAssociations.get( tc.getValueUri() ) );
                } else {
                    HashSet<Integer> genesNBCI = new HashSet<>( phenotypesGenesAssociations.get( tc.getValueUri() ) );
                    phenotypesGenesAssociations.put( getValueUri(), genesNBCI );
                }
            }
        }

        if ( phenotypesGenesAssociations.get( getValueUri() ) != null ) {
            allGenes.addAll( phenotypesGenesAssociations.get( getValueUri() ) );
        }
        this.setPrivateGeneCount( allGenes.size() );
    }

    /**
     * counts each public occurrence of genes for a phenotype
     *
     * @param phenotypesGenesAssociations map
     */
    public void countPublicGeneForEachNode( Map<String, Set<Integer>> phenotypesGenesAssociations ) {

        for ( TreeCharacteristicValueObject tc : this.children ) {

            tc.countPublicGeneForEachNode( phenotypesGenesAssociations );

            if ( phenotypesGenesAssociations.get( tc.getValueUri() ) != null ) {
                this.publicGenesNBCI.addAll( phenotypesGenesAssociations.get( tc.getValueUri() ) );

                if ( phenotypesGenesAssociations.get( getValueUri() ) != null ) {
                    phenotypesGenesAssociations.get( getValueUri() )
                            .addAll( phenotypesGenesAssociations.get( tc.getValueUri() ) );
                } else {
                    Set<Integer> genesNBCI = new HashSet<>( phenotypesGenesAssociations.get( tc.getValueUri() ) );
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

    public void set_id( String _id ) {
        this._id = _id;
    }

    /**
     * @return all valueUri of children
     */
    public Collection<String> getAllChildrenUri() {

        Collection<String> childrenURI = new HashSet<>();

        findAllChildPhenotypeURI( childrenURI );

        return childrenURI;
    }

    public Collection<TreeCharacteristicValueObject> getChildren() {
        return this.children;
    }

    public void setChildren( TreeSet<TreeCharacteristicValueObject> children ) {
        this.children = children;
    }

    public Set<Integer> getPublicGenesNBCI() {
        return publicGenesNBCI;
    }

    public void setPublicGenesNBCI( HashSet<Integer> publicGenesNBCI ) {
        this.publicGenesNBCI = publicGenesNBCI;
    }

    public String getRootOfTree() {
        return this.rootOfTree;
    }

    public void setRootOfTree( String rootOfTree ) {
        this.rootOfTree = rootOfTree;
    }

    public boolean isDbPhenotype() {
        return this.dbPhenotype;
    }

    public void setDbPhenotype( boolean dbPhenotype ) {
        this.dbPhenotype = dbPhenotype;
    }

    /**
     * the tree is built with many terms in the Ontology, this method removes all nodes not found in the database
     */
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

    @Override
    public String toString() {
        return toString( 0 );
    }

    public String toString( int level ) {

        StringBuilder output = new StringBuilder();

        for ( int i = 0; i < level; i++ ) {
            output.append( "    " );
        }

        output.append( getValue() ).append( " " ).append( getPublicGeneCount() ).append( " (" )
                .append( getPrivateGeneCount() ).append( ")\n" );

        int currentLevel = level + 1;
        for ( TreeCharacteristicValueObject treeVO : this.children ) {
            output.append( treeVO.toString( currentLevel ) );
        }

        return output.toString();
    }

    /**
     * step into the tree and keep tracks of all valueURI
     *
     * @param phenotypesToFind phenotypes
     */
    private void findAllChildPhenotypeURI( Collection<String> phenotypesToFind ) {

        phenotypesToFind.add( this.getValueUri() );

        for ( TreeCharacteristicValueObject tree : this.getChildren() ) {
            tree.findAllChildPhenotypeURI( phenotypesToFind );
        }
    }

}
