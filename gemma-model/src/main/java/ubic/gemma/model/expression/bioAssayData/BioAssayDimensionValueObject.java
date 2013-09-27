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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignValueObject;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssay.BioAssayValueObject;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.biomaterial.BioMaterialValueObject;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.FactorType;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.model.expression.experiment.FactorValueValueObject;

/**
 * @author Paul
 * @version $Id$
 */
public class BioAssayDimensionValueObject {

    @Override
    public String toString() {
        return "BioAssayDimensionValueObject [" + ( id != null ? "id=" + id + ", " : "" ) + "isReordered="
                + isReordered + ", " + ( bioAssays != null ? "bioAssays=" + StringUtils.join( bioAssays, "," ) : "" )
                + "]";
    }

    private BioAssayDimension bioAssayDimension = null;

    private List<BioAssayValueObject> bioAssays = new ArrayList<BioAssayValueObject>();

    private String description;

    private Long id;

    private boolean isReordered = false;

    private boolean isSubset = false;

    private String name;

    /**
     * If this is a subset, or a padded, BioAssayDimensionValueObject, the sourceBioAssayDimension is the original.
     */
    private BioAssayDimensionValueObject sourceBioAssayDimension;

    /**
     * Do not use this constructor unless this represents a subset of a persistent BioAssayDimension.
     */
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

    public BioAssayDimensionValueObject deepCopy() {
        BioAssayDimensionValueObject result = new BioAssayDimensionValueObject();
        result.bioAssays.addAll( this.getBioAssays() );
        result.bioAssayDimension = this.bioAssayDimension;
        result.sourceBioAssayDimension = this.sourceBioAssayDimension;
        result.id = this.id;
        result.isSubset = this.isSubset;
        result.isReordered = this.isReordered;
        result.name = this.name;
        result.description = this.description;
        return result;
    }

    public List<BioAssayValueObject> getBioAssays() {
        return bioAssays;
    }

    public String getDescription() {
        return description;
    }

    /**
     * @return the represented BioAssayDimension. If this represents the dimension for an ExpressionExperimentSubSet,
     *         then the return will be a "fake" entity that has minimal information stored. This is a kludge to avoid
     *         having to make the DataMatrix code use valueobjects.
     */
    public BioAssayDimension getEntity() {
        if ( this.bioAssayDimension == null ) {
            if ( !isSubset ) throw new IllegalStateException( "Entity was not set, not allowed unless isSubset=true" );

            /*
             * Begin hack.
             */
            return makeDummyBioAssayDimension();
        }

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

    /**
     * Represents the original source. If this is reordered or a subset, the return value will <em>not</em> be.
     * 
     * @return
     */
    public BioAssayDimensionValueObject getSourceBioAssayDimension() {
        return sourceBioAssayDimension;
    }

    public boolean isReordered() {
        return isReordered;
    }

    /**
     * @param newOrdering
     */
    public void reorder( List<BioAssayValueObject> newOrdering ) {
        if ( isReordered ) throw new IllegalStateException( "You cannot reorder twice" );

        assert bioAssays != null;

        synchronized ( this.bioAssays ) {

            // make sure we have a backup. (might not be very important)
            if ( this.sourceBioAssayDimension == null && !this.isReordered && !this.isSubset ) {
                this.sourceBioAssayDimension = this.deepCopy();
            }

            assert newOrdering.size() == bioAssays.size();
            assert newOrdering.containsAll( bioAssays );

            this.bioAssays.clear();
            this.bioAssays.addAll( newOrdering );
            this.isReordered = true;

        }
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

    public void setSourceBioAssayDimension( BioAssayDimensionValueObject source ) {
        this.sourceBioAssayDimension = source;
    }

    /**
     * @return
     */
    private BioAssayDimension makeDummyBioAssayDimension() {
        assert this.id == null;

        BioAssayDimension fakebd = BioAssayDimension.Factory.newInstance( "Placeholder representing: " + name,
                description, new ArrayList<BioAssay>() );

        Map<Long, ExperimentalFactor> fakeefs = new HashMap<Long, ExperimentalFactor>();
        for ( BioAssayValueObject bav : this.bioAssays ) {
            BioAssay ba = BioAssay.Factory.newInstance();
            ba.setId( bav.getId() );
            ba.setName( bav.getName() );
            ba.setDescription( "Fake placeholder" );

            BioMaterial sampleUsed = BioMaterial.Factory.newInstance();
            BioMaterialValueObject bmvo = bav.getSample();
            assert bmvo != null;
            sampleUsed.setId( bmvo.getId() );
            sampleUsed.setName( bmvo.getName() );
            sampleUsed.setDescription( "Fake placeholder" );
            for ( FactorValueValueObject fvvo : bmvo.getFactorValueObjects() ) {
                FactorValue fv = FactorValue.Factory.newInstance();
                assert fvvo.getId() != null;
                fv.setId( fvvo.getId() );
                assert fvvo.getValue() != null;
                fv.setValue( fvvo.getValue() );
                Long efid = fvvo.getFactorId();

                ExperimentalFactor ef = null;
                if ( fakeefs.containsKey( efid ) ) {
                    ef = fakeefs.get( efid );
                } else {
                    ef = ExperimentalFactor.Factory.newInstance();
                    ef.setId( efid );
                    ef.setName( fvvo.getCategory() );
                    ef.setType( fvvo.isMeasurement() ? FactorType.CONTINUOUS : FactorType.CATEGORICAL );
                    fakeefs.put( efid, ef );
                }
                ef.getFactorValues().add( fv );
                fv.setExperimentalFactor( ef );
                sampleUsed.getFactorValues().add( fv );
            }
            ba.setSampleUsed( sampleUsed );

            ArrayDesign ad = ArrayDesign.Factory.newInstance();
            ArrayDesignValueObject advo = bav.getArrayDesign();
            assert advo != null;
            ad.setId( advo.getId() );
            ad.setShortName( advo.getShortName() );
            ad.setDescription( "Fake placeholder" );
            ba.setArrayDesignUsed( ad );

            fakebd.getBioAssays().add( ba );
        }

        return fakebd;
    }

}
