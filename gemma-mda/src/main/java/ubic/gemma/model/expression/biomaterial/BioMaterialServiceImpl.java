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
package ubic.gemma.model.expression.biomaterial;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import ubic.gemma.model.common.measurement.Measurement;
import ubic.gemma.model.common.measurement.MeasurementType;
import ubic.gemma.model.common.quantitationtype.PrimitiveType;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.web.util.EntityNotFoundException;

/**
 * @author pavlidis
 * @author keshav
 * @version $Id$
 * @see ubic.gemma.model.expression.biomaterial.BioMaterialService
 */
@Service
public class BioMaterialServiceImpl extends ubic.gemma.model.expression.biomaterial.BioMaterialServiceBase {

    private static Logger log = LoggerFactory.getLogger( BioMaterialServiceImpl.class );

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.expression.biomaterial.BioMaterialService#exists(ubic.gemma.model.expression.biomaterial.BioMaterial
     * )
     */
    @Override
    public boolean exists( BioMaterial bioMaterial ) {
        return this.getBioMaterialDao().find( bioMaterial ) != null;
    }

    @Override
    public Collection<BioMaterial> findByExperiment( ExpressionExperiment experiment ) {
        return this.getBioMaterialDao().findByExperiment( experiment );
    }

    @Override
    public Collection<BioMaterial> findByFactorValue( FactorValue fv ) {
        return this.getBioMaterialDao().findByFactorValue( fv );
    }

    @Override
    public ExpressionExperiment getExpressionExperiment( Long id ) {
        return this.getBioMaterialDao().getExpressionExperiment( id );
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.expression.biomaterial.BioMaterialService#thaw(ubic.gemma.model.expression.biomaterial.BioMaterial
     * )
     */
    @Override
    public void thaw( BioMaterial bioMaterial ) {
        this.getBioMaterialDao().thaw( bioMaterial );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.biomaterial.BioMaterialService#thaw(java.util.Collection)
     */
    @Override
    public Collection<BioMaterial> thaw( Collection<BioMaterial> bioMaterials ) {
        return this.getBioMaterialDao().thaw( bioMaterials );
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.expression.biomaterial.BioMaterialServiceBase#handleCopy(ubic.gemma.model.expression.biomaterial
     * .BioMaterial)
     */
    @Override
    protected BioMaterial handleCopy( BioMaterial bioMaterial ) {
        return this.getBioMaterialDao().copy( bioMaterial );
    }

    @Override
    protected Integer handleCountAll() {
        return this.getBioMaterialDao().countAll();
    }

    /**
     * @param bioMaterial
     * @return @
     */
    @Override
    protected BioMaterial handleCreate( BioMaterial bioMaterial ) {
        return this.getBioMaterialDao().create( bioMaterial );
    }

    /**
     * @see ubic.gemma.model.expression.biomaterial.BioMaterialService#findOrCreate(ubic.gemma.model.expression.biomaterial.BioMaterial)
     */
    @Override
    protected BioMaterial handleFindOrCreate( BioMaterial bioMaterial ) {
        return this.getBioMaterialDao().findOrCreate( bioMaterial );
    }

    /**
     * @see ubic.gemma.model.expression.biomaterial.BioMaterialService#findOrId(java.lang.Long)
     */
    @Override
    protected BioMaterial handleLoad( Long id ) {
        return this.getBioMaterialDao().load( id );
    }

    /**
     * @see ubic.gemma.model.expression.biomaterial.BioMaterialService#loadAll()
     */
    @Override
    protected Collection<BioMaterial> handleLoadAll() {
        return ( Collection<BioMaterial> ) this.getBioMaterialDao().loadAll();
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.biomaterial.BioMaterialServiceBase#handleLoadMultiple(java.util.Collection)
     */
    @Override
    protected Collection<BioMaterial> handleLoadMultiple( Collection<Long> ids ) {
        return this.getBioMaterialDao().load( ids );
    }

    /**
     * @see ubic.gemma.model.expression.biomaterial.BioMaterialService#remove(ubic.gemma.model.expression.biomaterial.BioMaterial)
     */
    @Override
    protected void handleRemove( BioMaterial bioMaterial ) {
        this.getBioMaterialDao().remove( bioMaterial );

    }

    /**
     * @see ubic.gemma.model.expression.biomaterial.BioMaterialService#saveBioMaterial(ubic.gemma.model.expression.biomaterial.BioMaterial)
     */
    protected void handleSaveBioMaterial( ubic.gemma.model.expression.biomaterial.BioMaterial bioMaterial ) {
        this.getBioMaterialDao().create( bioMaterial );
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.expression.biomaterial.BioMaterialServiceBase#handleUpdate(ubic.gemma.model.expression.biomaterial
     * .BioMaterial)
     */
    @Override
    protected void handleUpdate( BioMaterial bioMaterial ) {
        this.getBioMaterialDao().update( bioMaterial );
    }

    @Override
    public Collection<BioMaterial> updateBioMaterials( Collection<BioMaterialValueObject> valueObjects ) {

        Collection<BioMaterial> bms = new HashSet<BioMaterial>();
        for ( BioMaterialValueObject bioMaterialValueObject : valueObjects ) {
            bms.add( this.update( bioMaterialValueObject ) );
        }
        return bms;
    }

    private BioMaterial update( BioMaterialValueObject bmvo ) {
        BioMaterial bm = load( bmvo.getId() );

        Collection<FactorValue> updatedFactorValues = new HashSet<FactorValue>();
        Map<String, String> factorIdToFactorValueId = bmvo.getFactorIdToFactorValueId(); // all of them.
        for ( String factorIdString : factorIdToFactorValueId.keySet() ) {
            String factorValueString = factorIdToFactorValueId.get( factorIdString );

            assert factorIdString.matches( "factor\\d+" );
            Long factorId = Long.parseLong( factorIdString.substring( 6 ) );

            if ( StringUtils.isBlank( factorValueString ) ) {
                // no value provided, that's okay, the curator can fill it in later.
                continue;

            } else if ( factorValueString.matches( "fv\\d+" ) ) {
                // categorical
                long fvId = Long.parseLong( factorValueString.substring( 2 ) );
                FactorValue fv = factorValueDao.load( fvId );
                if ( fv == null ) {
                    throw new EntityNotFoundException( "No such factorValue with id=" + fvId );
                }
                updatedFactorValues.add( fv );
            } else {
                // continuous, the value send is the actual value, not an id. This will only make sense if the value is
                // a measurement.
                boolean found = false;
                // find the right factor value.
                for ( FactorValue fv : bm.getFactorValues() ) {
                    if ( fv.getExperimentalFactor().getId().equals( factorId ) ) {
                        if ( fv.getMeasurement() == null ) {
                            throw new IllegalStateException( "Should have been a measurement associated with fv=" + fv
                                    + ", cannot update." );
                        } else if ( !fv.getMeasurement().getValue().equals( factorValueString ) ) {
                            log.debug( "Updating continuous value on biomaterial:" + bmvo + ", factor="
                                    + fv.getExperimentalFactor() + " value= '" + factorValueString + "'" );
                            fv.getMeasurement().setValue( factorValueString );
                        } else {
                            log.debug( "Value unchanged from " + fv.getMeasurement().getValue() );
                        }

                        // always add...
                        updatedFactorValues.add( fv );
                        found = true;
                        break;
                    }
                }

                if ( !found ) {

                    /*
                     * Have to load the factor, create a factor value.
                     */

                    ExperimentalFactor ef = experimentalFactorDao.load( factorId );

                    FactorValue fv = FactorValue.Factory.newInstance();
                    fv.setExperimentalFactor( ef );
                    fv.setValue( factorValueString );
                    Measurement m = Measurement.Factory.newInstance();
                    m.setType( MeasurementType.ABSOLUTE );
                    m.setValue( fv.getValue() );
                    try {
                        Double.parseDouble( fv.getValue() ); // check if it is a number, don't need the value.
                        m.setRepresentation( PrimitiveType.DOUBLE );
                    } catch ( NumberFormatException e ) {
                        m.setRepresentation( PrimitiveType.STRING );
                    }

                    fv.setMeasurement( m );

                    fv = factorValueDao.create( fv );

                    ef.getFactorValues().add( fv );

                    experimentalFactorDao.update( ef );

                }

            }
        }

        // <= because we might have just added one.
        assert bm.getFactorValues().size() <= updatedFactorValues.size();

        bm.getFactorValues().clear();
        bm.getFactorValues().addAll( updatedFactorValues );
        update( bm );
        return bm;
    }
}