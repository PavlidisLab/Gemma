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

import io.swagger.v3.oas.annotations.media.Schema;
import org.apache.commons.lang3.StringUtils;
import ubic.gemma.model.common.IdentifiableValueObject;
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

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * @author Paul
 */
@SuppressWarnings({ "unused", "WeakerAccess" }) // Used in frontend
public class BioAssayDimensionValueObject extends IdentifiableValueObject<BioAssayDimension> {

    private static final long serialVersionUID = -8686807689616396835L;
    @Deprecated
    @Schema(description = "This field is deprecated and scheduled for removal. Favour the quantitation type name.", deprecated = true)
    private String name;
    @Deprecated
    @Schema(description = "This field is deprecated and scheduled for removal. Favour the quantitation type description.", deprecated = true)
    private String description;
    private final List<BioAssayValueObject> bioAssays = new LinkedList<>();
    private boolean isReordered = false;
    private boolean isSubset = false;

    /**
     * If this is a subset, or a padded, BioAssayDimensionValueObject, the sourceBioAssayDimension is the original.
     */
    @Nullable
    private BioAssayDimensionValueObject sourceBioAssayDimension;

    /**
     * Required when using the class as a spring bean.
     */
    public BioAssayDimensionValueObject() {
        super();
    }

    /**
     * Do not use this constructor unless this represents a subset of a persistent BioAssayDimension.
     *
     * @param id id
     */
    public BioAssayDimensionValueObject( Long id ) {
        super( id );
    }

    /**
     * @param entity to be converted
     */
    public BioAssayDimensionValueObject( BioAssayDimension entity ) {
        super( entity );
        for ( BioAssay bv : entity.getBioAssays() ) {
            if ( bv == null ) {
                throw new IllegalArgumentException( "Null bioassay in " + entity );
            }
            bioAssays.add( new BioAssayValueObject( bv, false ) );
        }
    }

    @Override
    public String toString() {
        return "BioAssayDimensionValueObject [" + ( id != null ? "id=" + id + ", " : "" ) + "isReordered=" + isReordered
                + ", " + ( bioAssays != null ? "bioAssays=" + StringUtils.join( bioAssays, "," ) : "" ) + "]";
    }

    @Deprecated
    public String getName() {
        return name;
    }

    @Deprecated
    public void setName( String name ) {
        this.name = name;
    }

    @Deprecated
    public String getDescription() {
        return description;
    }

    @Deprecated
    public void setDescription( String description ) {
        this.description = description;
    }

    public List<BioAssayValueObject> getBioAssays() {
        return bioAssays;
    }

    public void clearBioAssays() {
        this.bioAssays.clear();
    }

    public void addBioAssays( List<BioAssayValueObject> bvos ) {
        this.bioAssays.addAll( bvos );
    }

    public boolean getIsSubset() {
        return isSubset;
    }

    public void setIsSubset( boolean isSubset ) {
        this.isSubset = isSubset;
    }

    /**
     * @return the original source. If this is reordered or a subset, the return value will <em>not</em> be.
     */
    public BioAssayDimensionValueObject getSourceBioAssayDimension() {
        return sourceBioAssayDimension;
    }

    public void setSourceBioAssayDimension( BioAssayDimensionValueObject source ) {
        this.sourceBioAssayDimension = source;
    }

    public boolean isReordered() {
        return isReordered;
    }

    public void reorder( List<BioAssayValueObject> newOrdering ) {
        if ( isReordered )
            throw new IllegalStateException( "You cannot reorder twice" );

        synchronized ( this.bioAssays ) {

            // make sure we have a backup. (might not be very important)
            if ( this.sourceBioAssayDimension == null && !this.isReordered && !this.isSubset ) {
                this.sourceBioAssayDimension = this.deepCopy();
            }

            if ( newOrdering.size() > bioAssays.size() ) {
                /*
                 * Means we have to pad?
                 */
                System.err.println( "New ordering is larger than vector" );
            }

            this.bioAssays.clear();
            this.bioAssays.addAll( newOrdering );
            this.isReordered = true;

        }
    }

    private BioAssayDimensionValueObject deepCopy() {
        BioAssayDimensionValueObject result = new BioAssayDimensionValueObject( this.id );
        result.bioAssays.addAll( this.bioAssays );
        result.sourceBioAssayDimension = this.sourceBioAssayDimension;
        result.isSubset = this.isSubset;
        result.isReordered = this.isReordered;
        result.name = this.name;
        result.description = this.description;
        return result;
    }

    private BioAssayDimension makeDummyBioAssayDimension() {
        assert this.id == null;

        BioAssayDimension fakeBd = BioAssayDimension.Factory.newInstance();

        Map<Long, ExperimentalFactor> fakeEfs = new HashMap<>();
        for ( BioAssayValueObject bav : this.bioAssays ) {
            BioAssay ba = BioAssay.Factory.newInstance();
            ba.setId( bav.getId() );
            ba.setName( bav.getName() );
            ba.setDescription( "Fake placeholder" );

            BioMaterial sampleUsed = BioMaterial.Factory.newInstance();
            BioMaterialValueObject bmVo = bav.getSample();
            assert bmVo != null;
            sampleUsed.setId( bmVo.getId() );
            sampleUsed.setName( bmVo.getName() );
            sampleUsed.setDescription( "Fake placeholder" );
            for ( IdentifiableValueObject iVo : bmVo.getFactorValueObjects() ) {
                FactorValueValueObject fvVo = ( FactorValueValueObject ) iVo;
                FactorValue fv = FactorValue.Factory.newInstance();
                assert fvVo.getId() != null;
                fv.setId( fvVo.getId() );
                assert fvVo.getValue() != null;
                fv.setValue( fvVo.getValue() );
                Long efId = fvVo.getFactorId();

                ExperimentalFactor ef;
                if ( fakeEfs.containsKey( efId ) ) {
                    ef = fakeEfs.get( efId );
                } else {
                    ef = ExperimentalFactor.Factory.newInstance();
                    ef.setId( efId );
                    ef.setName( fvVo.getCategory() );
                    ef.setType( fvVo.isMeasurement() ? FactorType.CONTINUOUS : FactorType.CATEGORICAL );
                    fakeEfs.put( efId, ef );
                }
                ef.getFactorValues().add( fv );
                fv.setExperimentalFactor( ef );
                sampleUsed.getFactorValues().add( fv );
            }
            ba.setSampleUsed( sampleUsed );

            ArrayDesign ad = ArrayDesign.Factory.newInstance();
            ArrayDesignValueObject adVo = bav.getArrayDesign();
            assert adVo != null;
            ad.setId( adVo.getId() );
            ad.setShortName( adVo.getShortName() );
            ad.setDescription( "Fake placeholder" );
            ba.setArrayDesignUsed( ad );

            fakeBd.getBioAssays().add( ba );
        }

        return fakeBd;
    }

}
