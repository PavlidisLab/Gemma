/*
 * The Gemma project
 * 
 * Copyright (c) 2013 University of British Columbia
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
package ubic.gemma.model.expression.bioAssayData;

import java.util.ArrayList;
import java.util.List;

import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssay.BioAssayValueObject;

/**
 * @author Paul
 * @version $Id$
 */
public class BioAssayDimensionValueObject {

    private BioAssayDimension bioAssayDimension = null;

    private List<BioAssayValueObject> bioAssays;

    private String description;

    private Long id;

    private boolean isSubset = false;

    private String name;

    public BioAssayDimensionValueObject() {
    }

    /**
     * @param entity to be converted
     */
    public BioAssayDimensionValueObject( BioAssayDimension entity ) {
        this.bioAssayDimension = entity;
        this.name = entity.getName();
        this.id = entity.getId();
        this.description = entity.getDescription();
        this.bioAssays = new ArrayList<BioAssayValueObject>();
        for ( BioAssay bv : entity.getBioAssays() ) {
            bioAssays.add( new BioAssayValueObject( bv ) );
        }
    }

    public List<BioAssayValueObject> getBioAssays() {
        return bioAssays;
    }

    public String getDescription() {
        return description;
    }

    public BioAssayDimension getEntity() {
        if ( this.bioAssayDimension == null ) throw new IllegalStateException( "Entity was not set" );
        return this.bioAssayDimension;
    }

    public Long getId() {
        return id;
    }

    public boolean getIsSubset() {
        return isSubset;
    }

    public String getName() {
        return name;
    }

    public void setBioAssays( List<BioAssayValueObject> bioAssays ) {
        this.bioAssays = bioAssays;
    }

    public void setDescription( String description ) {
        this.description = description;
    }

    public void setId( Long id ) {
        this.id = id;
    }

    public void setIsSubset( boolean isSubset ) {
        this.isSubset = isSubset;
    }

    public void setName( String name ) {
        this.name = name;
    }

}
