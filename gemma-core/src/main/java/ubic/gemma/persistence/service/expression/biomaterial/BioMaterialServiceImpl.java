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
package ubic.gemma.persistence.service.expression.biomaterial;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import ubic.gemma.model.association.GOEvidenceCode;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.measurement.Measurement;
import ubic.gemma.model.common.measurement.MeasurementType;
import ubic.gemma.model.common.quantitationtype.PrimitiveType;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.biomaterial.BioMaterialValueObject;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.persistence.service.AbstractService;
import ubic.gemma.persistence.service.AbstractVoEnabledService;
import ubic.gemma.persistence.service.common.description.CharacteristicService;
import ubic.gemma.persistence.service.expression.bioAssay.BioAssayDao;
import ubic.gemma.persistence.service.expression.experiment.ExperimentalFactorDao;
import ubic.gemma.persistence.service.expression.experiment.FactorValueDao;

import java.util.*;

/**
 * @author pavlidis
 * @author keshav
 * @see    BioMaterialService
 */
@Service
public class BioMaterialServiceImpl extends AbstractVoEnabledService<BioMaterial, BioMaterialValueObject>
        implements BioMaterialService {

    private final BioMaterialDao bioMaterialDao;
    private final FactorValueDao factorValueDao;
    private final BioAssayDao bioAssayDao;
    private final ExperimentalFactorDao experimentalFactorDao;
    @Autowired
    private CharacteristicService characteristicService;

    @Autowired
    public BioMaterialServiceImpl( BioMaterialDao bioMaterialDao, FactorValueDao factorValueDao,
            BioAssayDao bioAssayDao, ExperimentalFactorDao experimentalFactorDao ) {
        super( bioMaterialDao );
        this.bioMaterialDao = bioMaterialDao;
        this.factorValueDao = factorValueDao;
        this.bioAssayDao = bioAssayDao;
        this.experimentalFactorDao = experimentalFactorDao;
    }

    @Override
    public BioMaterial copy( BioMaterial bioMaterial ) {
        return this.bioMaterialDao.copy( bioMaterial );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<BioMaterial> findByExperiment( ExpressionExperiment experiment ) {
        return this.bioMaterialDao.findByExperiment( experiment );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<BioMaterial> findByFactorValue( FactorValue fv ) {
        return this.bioMaterialDao.findByFactorValue( fv );
    }

    @Override
    @Transactional(readOnly = true)
    public ExpressionExperiment getExpressionExperiment( Long id ) {
        return this.bioMaterialDao.getExpressionExperiment( id );
    }

    @Override
    @Transactional(readOnly = true)
    public BioMaterial thaw( BioMaterial bioMaterial ) {
        bioMaterial = ensureInSession( bioMaterial );
        this.bioMaterialDao.thaw( bioMaterial );
        return bioMaterial;
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<BioMaterial> thaw( Collection<BioMaterial> bioMaterials ) {
        bioMaterials = ensureInSession( bioMaterials );
        bioMaterials.forEach( this.bioMaterialDao::thaw );
        return bioMaterials;
    }

    @Override
    @Transactional
    public Collection<BioMaterial> updateBioMaterials( Collection<BioMaterialValueObject> valueObjects ) {

        Collection<BioMaterial> bms = new HashSet<>();
        for ( BioMaterialValueObject bioMaterialValueObject : valueObjects ) {
            BioMaterial updatedBm = this.update( bioMaterialValueObject );
            // the map FactorIdToFactorValueId contains values for all factors, including empty ones.
            assert bioMaterialValueObject.getFactorIdToFactorValueId().size() >= updatedBm.getFactorValues().size();
            bms.add( updatedBm );
        }
        return bms;
    }

    @Override
    @Transactional
    public <T> void associateBatchFactor( final Map<BioMaterial, T> descriptors, final Map<T, FactorValue> d2fv ) {

        for ( final BioMaterial bm : descriptors.keySet() ) {

            final BioMaterial toUpdate = Objects.requireNonNull( this.bioMaterialDao.load( bm.getId() ),
                    String.format( "No BioMaterial with ID %d.", bm.getId() ) );

            if ( !descriptors.containsKey( bm ) ) {
                throw new IllegalStateException( "Descriptor not provided for " + bm );
            }

            T descriptor = descriptors.get( toUpdate );
            // For RNA-seq, the descriptor is a fastq header string (possibly multi-line) associated with a specific sample. For microarrays, it is a date.
            if ( !d2fv.isEmpty() ) {
                FactorValue factorValue = d2fv.get( descriptor );
                if ( factorValue == null ) throw new IllegalStateException( "No factor for " + descriptor );
                toUpdate.getFactorValues().add( factorValue );
            }

            if ( !descriptors.values().isEmpty() ) {

                // Only if we are getting dates as descriptors, otherwise the FASTQ header field should be filled in.
                if ( Date.class
                        .isAssignableFrom( descriptors.values().iterator().next().getClass() ) ) {
                    for ( final BioAssay ba : toUpdate.getBioAssaysUsedIn() ) {

                        if ( ba.getProcessingDate() != null ) {
                            if ( !ba.getProcessingDate().equals( descriptor ) ) {
                                ba.setProcessingDate( ( Date ) descriptor );
                                bioAssayDao.update( ba );
                            }

                        } else {
                            ba.setProcessingDate( ( Date ) descriptor );
                            bioAssayDao.update( ba );
                        }
                    }
                } else {
                    // in this case, we should already have populated the header field?
                }
            }
            bioMaterialDao.update( toUpdate );
        }

    }

    @Override
    public String getBioMaterialIdList( Collection<BioMaterial> bioMaterials ) {
        StringBuilder buf = new StringBuilder();
        for ( BioMaterial bm : bioMaterials ) {
            buf.append( bm.getId() );
            buf.append( "," );
        }
        return buf.toString().replaceAll( ",$", "" );

    }

    @Override
    @Transactional
    public void addCharacteristic( BioMaterial bm, Characteristic vc ) {
        BioMaterialServiceImpl.log.debug( "Vocab Characteristic: " + vc );

        vc.setEvidenceCode( GOEvidenceCode.IC ); // manually added characteristic
        Set<Characteristic> chars = new HashSet<>();
        chars.add( vc );

        Set<Characteristic> current = bm.getCharacteristics();
        if ( current == null )
            current = new HashSet<>( chars );
        else
            current.addAll( chars );

        for ( Characteristic characteristic : chars ) {
            BioMaterialServiceImpl.log.info( "Adding characteristic to " + bm + " : " + characteristic );
        }

        bm.setCharacteristics( current );
        update( bm );
    }

    @Override
    public void removeCharacteristic( BioMaterial bm, Characteristic characterId ) {
        Assert.notNull( characterId.getId(), "The characteristic must be persistent." );
        if ( !bm.getCharacteristics().remove( characterId ) ) {
            throw new IllegalArgumentException( String.format( "%s does not belong to %s", characterId, bm ) );
        }
        characteristicService.remove( characterId );
    }

    private BioMaterial update( BioMaterialValueObject bmvo ) {
        BioMaterial bm = Objects.requireNonNull( this.load( bmvo.getId() ),
                String.format( "No BioMaterial with ID %d.", bmvo.getId() ) );

        Collection<FactorValue> updatedFactorValues = new HashSet<>();
        Map<String, String> factorIdToFactorValueId = bmvo.getFactorIdToFactorValueId(); // all of them.
        for ( String factorIdString : factorIdToFactorValueId.keySet() ) {
            String factorValueString = factorIdToFactorValueId.get( factorIdString );

            assert factorIdString.matches( "factor\\d+" );
            Long factorId = Long.parseLong( factorIdString.substring( 6 ) );

            //noinspection StatementWithEmptyBody // no value provided, that's okay, the curator can fill it in later.
            if ( StringUtils.isBlank( factorValueString ) ) {
            } else if ( factorValueString.matches( "fv\\d+" ) ) {
                // categorical
                long fvId = Long.parseLong( factorValueString.substring( 2 ) );
                FactorValue fv = factorValueDao.load( fvId );
                if ( fv == null ) {
                    throw new RuntimeException( "No such factorValue with id=" + fvId );
                }
                updatedFactorValues.add( fv );
            } else {
                // continuous, the value send is the actual value, not an id. This will only make sense if the value is
                // a measurement.
                boolean found = false;

                // find the right factor value to update.
                for ( FactorValue fv : bm.getFactorValues() ) {
                    if ( fv.getExperimentalFactor().getId().equals( factorId ) ) {
                        if ( fv.getMeasurement() == null ) {
                            throw new IllegalStateException(
                                    "Should have been a measurement associated with fv=" + fv + ", cannot update." );
                        } else if ( !fv.getMeasurement().getValue().equals( factorValueString ) ) {
                            AbstractService.log
                                    .debug( "Updating continuous value on biomaterial:" + bmvo + ", factor=" + fv
                                            .getExperimentalFactor() + " value= '" + factorValueString + "'" );
                            fv.getMeasurement().setValue( factorValueString );
                        } else {
                            AbstractService.log.debug( "Value unchanged from " + fv.getMeasurement().getValue() );
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
                    ExperimentalFactor ef = Objects.requireNonNull( experimentalFactorDao.load( factorId ),
                            String.format( "No ExperimentalFactor with ID %d.", factorId ) );

                    // note that this type of factorvalues are not reused for continuous ones.
                    AbstractService.log
                            .info( "Adding factor value for " + ef + ": " + factorValueString + " to " + bm );

                    FactorValue fv = FactorValue.Factory.newInstance();
                    fv.setExperimentalFactor( ef );
                    fv.setValue( factorValueString );
                    Measurement m = Measurement.Factory.newInstance();
                    m.setType( MeasurementType.ABSOLUTE );
                    m.setValue( fv.getValue() );
                    try {
                        //noinspection ResultOfMethodCallIgnored // check if it is a number, don't need the value.
                        Double.parseDouble( fv.getValue() );
                        m.setRepresentation( PrimitiveType.DOUBLE );
                    } catch ( NumberFormatException e ) {
                        m.setRepresentation( PrimitiveType.STRING );
                    }

                    fv.setMeasurement( m );

                    fv = factorValueDao.create( fv );
                    updatedFactorValues.add( fv );
                    ef.getFactorValues().add( fv );
                    experimentalFactorDao.update( ef );

                }

            }
        }

        // this is not valid, because it's possible that we are removing a factor value.
        // assert bm.getFactorValues().size() <= updatedFactorValues.size();

        bm.getFactorValues().clear();
        bm.getFactorValues().addAll( updatedFactorValues );
        assert !bm.getFactorValues().isEmpty();
        this.update( bm );
        assert !bm.getFactorValues().isEmpty();
        return bm;
    }
}