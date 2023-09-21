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

import ubic.gemma.model.common.description.CharacteristicValueObject;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

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
        super();
    }

    public TreeCharacteristicValueObject( Long id, String value, String valueUri ) {
        super( id, value, "", valueUri, "" );
        this._id = this.getUrlId();
    }

    public TreeCharacteristicValueObject( Long id, String value, String valueUri,
            TreeSet<TreeCharacteristicValueObject> children ) {
        super( id, value, "", valueUri, "" );
        this.children = children;
        this._id = this.getUrlId();
    }

    public String get_id() {
        return this._id;
    }

    public void set_id( String _id ) {
        this._id = _id;
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
}
