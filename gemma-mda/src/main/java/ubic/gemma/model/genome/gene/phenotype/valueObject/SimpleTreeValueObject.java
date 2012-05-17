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

// simple way to represent a phenotype for the tree, sending the minimal information
public class SimpleTreeValueObject implements Comparable<SimpleTreeValueObject> {

    private String _id = "";
    private String _parent = null;
    private boolean _is_leaf = false;
    private String value = "";
    protected String valueUri = "";
    protected long publicGeneCount = 0L;
    protected long privateGeneCount = 0L;
    private String urlId = "";

    public SimpleTreeValueObject( TreeCharacteristicValueObject treeCharacteristicValueObject ) {
        
        this.urlId = treeCharacteristicValueObject.getUrlId();
        this._id = treeCharacteristicValueObject.get_id();
        this._parent = treeCharacteristicValueObject.get_parent();
        this._is_leaf = treeCharacteristicValueObject.is_is_leaf();
        this.value = treeCharacteristicValueObject.getValue();
        this.valueUri = treeCharacteristicValueObject.getValueUri();
        this.publicGeneCount = treeCharacteristicValueObject.getPublicGeneCount();
        this.privateGeneCount = treeCharacteristicValueObject.getPrivateGeneCount();
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

    public long getPublicGeneCount() {
        return this.publicGeneCount;
    }

    public void setPublicGeneCount( long publicGeneCount ) {
        this.publicGeneCount = publicGeneCount;
    }

    public long getPrivateGeneCount() {
        return this.privateGeneCount;
    }

    public void setPrivateGeneCount( long privateGeneCount ) {
        this.privateGeneCount = privateGeneCount;
    }

    public String getUrlId() {
        return this.urlId;
    }

    public void setUrlId( String urlId ) {
        this.urlId = urlId;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( ( this.value == null ) ? 0 : this.value.hashCode() );
        result = prime * result + ( ( this.valueUri == null ) ? 0 : this.valueUri.hashCode() );
        return result;
    }

    @Override
    public boolean equals( Object obj ) {
        if ( this == obj ) return true;
        if ( obj == null ) return false;
        if ( getClass() != obj.getClass() ) return false;
        SimpleTreeValueObject other = ( SimpleTreeValueObject ) obj;
        if ( this.value == null ) {
            if ( other.value != null ) return false;
        } else if ( !this.value.equals( other.value ) ) return false;
        if ( this.valueUri == null ) {
            if ( other.valueUri != null ) return false;
        } else if ( !this.valueUri.equals( other.valueUri ) ) return false;
        return true;
    }

    @Override
    public int compareTo( SimpleTreeValueObject o ) {

        if ( !this.value.equalsIgnoreCase( o.value ) ) {
            return this.value.compareToIgnoreCase( o.value );
        }
        return this.valueUri.compareToIgnoreCase( o.valueUri );
    }

}
