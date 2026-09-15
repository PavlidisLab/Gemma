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
import org.springframework.lang.Nullable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import ubic.gemma.core.security.audit.Audited;
import ubic.gemma.core.security.audit.AuditedConditional;
import ubic.gemma.model.association.GOEvidenceCode;
import ubic.gemma.model.common.auditAndSecurity.eventType.ManualAnnotationEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.TagAddedEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.TagRemovedEvent;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.description.CharacteristicUtils;
import ubic.gemma.model.expression.experiment.Statement;
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
import java.util.function.Function;

/**
 * @author pavlidis
 * @author keshav
 * @see BioMaterialService
 */
@Service
public class BioMaterialServiceImpl extends AbstractVoEnabledService<BioMaterial, BioMaterialValueObject>
        implements BioMaterialService {

    private final BioMaterialDao bioMaterialDao;
    private final FactorValueDao factorValueDao;
    private final BioAssayDao bioAssayDao;
    private final ExperimentalFactorDao experimentalFactorDao;
    private final CharacteristicService characteristicService;
    private final BioMaterialReadService bioMaterialReadService;

    @Autowired
    public BioMaterialServiceImpl( BioMaterialDao bioMaterialDao, FactorValueDao factorValueDao,
            BioAssayDao bioAssayDao, ExperimentalFactorDao experimentalFactorDao,
            CharacteristicService characteristicService,
            BioMaterialReadService bioMaterialReadService ) {
        super( bioMaterialDao );
        this.bioMaterialDao = bioMaterialDao;
        this.factorValueDao = factorValueDao;
        this.bioAssayDao = bioAssayDao;
        this.experimentalFactorDao = experimentalFactorDao;
        this.characteristicService = characteristicService;
        this.bioMaterialReadService = bioMaterialReadService;
    }

    @Override
    public BioMaterial copy( BioMaterial bioMaterial ) {
        return bioMaterialReadService.copy( bioMaterial );
    }

    @Override
    public Collection<BioMaterial> findSubBioMaterials( BioMaterial bioMaterial, boolean direct ) {
        return bioMaterialReadService.findSubBioMaterials( bioMaterial, direct );
    }

    @Override
    public Collection<BioMaterial> findSiblings( BioMaterial bioMaterial ) {
        return bioMaterialReadService.findSiblings( bioMaterial );
    }

    @Override
    public Collection<BioMaterial> findByExperiment( ExpressionExperiment experiment ) {
        return bioMaterialReadService.findByExperiment( experiment );
    }

    @Override
    public Collection<BioMaterial> findByFactor( ExperimentalFactor experimentalFactor ) {
        return bioMaterialReadService.findByFactor( experimentalFactor );
    }

    @Override
    public <T extends Exception> BioMaterial loadAndThawOrFail( Long bmId, Function<String, T> exceptionSupplier, String message ) throws T {
        return bioMaterialReadService.loadAndThawOrFail( bmId, exceptionSupplier, message );
    }

    @Override
    public Map<BioMaterial, Map<BioAssay, ExpressionExperiment>> getExpressionExperiments( BioMaterial bm ) {
        return bioMaterialReadService.getExpressionExperiments( bm );
    }

    @Override
    public BioMaterial thaw( BioMaterial bioMaterial ) {
        return bioMaterialReadService.thaw( bioMaterial );
    }

    @Override
    public Collection<BioMaterial> thaw( Collection<BioMaterial> bioMaterials ) {
        return bioMaterialReadService.thaw( bioMaterials );
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
    @Transactional
    public void removeCharacteristics( BioMaterial bm, Collection<Characteristic> characteristicsToRemove ) {
        Assert.isTrue( characteristicsToRemove.stream().allMatch( c -> c.getId() != null ), "All characteristics must be persistent." );
        Assert.isTrue( bm.getCharacteristics().containsAll( characteristicsToRemove ) , "expected true");
        bm.getCharacteristics().removeAll( characteristicsToRemove );
        update( bm );
        characteristicService.remove( characteristicsToRemove );
    }

    @Override
    @Transactional
    @AuditedConditional( value = ManualAnnotationEvent.class,
            when = "#result > 0",
            messageSpel = "'Replaced sample annotations via API (' + #result + ' change(s)) on biomaterial ' + #bm.id" )
    public int updateAnnotations( ExpressionExperiment owner, BioMaterial bm, Collection<Characteristic> desired ) {
        Assert.notNull( owner, "Owner experiment must not be null." );
        Assert.notNull( bm, "Biomaterial must not be null." );
        Assert.notNull( desired, "Desired characteristic set must not be null (use an empty collection to clear)." );
        for ( Characteristic vc : desired ) {
            Assert.isTrue( StringUtils.isNotBlank( vc.getCategory() ), "Each desired characteristic must have a non-blank category." );
            Assert.isTrue( StringUtils.isNotBlank( vc.getValue() ), "Each desired characteristic must have a non-blank value." );
        }

        bm = Objects.requireNonNull( bioMaterialDao.load( bm.getId() ),
                String.format( "No BioMaterial with ID %d.", bm.getId() ) );

        Set<Characteristic> current = bm.getCharacteristics();
        List<Characteristic> toRemove = new ArrayList<>();
        List<Characteristic> toAdd = new ArrayList<>();

        // anything in current not represented in desired -> remove
        for ( Characteristic c : current ) {
            boolean keep = false;
            for ( Characteristic d : desired ) {
                if ( CharacteristicUtils.sameTag( c, d ) ) {
                    keep = true;
                    break;
                }
            }
            if ( !keep ) {
                toRemove.add( c );
            }
        }
        // anything in desired not already present -> add; a matched-but-present tag arriving with new
        // supporting evidence -> refresh the evidence in place (identity by sameTag is unchanged).
        int evidenceUpdates = 0;
        for ( Characteristic d : desired ) {
            Characteristic match = null;
            for ( Characteristic c : current ) {
                if ( CharacteristicUtils.sameTag( c, d ) ) {
                    match = c;
                    break;
                }
            }
            if ( match == null ) {
                toAdd.add( copyForAdd( d ) );
            } else if ( d.getSupportingEvidence() != null
                    && !Objects.equals( d.getSupportingEvidence(), match.getSupportingEvidence() ) ) {
                match.setSupportingEvidence( d.getSupportingEvidence() );
                evidenceUpdates++;
            }
        }

        if ( toRemove.isEmpty() && toAdd.isEmpty() && evidenceUpdates == 0 ) {
            BioMaterialServiceImpl.log.debug( "updateAnnotations: no change for biomaterial " + bm.getId() );
            return 0;
        }

        if ( !toRemove.isEmpty() ) {
            Assert.isTrue( toRemove.stream().allMatch( c -> c.getId() != null ), "All characteristics to remove must be persistent." );
            current.removeAll( toRemove );
        }
        if ( !toAdd.isEmpty() ) {
            current.addAll( toAdd );
        }
        update( bm );
        if ( !toRemove.isEmpty() ) {
            characteristicService.remove( toRemove );
        }

        BioMaterialServiceImpl.log.info( "updateAnnotations: biomaterial " + bm.getId() + " added=" + toAdd.size()
                + " removed=" + toRemove.size() + " evidenceUpdates=" + evidenceUpdates );
        // Audit event written on the owning experiment by @AuditedConditional (the aspect targets the
        // first Auditable argument, i.e. owner); the SpEL guard keeps the no-change branch silent.
        return toAdd.size() + toRemove.size() + evidenceUpdates;
    }

    @Override
    @Transactional
    @Audited( value = TagAddedEvent.class,
            messageSpel = "'Added tag ' + #vc.category + ' = ' + #vc.value + ' to biomaterial ' + #bm.id" )
    public Characteristic addAnnotation( ExpressionExperiment owner, BioMaterial bm, Characteristic vc ) {
        return doAddAnnotation( owner, bm, vc );
    }

    /**
     * Reason-carrying overload. A separate method rather than a parameter on the one above so every
     * existing caller keeps its signature; the two differ only in the audit note the aspect writes.
     * Both delegate to the same private body through a plain {@code this} call, which is not re-advised,
     * so one call still writes one event.
     */
    @Override
    @Transactional
    @Audited( value = TagAddedEvent.class,
            messageSpel = "'Added tag ' + #vc.category + ' = ' + #vc.value + ' to biomaterial ' + #bm.id + (#reason != null ? ' \u2014 ' + #reason : '')" )
    public Characteristic addAnnotation( ExpressionExperiment owner, BioMaterial bm, Characteristic vc,
            @Nullable String reason ) {
        return doAddAnnotation( owner, bm, vc );
    }

    private Characteristic doAddAnnotation( ExpressionExperiment owner, BioMaterial bm, Characteristic vc ) {
        Assert.notNull( owner, "Owner experiment must not be null." );
        Assert.notNull( vc, "Characteristic must not be null." );
        Assert.isTrue( StringUtils.isNotBlank( vc.getCategory() ), "Must provide a category" );
        Assert.isTrue( StringUtils.isNotBlank( vc.getValue() ), "Must provide a value" );
        bm = Objects.requireNonNull( bioMaterialDao.load( bm.getId() ),
                String.format( "No BioMaterial with ID %d.", bm.getId() ) );
        for ( Characteristic existing : bm.getCharacteristics() ) {
            if ( CharacteristicUtils.sameTag( existing, vc ) ) {
                throw new IllegalArgumentException( "An annotation with the same (category, value) already exists on biomaterial "
                        + bm.getId() + " (existing id=" + existing.getId() + ")." );
            }
        }
        if ( vc.getEvidenceCode() == null ) {
            vc.setEvidenceCode( GOEvidenceCode.IC ); // manually added characteristic
        }
        bm.getCharacteristics().add( vc );
        update( bm );
        return vc;
    }

    @Override
    @Transactional
    @AuditedConditional( value = TagRemovedEvent.class,
            when = "#result != null",
            messageSpel = "'Removed tag ' + #result.category + ' = ' + #result.value + ' from biomaterial ' + #bm.id" )
    @Nullable
    public Characteristic removeAnnotation( ExpressionExperiment owner, BioMaterial bm, Long annotationId ) {
        return doRemoveAnnotation( owner, bm, annotationId );
    }

    /** Reason-carrying overload; see {@link #addAnnotation(ExpressionExperiment, BioMaterial, Characteristic, String)}. */
    @Override
    @Transactional
    @AuditedConditional( value = TagRemovedEvent.class,
            when = "#result != null",
            messageSpel = "'Removed tag ' + #result.category + ' = ' + #result.value + ' from biomaterial ' + #bm.id + (#reason != null ? ' \u2014 ' + #reason : '')" )
    @Nullable
    public Characteristic removeAnnotation( ExpressionExperiment owner, BioMaterial bm, Long annotationId,
            @Nullable String reason ) {
        return doRemoveAnnotation( owner, bm, annotationId );
    }

    @Nullable
    private Characteristic doRemoveAnnotation( ExpressionExperiment owner, BioMaterial bm, Long annotationId ) {
        Assert.notNull( owner, "Owner experiment must not be null." );
        Assert.notNull( annotationId, "Annotation id must not be null." );
        bm = Objects.requireNonNull( bioMaterialDao.load( bm.getId() ),
                String.format( "No BioMaterial with ID %d.", bm.getId() ) );
        Characteristic target = null;
        for ( Characteristic c : bm.getCharacteristics() ) {
            if ( annotationId.equals( c.getId() ) ) {
                target = c;
                break;
            }
        }
        if ( target == null ) {
            return null;
        }
        bm.getCharacteristics().remove( target );
        update( bm );
        characteristicService.remove( Collections.singleton( target ) );
        return target;
    }

    /**
     * Build the row to persist for an added characteristic, preserving the {@link Statement} discriminator
     * and its predicate / object pair (a plain {@code Characteristic.Factory} would silently downgrade a
     * Statement and drop the S-P-O semantics). Mirrors the experiment-level add path.
     */
    private static Characteristic copyForAdd( Characteristic d ) {
        Characteristic fresh;
        if ( d instanceof Statement ) {
            Statement ds = ( Statement ) d;
            Statement fs = Statement.Factory.newInstance();
            fs.setCategory( ds.getCategory() );
            fs.setCategoryUri( ds.getCategoryUri() );
            fs.setSubject( ds.getSubject() );
            if ( ds.getSubjectUri() != null ) {
                fs.setSubjectUri( ds.getSubjectUri() );
            }
            fs.setPredicate( ds.getPredicate() );
            fs.setPredicateUri( ds.getPredicateUri() );
            fs.setObject( ds.getObject() );
            fs.setObjectUri( ds.getObjectUri() );
            fs.setSecondPredicate( ds.getSecondPredicate() );
            fs.setSecondPredicateUri( ds.getSecondPredicateUri() );
            fs.setSecondObject( ds.getSecondObject() );
            fs.setSecondObjectUri( ds.getSecondObjectUri() );
            fresh = fs;
        } else {
            fresh = Characteristic.Factory.newInstance();
            fresh.setCategory( d.getCategory() );
            fresh.setCategoryUri( d.getCategoryUri() );
            fresh.setValue( d.getValue() );
            fresh.setValueUri( d.getValueUri() );
        }
        fresh.setEvidenceCode( d.getEvidenceCode() != null ? d.getEvidenceCode() : GOEvidenceCode.IC );
        fresh.setSupportingEvidence( d.getSupportingEvidence() );
        return fresh;
    }

    private BioMaterial update( BioMaterialValueObject bmvo ) {
        BioMaterial bm = Objects.requireNonNull( this.load( bmvo.getId() ),
                String.format( "No BioMaterial with ID %d.", bmvo.getId() ) );

        Collection<FactorValue> updatedFactorValues = new HashSet<>();
        Map<String, String> factorIdToFactorValueId = bmvo.getFactorIdToFactorValueId(); // all of them.
        for ( Map.Entry<String, String> fEntry : factorIdToFactorValueId.entrySet() ) {
            String factorIdString = fEntry.getKey();
            String factorValueString = fEntry.getValue();

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
