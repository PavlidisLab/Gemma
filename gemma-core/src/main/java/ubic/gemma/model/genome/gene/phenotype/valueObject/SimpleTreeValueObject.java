/*
 * The Gemma project
 *
 * Copyright (c) 2011 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package ubic.gemma.model.genome.gene.phenotype.valueObject;

import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.util.List;
import java.util.Vector;

// simple way to represent a phenotype for the tree, sending the minimal information
@SuppressWarnings("unused") // Possibly used in front end
public class SimpleTreeValueObject implements Comparable<SimpleTreeValueObject>, Serializable {

    // private String _id = "";
    private String _parent;
    private boolean _is_leaf;
    private String value;
    private String valueUri;
    private long publicGeneCount;
    private long privateGeneCount;
    private String urlId;
    private boolean dbPhenotype;
    private List<String> children = new Vector<>();

    public SimpleTreeValueObject() {
        super();
    }

    /**
     * @param treeCharacteristicValueObject tree characteristic VO
     * @param parent                        the unique ID of the parent (get_id())
     */
    public SimpleTreeValueObject( TreeCharacteristicValueObject treeCharacteristicValueObject, String parent ) {

        this.urlId = treeCharacteristicValueObject.getUrlId();
        // this._id = makeUniqueId( treeCharacteristicValueObject.get_id(), parent );
        this._is_leaf = treeCharacteristicValueObject.getChildren().isEmpty();
        this._parent = parent;
        this.value = treeCharacteristicValueObject.getValue();
        this.valueUri = treeCharacteristicValueObject.getValueUri();
        this.publicGeneCount = treeCharacteristicValueObject.getPublicGeneCount();
        this.privateGeneCount = treeCharacteristicValueObject.getPrivateGeneCount();
        this.dbPhenotype = treeCharacteristicValueObject.isDbPhenotype();

        for ( TreeCharacteristicValueObject child : treeCharacteristicValueObject.getChildren() ) {
            children.add( this.makeUniqueId( child.get_id(), this.get_id() ) );
        }

    }

    @Override
    public int compareTo( SimpleTreeValueObject o ) {

        if ( !this.value.equalsIgnoreCase( o.value ) ) {
            return this.value.compareToIgnoreCase( o.value );
        }
        if ( !this.valueUri.equalsIgnoreCase( o.valueUri ) ) {
            return this.valueUri.compareToIgnoreCase( o.valueUri );
        }

        if ( this._parent != null )
            return this._parent.compareToIgnoreCase( o._parent );

        return this.makeUniqueId( this.urlId, null ).compareTo( o.get_id() );
    }

    public String get_id() {
        return this.makeUniqueId( urlId, _parent );
    }

    /**
     * @param _id id
     */
    public void set_id( String _id ) {
        throw new UnsupportedOperationException( "Don't set this manually" );
        // this._id = _id;
    }

    public String get_parent() {
        return this._parent;
    }

    public void set_parent( String _parent ) {
        this._parent = _parent;
    }

    /**
     * @return ids of the children.
     */
    public List<String> getChildren() {
        return children;
    }

    /**
     * Not used directly in java, allow constructor to manage.
     *
     * @param children children
     */
    public void setChildren( List<String> children ) {
        this.children = children;
    }

    public long getPrivateGeneCount() {
        return this.privateGeneCount;
    }

    public void setPrivateGeneCount( long privateGeneCount ) {
        this.privateGeneCount = privateGeneCount;
    }

    public long getPublicGeneCount() {
        return this.publicGeneCount;
    }

    public void setPublicGeneCount( long publicGeneCount ) {
        this.publicGeneCount = publicGeneCount;
    }

    public String getUrlId() {
        return this.urlId;
    }

    public void setUrlId( String urlId ) {
        this.urlId = urlId;
    }

    public String getValue() {
        return this.value;
    }

    public void setValue( String value ) {
        this.value = value;
    }

    public String getValueUri() {
        return this.valueUri;
    }

    public void setValueUri( String valueUri ) {
        this.valueUri = valueUri;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( ( this._parent == null ) ? 0 : this._parent.hashCode() );
        result = prime * result + ( ( this.value == null ) ? 0 : this.value.hashCode() );
        result = prime * result + ( ( this.valueUri == null ) ? 0 : this.valueUri.hashCode() );
        return result;
    }

    @Override
    public boolean equals( Object obj ) {
        if ( this == obj )
            return true;
        if ( obj == null )
            return false;
        if ( this.getClass() != obj.getClass() )
            return false;
        SimpleTreeValueObject other = ( SimpleTreeValueObject ) obj;
        if ( this._parent == null ) {
            if ( other._parent != null )
                return false;
        } else if ( !this._parent.equals( other._parent ) )
            return false;
        if ( this.value == null ) {
            if ( other.value != null )
                return false;
        } else if ( !this.value.equals( other.value ) )
            return false;
        if ( this.valueUri == null ) {
            return other.valueUri == null;
        } else
            return this.valueUri.equals( other.valueUri );
    }

    public boolean is_is_leaf() {
        return this._is_leaf;
    }

    public void set_is_leaf( boolean _is_leaf ) {
        this._is_leaf = _is_leaf;
    }

    public boolean isDbPhenotype() {
        return this.dbPhenotype;
    }

    public void setDbPhenotype( boolean dbPhenotype ) {
        this.dbPhenotype = dbPhenotype;
    }

    private String makeUniqueId( String u, String p ) {
        if ( StringUtils.isBlank( p ) ) {
            return u;
        }
        return u + "<" + p;
    }

}
