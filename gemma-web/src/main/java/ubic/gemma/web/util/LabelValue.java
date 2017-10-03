/*
 * The Gemma project
 * 
 * Copyright (c) 2006 University of British Columbia
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
package ubic.gemma.web.util;

import java.io.Serializable;
import java.util.Comparator;

/**
 * A simple JavaBean to represent label-value pairs. This is most commonly used when constructing user interface
 * elements which have a label to be displayed to the user, and a corresponding value to be returned to the server. One
 * example is the <code>&lt;html:options&gt;</code> tag.
 * Note: this class has a natural ordering that is inconsistent with equals.
 */
public class LabelValue implements Comparable<LabelValue>, Serializable {

    /**
     * Comparator that can be used for a case insensitive sort of <code>LabelValue</code> objects.
     */
    public static final Comparator<LabelValue> CASE_INSENSITIVE_ORDER = new Comparator<LabelValue>() {
        @Override
        public int compare( LabelValue o1, LabelValue o2 ) {
            String label1 = o1.getLabel();
            String label2 = o2.getLabel();
            return label1.compareToIgnoreCase( label2 );
        }
    };
    private static final long serialVersionUID = 3485837885293359602L;
    /**
     * The property which supplies the option label visible to the end user.
     */
    private String label = null;

    /**
     * The property which supplies the value returned to the server.
     */
    private String value = null;

    /**
     * Default constructor.
     */
    public LabelValue() {
        super();
    }

    /**
     * Construct an instance with the supplied property values.
     *
     * @param label The label to be displayed to the user
     * @param value The value to be returned to the server
     */
    public LabelValue( String label, String value ) {
        this.label = label;
        this.value = value;
    }

    @Override
    public int compareTo( LabelValue o ) {
        // Implicitly tests for the correct type, throwing
        // ClassCastException as required by interface
        String otherLabel = o.getLabel();

        return this.getLabel().compareTo( otherLabel );
    }

    @Override
    public boolean equals( Object obj ) {
        if ( obj == this ) {
            return true;
        }

        if ( !( obj instanceof LabelValue ) ) {
            return false;
        }

        LabelValue bean = ( LabelValue ) obj;
        int nil = ( this.getValue() == null ) ? 1 : 0;
        nil += ( bean.getValue() == null ) ? 1 : 0;

        return nil == 2 || nil != 1 && this.getValue().equals( bean.getValue() );

    }

    public String getLabel() {
        return this.label;
    }

    public void setLabel( String label ) {
        this.label = label;
    }

    public String getValue() {
        return this.value;
    }

    public void setValue( String value ) {
        this.value = value;
    }

    @Override
    public int hashCode() {
        return ( this.getValue() == null ) ? 17 : this.getValue().hashCode();
    }

    @Override
    public String toString() {
        return ( "LabelValue[" + this.label + ", " + this.value + "]" );
    }
}