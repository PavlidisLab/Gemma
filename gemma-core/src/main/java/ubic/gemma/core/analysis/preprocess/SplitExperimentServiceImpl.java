/*
 * The gemma-core project
 * 
 * Copyright (c) 2018 University of British Columbia
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

package ubic.gemma.core.analysis.preprocess;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ubic.gemma.core.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.core.datastructure.matrix.ExpressionDataMatrix;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.common.measurement.Measurement;
import ubic.gemma.model.common.measurement.MeasurementKind;
import ubic.gemma.model.common.quantitationtype.PrimitiveType;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.bioAssayData.RawExpressionDataVector;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.biomaterial.Treatment;
import ubic.gemma.model.expression.experiment.ExperimentalDesign;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.persistence.persister.Persister;
import ubic.gemma.persistence.service.common.auditAndSecurity.CurationDetailsDao;
import ubic.gemma.persistence.service.expression.bioAssayData.RawExpressionDataVectorService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;

/**
 * 
 * Split an experiment into multiple experiments. This is needed when a load EE (e.g. from GEO) is better represented as
 * two more more distinct experiments. The decision of what to split is based on curation guidelines documented
 * elsewhere.
 * 
 * @author paul
 */
@Service
public class SplitExperimentServiceImpl implements SplitExperimentService {

    private static final Log log = LogFactory.getLog( SplitExperimentServiceImpl.class );

    @Autowired
    private PreprocessorService preprocessor;

    @Autowired
    private ExpressionExperimentService eeService;

    @Autowired
    private RawExpressionDataVectorService rawExpressionDataVectorService;

    @Autowired
    private CurationDetailsDao curationDetailsDao;

    @Autowired
    private Persister persister;

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService#split(ubic.gemma.model.
     * expression.experiment.ExpressionExperiment, ubic.gemma.model.expression.experiment.ExperimentalFactor)
     */
    @Override
    public Collection<ExpressionExperiment> split( ExpressionExperiment toSplit, ExperimentalFactor splitOn ) {

        toSplit = eeService.thawLite( toSplit );

        // Clean the experiment: remove diff and coex analyses, PCA, correlation matrices, processed data vectors
        // TODO

        Collection<ExpressionExperiment> result = new HashSet<>();

        String sourceShortName = toSplit.getShortName();

        if ( eeService.getArrayDesignsUsed( toSplit ).size() > 1 ) {
            throw new IllegalArgumentException( "Cannot split experiments that are on more than one platform" );
        }

        Collection<QuantitationType> qts = eeService.getQuantitationTypes( toSplit );

        // Get the expression data matrices for the experiment. We'll split them and generate new vectors
        Map<QuantitationType, ExpressionDataMatrix<?>> qt2mat = new HashMap<>();
        for ( QuantitationType qt : qts ) {
            if ( !qt.getRepresentation().equals( PrimitiveType.DOUBLE ) ) {
                throw new UnsupportedOperationException( "Non-double values currently not supported for experiment split" );
            }

            Collection<RawExpressionDataVector> vecs = rawExpressionDataVectorService.find( qt );
            rawExpressionDataVectorService.thaw( vecs );
            if ( vecs.isEmpty() ) {
                log.debug( "No vectors for " + qt ); // that's okay, e.g. processed data
                continue;
            }
            log.info( vecs.size() + " vectors for " + qt );

            qt2mat.put( qt, ExpressionDataMatrixBuilder.getMatrix( vecs ) );
        }

        // stub the new experiments and create new names; all other information should be retained. Permissions should be the same. 
        // Audit events should start over with a notated create event
        int i = 0;
        for ( FactorValue fv : splitOn.getFactorValues() ) {
            i++;
            ExpressionExperiment split = ExpressionExperiment.Factory.newInstance();
            split.setShortName( sourceShortName + "." + i );

            // copy everything but samples over
            split.setName( "Split part " + i + " " + " from " + toSplit.getName() );
            split.setDescription( "This experiment was created by Gemma splitting another: \n" + toSplit + toSplit.getDescription() );

            split.setCharacteristics( this.cloneCharacteristics( toSplit.getCharacteristics() ) ); // which might no longer be accurate...

            split.setCurationDetails( curationDetailsDao.create() ); // not sure anything we want to copy

            split.setMetadata( toSplit.getMetadata() ); // might no longer be accurate/relevant?

            split.setPrimaryPublication( toSplit.getPrimaryPublication() );
            split.getOtherRelevantPublications().addAll( toSplit.getOtherRelevantPublications() );
            split.setAccession( toSplit.getAccession() );

            split.setExperimentalDesign( this.cloneExperimentalDesign( toSplit.getExperimentalDesign() ) );

            // starting with a fresh audit trail, assuming that's the right thing to do.

            split.setOwner( toSplit.getOwner() );
            split.setSource( toSplit.getSource() );
            // split.setRelatedTo... keep track of this being related to other parts of the split (which might be more than 2 parts)

            // add the biomaterials
            List<BioMaterial> bms = new ArrayList<>();
            for ( BioAssay ba : toSplit.getBioAssays() ) {
                BioAssay newBa = this.cloneBioAssay( ba );

                BioMaterial bm = ba.getSampleUsed();
                bms.add( bm );
                for ( FactorValue fvs : bm.getFactorValues() ) {
                    if ( fvs.equals( fv ) ) { // FIXME this won't work since we have now cloned the factor values
                        split.getBioAssays().add( ba );
                        bm.getFactorValues().remove( fvs ); // remove the factor we are using to split on
                    }
                }

                split.getBioAssays().add( newBa );
            }

            for ( QuantitationType qt : qt2mat.keySet() ) {

                QuantitationType clonedQt = this.cloneQt( qt );
                split.getQuantitationTypes().add( clonedQt );

                // careful that these bms are same as the ones associated with the vectors, not the clones
                ExpressionDataDoubleMatrix expressionDataMatrix = new ExpressionDataDoubleMatrix( ( ExpressionDataDoubleMatrix ) qt2mat.get( qt ),
                        bms, makeBioAssayDimension( bms, toSplit ) );

                Collection<RawExpressionDataVector> rawDataVectors = expressionDataMatrix.toRawDataVectors();
                for ( RawExpressionDataVector v : rawDataVectors ) {
                    v.setQuantitationType( clonedQt );
                }

                split.getRawExpressionDataVectors().addAll( rawDataVectors );
            }

            split = ( ExpressionExperiment ) persister.persist( split );
            result.add( split );

            // postprocess
            try {
                preprocessor.process( split );
            } catch ( PreprocessingException e ) {
                log.error( "Failure while preprocessing: " + split, e );
            }
        }

        // delete the old experiment (maybe not yet... in case ...
        // eeService.remove(toSplit);

        return result;
    }

    /**
     * @param  experimentalDesign
     * @return
     */
    private ExperimentalDesign cloneExperimentalDesign( ExperimentalDesign experimentalDesign ) {
        ExperimentalDesign clone = ExperimentalDesign.Factory.newInstance();
        clone.setDescription( experimentalDesign.getDescription() );
        clone.setName( experimentalDesign.getName() );
        clone.setNormalizationDescription( experimentalDesign.getNormalizationDescription() );
        clone.setQualityControlDescription( experimentalDesign.getQualityControlDescription() );
        clone.setReplicateDescription( experimentalDesign.getReplicateDescription() );
        clone.setTypes( this.cloneCharacteristics( experimentalDesign.getTypes() ) );

        clone.getExperimentalFactors().addAll( this.cloneExperimentalFactors( experimentalDesign.getExperimentalFactors() ) );

        return clone;
    }

    /**
     * @param  experimentalFactors
     * @return
     */
    private Collection<ExperimentalFactor> cloneExperimentalFactors( Collection<ExperimentalFactor> experimentalFactors ) {
        Collection<ExperimentalFactor> result = new HashSet<>();
        for ( ExperimentalFactor ef : experimentalFactors ) {
            ExperimentalFactor clone = ExperimentalFactor.Factory.newInstance();
            clone.setAnnotations( this.cloneCharacteristics( ef.getAnnotations() ) );
            clone.setCategory( this.cloneCharacteristic( ef.getCategory() ) );
            clone.setName( ef.getName() );
            clone.setDescription( ef.getDescription() );
            clone.setType( ef.getType() );
            clone.getFactorValues().addAll( this.cloneFactorValues( ef.getFactorValues(), clone ) );
            result.add( clone );
        }
        return result;
    }

    /**
     * @param  factorValues
     * @return
     */
    private Collection<FactorValue> cloneFactorValues( Collection<FactorValue> factorValues, ExperimentalFactor ef ) {
        Collection<FactorValue> result = new HashSet<>();
        for ( FactorValue fv : factorValues ) {
            FactorValue clone = FactorValue.Factory.newInstance( ef );
            clone.setCharacteristics( this.cloneCharacteristics( fv.getCharacteristics() ) );
            clone.setIsBaseline( fv.getIsBaseline() );
            clone.setValue( fv.getValue() );
            clone.setMeasurement( this.cloneMeasurement( fv.getMeasurement() ) );
            result.add( fv );
        }

        return result;
    }

    /**
     * @param  measurement
     * @return
     */
    private Measurement cloneMeasurement( Measurement measurement ) {

        if ( measurement == null ) return null;
        Measurement clone = Measurement.Factory.newInstance();
        clone.setKindCV( measurement.getKindCV() );
        clone.setRepresentation( measurement.getRepresentation() );
        clone.setOtherKind( measurement.getOtherKind() );
        clone.setValue( measurement.getValue() );
        clone.setType( measurement.getType() );

        return clone;
    }

    /**
     * @param  qt
     * @return    peristent new copy of qt
     */
    private QuantitationType cloneQt( QuantitationType qt ) {
        QuantitationType result = QuantitationType.Factory.newInstance();
        result.setDescription( qt.getDescription() );
        result.setName( qt.getName() );
        result.setGeneralType( qt.getGeneralType() );
        result.setIsBackground( qt.getIsBackground() );
        result.setIsBackgroundSubtracted( qt.getIsBackgroundSubtracted() );
        result.setIsBatchCorrected( qt.getIsBatchCorrected() );
        result.setIsMaskedPreferred( qt.getIsMaskedPreferred() );
        result.setIsNormalized( qt.getIsNormalized() );
        result.setIsPreferred( qt.getIsPreferred() );
        result.setIsRatio( qt.getIsRatio() );
        result.setIsRecomputedFromRawData( qt.getIsRecomputedFromRawData() );
        result.setRepresentation( qt.getRepresentation() );
        result.setType( qt.getType() );
        result.setScale( qt.getScale() );

        return ( QuantitationType ) persister.persist( qt );
    }

    private Collection<Characteristic> cloneCharacteristics( Collection<Characteristic> ch ) {
        Collection<Characteristic> result = new HashSet<>();
        for ( Characteristic c : ch ) {
            Characteristic clone = cloneCharacteristic( c );

            result.add( ( Characteristic ) persister.persist( clone ) );

        }
        return result;
    }

    /**
     * @param  c
     * @return
     */
    private Characteristic cloneCharacteristic( Characteristic c ) {
        Characteristic clone = Characteristic.Factory.newInstance( c.getName(), c.getDescription(), c.getValue(), c.getValueUri(),
                c.getCategory(), c.getCategoryUri(), c.getEvidenceCode() );
        return clone;
    }

    /**
     * Deeply clone a bioAssay
     * 
     * @param  ba
     * @return
     */
    private BioAssay cloneBioAssay( BioAssay ba ) {
        BioAssay clone = BioAssay.Factory.newInstance();

        clone.setName( ba.getName() );
        clone.setArrayDesignUsed( ba.getArrayDesignUsed() );
        clone.setDescription( ba.getDescription() );
        clone.setMetadata( ba.getMetadata() );
        clone.setIsOutlier( ba.getIsOutlier() );
        clone.setOriginalPlatform( ba.getOriginalPlatform() );
        clone.setProcessingDate( ba.getProcessingDate() );
        clone.setSequencePairedReads( ba.getSequencePairedReads() );

        clone.setSequenceReadCount( ba.getSequenceReadCount() );
        clone.setSequenceReadLength( ba.getSequenceReadLength() );

        clone.setSampleUsed( this.cloneBioMaterial( ba.getSampleUsed(), ba ) );
        clone.setAccession( this.cloneAccession( ba.getAccession() ) );

        return clone;
    }

    /**
     * @param  accession
     * @return
     */
    private DatabaseEntry cloneAccession( DatabaseEntry de ) {
        if ( de == null ) return null;
        return DatabaseEntry.Factory.newInstance( de.getAccession(), de.getAccessionVersion(), de.getUri(),
                de.getExternalDatabase() );
    }

    /**
     * @param  sampleUsed
     * @return
     */
    private BioMaterial cloneBioMaterial( BioMaterial bm, BioAssay ba ) {
        BioMaterial clone = BioMaterial.Factory.newInstance();
        clone.setName( bm.getName() );
        clone.setDescription( bm.getDescription() );
        clone.setCharacteristics( this.cloneCharacteristics( bm.getCharacteristics() ) );
        clone.setExternalAccession( this.cloneAccession( bm.getExternalAccession() ) );
        clone.setSourceTaxon( bm.getSourceTaxon() );
        clone.setTreatments( this.cloneTreatments( bm.getTreatments() ) );
        clone.getBioAssaysUsedIn().add( ba );
        /*
         * Factorvalues are done separately
         */

        return clone;
    }

    /**
     * @param  treatments
     * @return
     */
    private Collection<Treatment> cloneTreatments( Collection<Treatment> ts ) {
        Collection<Treatment> result = new HashSet<>();
        for ( Treatment t : ts ) {
            Treatment clone = Treatment.Factory.newInstance();
            clone.setDescription( t.getDescription() );
            clone.setName( t.getName() );
            clone.setOrderApplied( t.getOrderApplied() );
            result.add( clone );
        }

        return result;
    }

    private BioAssayDimension makeBioAssayDimension( List<BioMaterial> samplesToUse, ExpressionExperiment ee ) {

        List<BioAssay> bioAssays = new ArrayList<>();
        for ( BioMaterial bm : samplesToUse ) {
            bioAssays.add( bm.getBioAssaysUsedIn().iterator().next() );
        }

        BioAssayDimension result = BioAssayDimension.Factory.newInstance();
        result.setBioAssays( bioAssays );
        result.setName( "" );
        result.setDescription( bioAssays.size() + " bioAssays extracted from source experiment " + ee.getShortName() );

        return result;
    }
}
