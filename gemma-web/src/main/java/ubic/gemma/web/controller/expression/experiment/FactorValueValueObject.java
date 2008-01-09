/*
 * The Gemma project
 * 
 * Copyright (c) 2008 University of British Columbia
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
package ubic.gemma.web.controller.expression.experiment;

import java.util.Iterator;

import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.description.VocabCharacteristic;
import ubic.gemma.model.expression.experiment.FactorValue;

/**
 * @author luke
 */
public class FactorValueValueObject {

    private long   charId;
    private long   factorValueId;
    private String factorValueString;
    private String category;
    private String categoryUri;
    private String value;
    private String valueUri;
    
    public FactorValueValueObject() {
    }

    public FactorValueValueObject( FactorValue value, Characteristic c ) {
        this.setCharId( c.getId() );
        this.setFactorValueId( value.getId() );
        this.setFactorValueString( getSummaryString( value ) );
        this.setCategory( c.getCategory() );
        this.setValue( c.getValue() );
        if ( c instanceof VocabCharacteristic ) {
            VocabCharacteristic vc = (VocabCharacteristic)c;
            this.setCategoryUri( vc.getCategoryUri() );
            this.setValueUri( vc.getValueUri() );
        }
    }

    private String getSummaryString( FactorValue value ) {
        StringBuffer buf = new StringBuffer();
        for ( Iterator<Characteristic> iter = value.getCharacteristics().iterator(); iter.hasNext(); ) {
            Characteristic c = iter.next();
            buf.append( c.getCategory() );
            buf.append( ": " );
            buf.append( c.getValue() == null ? "no value" : c.getValue() );
            if ( iter.hasNext() )
                buf.append( ", " );
        }
        return buf.length() > 0 ? buf.toString() : "no characteristics";
    }

    public long getCharId() {
        return charId;
    }

    public void setCharId( long charId ) {
        this.charId = charId;
    }
    
    public long getFactorValueId() {
        return factorValueId;
    }
    
    public void setFactorValueId( long factorValueId ) {
        this.factorValueId = factorValueId;
    }

    public String getFactorValueString() {
        return factorValueString;
    }

    public void setFactorValueString( String factorValueString ) {
        this.factorValueString = factorValueString;
    }
    
    public String getCategory() {
        return category;
    }
    
    public void setCategory( String category ) {
        this.category = category;
    }
    
    public String getCategoryUri() {
        return categoryUri;
    }
    
    public void setCategoryUri( String categoryUri ) {
        this.categoryUri = categoryUri;
    }
    
    public String getValue() {
        return value;
    }
    
    public void setValue( String value ) {
        this.value = value;
    }
    
    public String getValueUri() {
        return valueUri;
    }
    
    public void setValueUri( String valueUri ) {
        this.valueUri = valueUri;
    }
    
}
