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

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import gemma.gsec.SecurityService;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.core.analysis.preprocess.batcheffects.BatchInfoPopulationServiceImpl;
import ubic.gemma.core.analysis.service.ExpressionDataFileService;
import ubic.gemma.core.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.core.datastructure.matrix.ExpressionDataMatrix;
import ubic.gemma.model.analysis.expression.ExpressionExperimentSet;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.common.measurement.Measurement;
import ubic.gemma.model.common.quantitationtype.PrimitiveType;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
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
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentSetService;

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

    @Autowired
    private SecurityService securityService;

    @Autowired
    private ExpressionDataFileService dataFileService;

    @Autowired
    private ExpressionExperimentSetService expressionExperimentSetService;

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService#split(ubic.gemma.model.
     * expression.experiment.ExpressionExperiment, ubic.gemma.model.expression.experiment.ExperimentalFactor)
     */
    @Override
    public Collection<ExpressionExperiment> split( ExpressionExperiment toSplit, ExperimentalFactor splitOn, boolean postProcess ) {

        toSplit = eeService.thawLite( toSplit );

        if ( !toSplit.getOtherParts().isEmpty() ) {
            throw new IllegalArgumentException( "You cannot split an experiment that was already created by a split" );
        }

        if ( eeService.getArrayDesignsUsed( toSplit ).size() > 1 ) {
            throw new IllegalArgumentException( "Cannot split experiments that are on more than one platform" );
        }

        if ( BatchInfoPopulationServiceImpl.isBatchFactor( splitOn ) ) {
            throw new IllegalArgumentException( "Do not split experiments on 'batch'" );
        }

        Collection<ExpressionExperiment> result = new HashSet<>();

        String sourceShortName = toSplit.getShortName();

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
        int splitNumber = 0;

        for ( FactorValue splitValue : splitOn.getFactorValues() ) {
            splitNumber++;
            ExpressionExperiment split = ExpressionExperiment.Factory.newInstance();
            split.setShortName( sourceShortName + "." + splitNumber );

            // copy everything but samples over
            String factorValueString = splitValue.getValue();
            if ( StringUtils.isBlank( factorValueString ) ) {
                factorValueString = splitValue.getDescriptiveString();
            }
            split.setName( "Split part " + splitNumber + " of: " + toSplit.getName() + " ["
                    + splitValue.getExperimentalFactor().getCategory().getValue() + " = "
                    + factorValueString + "]" );
            split.setDescription( "This experiment was created by Gemma splitting another: \n" + toSplit + toSplit.getDescription() );

            split.setCharacteristics( this.cloneCharacteristics( toSplit.getCharacteristics() ) );
            split.setCurationDetails( curationDetailsDao.create() ); // not sure anything we want to copy
            split.setMetadata( toSplit.getMetadata() ); // 
            split.setPrimaryPublication( toSplit.getPrimaryPublication() );
            split.getOtherRelevantPublications().addAll( toSplit.getOtherRelevantPublications() );
            split.setAccession( this.cloneAccession( toSplit.getAccession() ) ); // accession is currently unique, so have to clone
            split.setOwner( toSplit.getOwner() );
            split.setSource( toSplit.getSource() );
            split.setTaxon( toSplit.getTaxon() );
            // starting with a fresh audit trail.

            Map<FactorValue, FactorValue> old2cloneFV = new HashMap<>();
            split.setExperimentalDesign( this.cloneExperimentalDesign( toSplit.getExperimentalDesign(), old2cloneFV ) );

            // add the biomaterials
            Map<BioAssay, BioAssay> old2cloneBA = new HashMap<>();
            List<BioMaterial> bms = new ArrayList<>();
            Collection<FactorValue> usedFactorValues = new HashSet<>();
            for ( BioAssay ba : toSplit.getBioAssays() ) {
                boolean kept = false;
                BioMaterial bm = ba.getSampleUsed();

                // identify samples we want to include
                for ( FactorValue fv : bm.getFactorValues() ) {
                    if ( fv.equals( splitValue ) ) {
                        assert !bms.contains( bm );
                        bms.add( bm );
                        BioAssay newBa = this.cloneBioAssay( ba );
                        old2cloneBA.put( ba, newBa );
                        kept = true;
                    }
                }

                if ( kept ) {
                    // copy other factor values over
                    BioAssay newBa = old2cloneBA.get( ba );
                    for ( FactorValue fv : bm.getFactorValues() ) {
                        if ( fv.equals( splitValue ) ) {
                            // make a BioMaterial characteristic so we don't lose the information (might be redundant)
                            for ( Characteristic c : fv.getCharacteristics() ) {
                                newBa.getSampleUsed().getCharacteristics().add( this.cloneCharacteristic( c ) );
                            }
                            continue;
                        }
                        newBa.getSampleUsed().getFactorValues().add( old2cloneFV.get( fv ) );
                        usedFactorValues.add( old2cloneFV.get( fv ) );
                    }
                }
            }

            // remove unused factorvalues from the design
            Collection<ExperimentalFactor> toRemoveFactors = new HashSet<>();
            for ( ExperimentalFactor ef : split.getExperimentalDesign().getExperimentalFactors() ) {
                Collection<FactorValue> toRemove = new HashSet<>();
                for ( FactorValue fv : ef.getFactorValues() ) { // these are clones
                    if ( !usedFactorValues.contains( fv ) ) {
                        toRemove.add( fv );
                    }
                }

                if ( ef.getFactorValues().removeAll( toRemove ) ) {
                    log.info( toRemove.size() + " unused factor values removed for " + ef + " in split " + splitNumber );
                }

                if ( ef.getFactorValues().isEmpty() ) {
                    toRemoveFactors.add( ef );
                }
            }
            // remove unused factors
            if ( split.getExperimentalDesign().getExperimentalFactors().removeAll( toRemoveFactors ) ) {
                log.info( toRemoveFactors.size() + " unused experimental factors dropped from split " + splitNumber );
            }

            // here we're using the original bms; we'll replace them later
            BioAssayDimension newBAD = makeBioAssayDimension( bms, toSplit );

            for ( QuantitationType qt : qt2mat.keySet() ) {

                QuantitationType clonedQt = this.cloneQt( qt, split );

                split.getQuantitationTypes().add( clonedQt );

                // these bms are same as the ones associated with the vectors, not the clones
                ExpressionDataDoubleMatrix expressionDataMatrix = new ExpressionDataDoubleMatrix( ( ExpressionDataDoubleMatrix ) qt2mat.get( qt ),
                        bms, newBAD );

                Collection<RawExpressionDataVector> rawDataVectors = expressionDataMatrix.toRawDataVectors();
                for ( RawExpressionDataVector v : rawDataVectors ) {
                    v.setQuantitationType( clonedQt );
                    v.setExpressionExperiment( split );
                    assert v.getBioAssayDimension().equals( newBAD );
                    assert v.getDesignElement() != null;
                    assert v.getDesignElement().getArrayDesign() != null;
                    assert v.getDesignElement().getArrayDesign().getId() != null;
                }
                log.info( split.getShortName() + ": Adding " + rawDataVectors.size() + " raw data vectors for " + clonedQt + " preferred="
                        + clonedQt.getIsPreferred() );
                split.getRawExpressionDataVectors().addAll( rawDataVectors );
            }

            // now replace the bms in the newBAD with the clones
            List<BioAssay> badBAs = newBAD.getBioAssays();
            List<BioAssay> replaceBAs = new ArrayList<>();
            for ( BioAssay ba : badBAs ) {
                BioAssay clonedBA = old2cloneBA.get( ba );
                assert clonedBA != null;
                assert clonedBA.getSampleUsed().getId() == null;
                replaceBAs.add( clonedBA );
            }
            newBAD.getBioAssays().clear();
            newBAD.getBioAssays().addAll( replaceBAs );
            assert replaceBAs.size() == badBAs.size();

            split.getBioAssays().clear();
            split.getBioAssays().addAll( replaceBAs );
            split.setNumberOfSamples( replaceBAs.size() );

            split = ( ExpressionExperiment ) persister.persist( split );

            // securityService.makePublic( split ); // temporary 
            result.add( split );

            // postprocess. One problem can be that now we may have batches that are singletons etc.
            if ( postProcess ) {
                try {
                    preprocessor.process( split );
                } catch ( PreprocessingException e ) {
                    log.error( "Failure while preprocessing: " + split, e );
                }
            }
        }

        enforceOtherParts( toSplit, result );

        for ( ExpressionExperiment split : result ) {
            eeService.update( split );
        }

        /*
         * Create a new "experiment set" that groups them together (not sure we'll keep this) Do we make the "source"
         * part of this set?
         */
        ExpressionExperimentSet g = ExpressionExperimentSet.Factory.newInstance();
        g.setDescription( "Parts of " + toSplit.getShortName() + " that were split on " + splitOn.getName() );
        g.setName( toSplit.getShortName() + " splits" );
        g.setTaxon( toSplit.getBioAssays().iterator().next().getSampleUsed().getSourceTaxon() );
        g.getExperiments().addAll( result );
        this.expressionExperimentSetService.create( g );

        //   securityService.makePublic( g ); // at some point this would happen

        // FIXME
        // remove useless data files
        dataFileService.deleteAllFiles( toSplit );
        // Clean the source experiment? remove diff and coex analyses, PCA, correlation matrices, processed data vectors
        // delete it?
        // eeService.remove(toSplit);
        // OR perhaps only
        securityService.makePrivate( toSplit );
        // Or mark it as troubled?

        return result;
    }

    @Transactional
    void enforceOtherParts( ExpressionExperiment toSplit, Collection<ExpressionExperiment> result ) {
        // Enforce relation to other parts of the split.
        for ( ExpressionExperiment split : result ) {
            for ( ExpressionExperiment split2 : result ) {
                if ( split.equals( split2 ) ) continue;
                split.getOtherParts().add( split2 );
            }
        }
    }

    /**
     * @param  experimentalDesign
     * @param  old2cloneFV
     * @return                    non-persistent clone
     */
    private ExperimentalDesign cloneExperimentalDesign( ExperimentalDesign experimentalDesign, Map<FactorValue, FactorValue> old2cloneFV ) {
        ExperimentalDesign clone = ExperimentalDesign.Factory.newInstance();
        clone.setDescription( experimentalDesign.getDescription() );
        clone.setName( experimentalDesign.getName() );
        clone.setNormalizationDescription( experimentalDesign.getNormalizationDescription() );
        clone.setQualityControlDescription( experimentalDesign.getQualityControlDescription() );
        clone.setReplicateDescription( experimentalDesign.getReplicateDescription() );
        clone.setTypes( this.cloneCharacteristics( experimentalDesign.getTypes() ) );

        clone.getExperimentalFactors().addAll( this.cloneExperimentalFactors( experimentalDesign.getExperimentalFactors(), old2cloneFV ) );

        return clone;
    }

    /**
     * @param  experimentalFactors
     * @param  old2cloneFV
     * @return                     non-persistent clones
     */
    private Collection<ExperimentalFactor> cloneExperimentalFactors( Collection<ExperimentalFactor> experimentalFactors,
            Map<FactorValue, FactorValue> old2cloneFV ) {
        Collection<ExperimentalFactor> result = new HashSet<>();
        for ( ExperimentalFactor ef : experimentalFactors ) {
            ExperimentalFactor clone = ExperimentalFactor.Factory.newInstance();
            clone.setAnnotations( this.cloneCharacteristics( ef.getAnnotations() ) );
            clone.setCategory( this.cloneCharacteristic( ef.getCategory() ) );
            clone.setName( ef.getName() );
            clone.setDescription( ef.getDescription() );
            clone.setType( ef.getType() );
            clone.getFactorValues().addAll( this.cloneFactorValues( ef.getFactorValues(), clone, old2cloneFV ) );
            result.add( clone );
            //    assert clone.getId() == null;
        }
        return result;
    }

    /**
     * @param  factorValues
     * @param  old2cloneFV
     * @return              non-persistent clone
     */
    private Collection<FactorValue> cloneFactorValues( Collection<FactorValue> factorValues, ExperimentalFactor ef,
            Map<FactorValue, FactorValue> old2cloneFV ) {
        Collection<FactorValue> result = new HashSet<>();
        for ( FactorValue fv : factorValues ) {
            FactorValue clone = FactorValue.Factory.newInstance( ef );
            clone.setCharacteristics( this.cloneCharacteristics( fv.getCharacteristics() ) );
            clone.setIsBaseline( fv.getIsBaseline() );
            clone.setValue( fv.getValue() );
            clone.setMeasurement( this.cloneMeasurement( fv.getMeasurement() ) );
            result.add( clone );
            assert !old2cloneFV.containsKey( fv );
            old2cloneFV.put( fv, clone );
        }

        return result;
    }

    /**
     * @param  measurement
     * @return             non-persistent clone
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
     * @return    clone
     */
    private QuantitationType cloneQt( QuantitationType qt, ExpressionExperiment split ) {
        QuantitationType clone = QuantitationType.Factory.newInstance();
        clone.setDescription( qt.getDescription() + " (created for split: " + split.getShortName() + ")" );
        clone.setName( qt.getName() );
        clone.setGeneralType( qt.getGeneralType() );
        clone.setIsBackground( qt.getIsBackground() );
        clone.setIsBackgroundSubtracted( qt.getIsBackgroundSubtracted() );
        clone.setIsBatchCorrected( qt.getIsBatchCorrected() );
        clone.setIsMaskedPreferred( qt.getIsMaskedPreferred() );
        clone.setIsNormalized( qt.getIsNormalized() );
        clone.setIsPreferred( qt.getIsPreferred() );
        clone.setIsRatio( qt.getIsRatio() );
        clone.setIsRecomputedFromRawData( qt.getIsRecomputedFromRawData() );
        clone.setRepresentation( qt.getRepresentation() );
        clone.setType( qt.getType() );
        clone.setScale( qt.getScale() );

        return clone;
    }

    /**
     * @param  ch
     * @return    clones
     */
    private Collection<Characteristic> cloneCharacteristics( Collection<Characteristic> ch ) {
        Collection<Characteristic> result = new HashSet<>();
        for ( Characteristic c : ch ) {
            Characteristic clone = cloneCharacteristic( c );

            result.add( clone );

        }
        return result;
    }

    /**
     * @param  c
     * @return   clone
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
     * @return    non-persistent clone
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
     * @param  de databse entry
     * @return           non-persistent clone
     */
    private DatabaseEntry cloneAccession( DatabaseEntry de ) {
        if ( de == null ) return null;
        return DatabaseEntry.Factory.newInstance( de.getAccession(), de.getAccessionVersion(), de.getUri(),
                de.getExternalDatabase() );
    }

    /**
     * @param  bm biomaterial
     * @param ba bioassay
     * @return            non-persistent clone
     */
    private BioMaterial cloneBioMaterial( BioMaterial bm, BioAssay ba ) {
        BioMaterial clone = BioMaterial.Factory.newInstance();
        clone.setName( bm.getName() + " (Split)" ); // it is important we make a new name so we don't confuse this with the previous one in 'findorcreate()';
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
     * @return            non-persistent clones
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

    /**
     * 
     * @param  samplesToUse (cloned)
     * @param  ee
     * @return              non-persistent populated BAD
     */
    private BioAssayDimension makeBioAssayDimension( List<BioMaterial> samplesToUse, ExpressionExperiment ee ) {

        List<BioAssay> bioAssays = new ArrayList<>();
        for ( BioMaterial bm : samplesToUse ) {
            BioAssay ba = bm.getBioAssaysUsedIn().iterator().next();
            bioAssays.add( ba );
        }

        BioAssayDimension result = BioAssayDimension.Factory.newInstance();
        result.getBioAssays().addAll( bioAssays );
        result.setName( "For  " + bioAssays.size() + " bioAssays " );
        result.setDescription( bioAssays.size() + " bioAssays extracted via split from source experiment " + ee.getShortName() );

        assert result.getBioAssays().size() == samplesToUse.size();
        return result;
    }
}
