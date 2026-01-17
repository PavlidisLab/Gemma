package ubic.gemma.core.analysis.preprocess;


import lombok.Value;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import ubic.gemma.core.analysis.preprocess.slice.BulkDataSlicerUtils;
import ubic.gemma.core.analysis.singleCell.SingleCellSlicerUtils;
import ubic.gemma.model.analysis.expression.ExpressionExperimentSet;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.common.measurement.Measurement;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.*;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.biomaterial.Treatment;
import ubic.gemma.model.expression.experiment.*;
import ubic.gemma.persistence.persister.Persister;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentSetService;
import ubic.gemma.persistence.service.expression.experiment.SingleCellExpressionExperimentService;

import java.nio.charset.StandardCharsets;
import java.util.*;

import static ubic.gemma.core.util.StringUtils.abbreviateWithSuffix;

/**
 * Helper service for {@link SplitExperimentService} to perform the split part in a transaction.
 */
@CommonsLog
@Service
class SplitExperimentHelperService {

    @Autowired
    private ExpressionExperimentService eeService;
    @Autowired
    private SingleCellExpressionExperimentService singleCellExpressionExperimentService;
    @Autowired
    private ExpressionExperimentSetService expressionExperimentSetService;
    @Autowired
    private Persister persister;

    @Value
    static class ExperimentSplitResult {
        /**
         * Experiment set containing the resulting split experiments which are completely detached from the original 
         * experiment.
         */
        ExpressionExperimentSet experimentSet;
        /**
         * Indicate if a preferred set of raw data vectors were found in the original experiment. This is used to decide
         * if post-processing is possible.
         */
        boolean foundPreferred;
    }

    @Transactional
    public ExperimentSplitResult split( ExpressionExperiment toSplit, ExperimentalFactor splitOn ) {
        if ( !toSplit.getOtherParts().isEmpty() ) {
            throw new IllegalArgumentException( "Cannot split an experiment that has other parts. Delete the other parts first before splitting again." );
        }

        if ( eeService.getArrayDesignsUsed( toSplit ).size() > 1 ) {
            throw new IllegalArgumentException( "Cannot split experiments that are on more than one platform." );
        }

        if ( ExperimentFactorUtils.isBatchFactor( splitOn ) ) {
            throw new IllegalArgumentException( "Cannot split an experiment on a batch factor." );
        }

        Set<ExpressionExperiment> result = new HashSet<>();

        String sourceShortName = toSplit.getShortName();

        // we cannot rely on ExpressionExperiment.getQuantitationTypes() because it is a denormalization, and we might
        // miss some vectors
        Map<Class<? extends DataVector>, Set<QuantitationType>> qtsByVectorType = eeService.getQuantitationTypesByVectorType( toSplit );

        if ( qtsByVectorType.isEmpty() ) {
            log.warn( "Experiment has no QTs, probably doesn't have data, post-processing of splits will be skipped" );
        }

        // Get the expression data matrices for the experiment. We'll split them and generate new vectors
        boolean foundPreferred = false;
        Map<QuantitationType, Collection<RawExpressionDataVector>> qt2RawVec = new HashMap<>();
        Map<QuantitationType, Collection<SingleCellExpressionDataVector>> qt2SingleCellVec = new HashMap<>();

        for ( Map.Entry<Class<? extends DataVector>, Set<QuantitationType>> e : qtsByVectorType.entrySet() ) {
            Class<? extends DataVector> vectorType = e.getKey();
            Set<QuantitationType> qts = e.getValue();
            if ( RawExpressionDataVector.class.isAssignableFrom( vectorType ) ) {
                log.info( "Fetching raw expression data vectors... " );
                for ( QuantitationType qt : qts ) {
                    Collection<RawExpressionDataVector> vectors = eeService.getRawDataVectors( toSplit, qt );
                    if ( vectors.isEmpty() ) {
                        // this is okay if the data is processed, or if we have stray orphaned QTs
                        log.debug( "No raw vectors for " + qt + ", skipping..." );
                        continue;
                    }
                    if ( qt.getIsPreferred() ) {
                        foundPreferred = true;
                    }
                    log.info( vectors.size() + " vectors for " + qt + "; preferred=" + qt.getIsPreferred() );

                    qt2RawVec.put( qt, ( vectors ) );
                }
                if ( !foundPreferred ) {
                    log.warn( "No preferred quantitation type found; post-processing of splits will be skipped" );
                }
            } else if ( SingleCellExpressionDataVector.class.isAssignableFrom( vectorType ) ) {
                log.info( "Fetching single-cell data vectors..." );
                for ( QuantitationType qt : qts ) {
                    List<SingleCellExpressionDataVector> vectors = new ArrayList<>( singleCellExpressionExperimentService.getSingleCellDataVectors( toSplit, qt ) );
                    if ( vectors.isEmpty() ) {
                        log.warn( "No single-cell vectors for " + qt + ", skipping..." );
                        continue;
                    }
                    qt2SingleCellVec.put( qt, vectors );
                }
            } else if ( ProcessedExpressionDataVector.class.isAssignableFrom( vectorType ) ) {
                log.debug( "Found processed data vectors; these will not be carried over to the splits." );
            } else {
                throw new UnsupportedOperationException( "Unsupported data vector type for splitting: " + vectorType.getName() + "." );
            }
        }

        // stub the new experiments and create new names; all other information should be retained. Permissions should be the same.
        int splitNumber = 0;

        for ( FactorValue splitValue : splitOn.getFactorValues() ) {
            splitNumber++;
            ExpressionExperiment split = ExpressionExperiment.Factory.newInstance();
            split.setShortName( sourceShortName + "." + splitNumber );

            // copy everything but samples over
            split.setName( generateNameForSplit( toSplit, splitNumber, splitValue ) );
            split.setDescription( "This experiment was created by Gemma splitting another: \n" + toSplit + toSplit.getDescription() );

            split.setCharacteristics( this.cloneCharacteristics( toSplit.getCharacteristics() ) );
            split.setMetadata( toSplit.getMetadata() ); //
            split.setPrimaryPublication( toSplit.getPrimaryPublication() );
            split.getOtherRelevantPublications().addAll( toSplit.getOtherRelevantPublications() );
            if ( toSplit.getAccession() != null ) {
                split.setAccession( this.cloneAccession( toSplit.getAccession() ) ); // accession is currently unique, so have to clone
            }
            split.setOwner( toSplit.getOwner() );
            split.setSource( toSplit.getSource() );
            split.setTaxon( toSplit.getTaxon() );
            // starting with a fresh audit trail.

            Map<FactorValue, FactorValue> old2cloneFV = new HashMap<>();
            if ( toSplit.getExperimentalDesign() != null ) {
                split.setExperimentalDesign( this.cloneExperimentalDesign( toSplit.getExperimentalDesign(), old2cloneFV ) );
            }

            // add the biomaterials
            List<BioAssay> clonedBAs = new ArrayList<>();
            Collection<FactorValue> usedFactorValues = new HashSet<>();
            for ( BioAssay ba : toSplit.getBioAssays() ) {
                boolean kept = false;
                BioMaterial bm = ba.getSampleUsed();

                // identify samples we want to include
                // TODO: support sub-biomaterials and use getAllFactorValues() instead, we also need to implement
                //       cloneBioMaterial() accordingly
                for ( FactorValue fv : bm.getFactorValues() ) {
                    if ( fv.equals( splitValue ) ) {
                        kept = true;
                        break;
                    }
                }

                if ( kept ) {
                    BioAssay newBa = this.cloneBioAssay( ba );
                    clonedBAs.add( newBa );
                    for ( FactorValue fv : bm.getFactorValues() ) {
                        if ( fv.equals( splitValue ) ) {
                            // make a BioMaterial characteristic, so we don't lose the information (might be redundant)
                            for ( Characteristic c : fv.getCharacteristics() ) {
                                newBa.getSampleUsed().getCharacteristics().add( this.cloneCharacteristic( c ) );
                            }
                            // note that the split FV is not included as a FV in the new BM because all the samples
                            // share the same value
                            continue;
                        }
                        newBa.getSampleUsed().getFactorValues().add( old2cloneFV.get( fv ) );
                        usedFactorValues.add( old2cloneFV.get( fv ) );
                    }
                }
            }

            split.getBioAssays().clear();
            split.getBioAssays().addAll( clonedBAs );
            split.setNumberOfSamples( clonedBAs.size() );

            // remove unused factors and factor values from the design and biomaterials
            if ( split.getExperimentalDesign() != null ) {
                Collection<ExperimentalFactor> toRemoveFactors = new HashSet<>();
                for ( ExperimentalFactor ef : split.getExperimentalDesign().getExperimentalFactors() ) {
                    Collection<FactorValue> toRemove = new HashSet<>();
                    for ( FactorValue fv : ef.getFactorValues() ) { // these are clones
                        if ( !usedFactorValues.contains( fv ) ) {
                            toRemove.add( fv );
                        }
                    }

                    if ( ef.getFactorValues().removeAll( toRemove ) ) {
                        log.info( toRemove.size() + " unused factor values removed for " + ef + " in split " + splitNumber + ", leaving "
                                + ef.getFactorValues().size() + " fvs still used" );
                    }

                    assert !split.getBioAssays().isEmpty();

                    // EFs that have only one level, or which aren't used at all, are removed from the biomaterials (and gathered for removal from the ED)
                    if ( ef.getFactorValues().size() <= 1 ) {
                        toRemoveFactors.add( ef );
                        for ( BioAssay ba : split.getBioAssays() ) {
                            BioMaterial bm = ba.getSampleUsed();
                            Collection<FactorValue> fvsToClear = new HashSet<>();
                            for ( FactorValue fv : bm.getFactorValues() ) {
                                if ( fv.getExperimentalFactor().equals( ef ) ) {
                                    fvsToClear.add( fv );
                                }
                            }
                            if ( bm.getFactorValues().removeAll( fvsToClear ) ) {
                                log.debug( "Cleared " + fvsToClear.size() + " unused factor values from " + bm );
                            }
                        }
                    }
                }

                // remove the unused/unneeded factors from the ED
                if ( split.getExperimentalDesign().getExperimentalFactors().removeAll( toRemoveFactors ) ) {
                    log.info( toRemoveFactors.size() + " unused experimental factors dropped from split " + splitNumber );
                }
            }

            log.info( "Building vectors for " + qt2RawVec.size() + " quantitation types ..." );
            Map<BioAssayDimension, BioAssayDimension> dimensionCache = new HashMap<>();
            for ( QuantitationType qt : qt2RawVec.keySet() ) {
                QuantitationType clonedQt = this.cloneQt( qt, split );
                split.getQuantitationTypes().add( clonedQt );
                // these bms are same as the ones associated with the vectors, not the clones
                Collection<RawExpressionDataVector> vectors = qt2RawVec.get( qt );
                Collection<RawExpressionDataVector> rawDataVectors = BulkDataSlicerUtils.slice( vectors, clonedBAs, RawExpressionDataVector.class, true, dimensionCache );
                // slice retain the original QT, so we need to replace it with the clone
                vectors.forEach( v -> v.setQuantitationType( clonedQt ) );
                log.info( split.getShortName() + ": Adding " + rawDataVectors.size() + " raw data vectors for " + clonedQt + " preferred="
                        + clonedQt.getIsPreferred() );
                split.getRawExpressionDataVectors().addAll( rawDataVectors );
            }

            Map<SingleCellDimension, SingleCellDimension> singleCellDimensionCache = new HashMap<>();

            for ( QuantitationType qt : qt2SingleCellVec.keySet() ) {
                QuantitationType clonedQt = this.cloneQt( qt, split );
                split.getQuantitationTypes().add( clonedQt );
                Collection<SingleCellExpressionDataVector> scVectors = SingleCellSlicerUtils.slice( qt2SingleCellVec.get( qt ), clonedBAs, singleCellDimensionCache );
                // slice retain the original QT, so we need to replace it with the clone
                scVectors.forEach( v -> v.setQuantitationType( clonedQt ) );
                log.info( split.getShortName() + ": Adding " + scVectors.size() + " single-cell data vectors for " + clonedQt + " preferred="
                        + clonedQt.getIsSingleCellPreferred() );
                split.getSingleCellExpressionDataVectors().addAll( scVectors );
            }

            split = persister.persist( split );

            // securityService.makePublic( split ); // temporary
            result.add( split );
        }

        enforceOtherParts( result );
        eeService.update( result );

        /*
         * Create a new "experiment set" that groups them together (not sure if we'll keep this)
         */
        ExpressionExperimentSet g = ExpressionExperimentSet.Factory.newInstance();
        g.setDescription( "Parts of " + toSplit.getShortName() + " that were split on " + splitOn.getName() );
        g.setName( toSplit.getShortName() + " splits" );
        g.setTaxon( toSplit.getTaxon() );
        g.getExperiments().addAll( result );
        g = this.expressionExperimentSetService.create( g );

        return new ExperimentSplitResult( g, foundPreferred );
    }

    static String generateNameForSplit( ExpressionExperiment toSplit, int splitNumber, FactorValue splitValue ) {
        String categoryString = StringUtils.strip( splitValue.getExperimentalFactor().getCategory() != null ?
                splitValue.getExperimentalFactor().getCategory().getValue() :
                splitValue.getExperimentalFactor().getName() );
        String factorValueString = FactorValueUtils.getSummaryString( splitValue );
        String suffix = String.format( " [%s = %s]", categoryString, factorValueString );
        return abbreviateWithSuffix(
                String.format( "Split part %d of: %s", splitNumber, StringUtils.strip( toSplit.getName() ) ), suffix,
                "…", ExpressionExperiment.MAX_NAME_LENGTH, true, StandardCharsets.UTF_8 );
    }

    private void enforceOtherParts( Collection<ExpressionExperiment> result ) {
        // Enforce relation to other parts of the split.
        for ( ExpressionExperiment split : result ) {
            for ( ExpressionExperiment split2 : result ) {
                if ( split.equals( split2 ) ) continue;
                split.getOtherParts().add( split2 );
            }
        }
    }

    private ExperimentalDesign cloneExperimentalDesign( ExperimentalDesign experimentalDesign, Map<FactorValue, FactorValue> old2cloneFV ) {
        ExperimentalDesign clone = ExperimentalDesign.Factory.newInstance();
        clone.setDescription( experimentalDesign.getDescription() );
        clone.setName( experimentalDesign.getName() );
        clone.setNormalizationDescription( experimentalDesign.getNormalizationDescription() );
        clone.setQualityControlDescription( experimentalDesign.getQualityControlDescription() );
        clone.setReplicateDescription( experimentalDesign.getReplicateDescription() );
        clone.setTypes( this.cloneCharacteristics( experimentalDesign.getTypes() ) );

        clone.getExperimentalFactors()
                .addAll( this.cloneExperimentalFactors( experimentalDesign.getExperimentalFactors(), clone, old2cloneFV ) );

        return clone;
    }

    private Collection<ExperimentalFactor> cloneExperimentalFactors( Collection<ExperimentalFactor> experimentalFactors, ExperimentalDesign ed,
            Map<FactorValue, FactorValue> old2cloneFV ) {
        assert ed.getId() == null;
        Collection<ExperimentalFactor> result = new HashSet<>();
        for ( ExperimentalFactor ef : experimentalFactors ) {
            ExperimentalFactor clone = ExperimentalFactor.Factory.newInstance();
            //noinspection deprecation
            clone.setAnnotations( this.cloneCharacteristics( ef.getAnnotations() ) );
            if ( ef.getCategory() != null ) {
                clone.setCategory( this.cloneCharacteristic( ef.getCategory() ) );
            }
            clone.setName( ef.getName() );
            clone.setDescription( ef.getDescription() );
            clone.setType( ef.getType() );
            clone.getFactorValues().addAll( this.cloneFactorValues( ef.getFactorValues(), clone, old2cloneFV ) );
            clone.setExperimentalDesign( ed );
            result.add( clone );
            //    assert clone.getId() == null;
        }
        return result;
    }

    private Collection<FactorValue> cloneFactorValues( Collection<FactorValue> factorValues, ExperimentalFactor ef,
            Map<FactorValue, FactorValue> old2cloneFV ) {
        assert ef.getId() == null;
        Collection<FactorValue> result = new HashSet<>();
        for ( FactorValue fv : factorValues ) {
            FactorValue clone = FactorValue.Factory.newInstance( ef );
            clone.setCharacteristics( cloneStatements( fv ) );
            clone.setIsBaseline( fv.getIsBaseline() );
            //noinspection deprecation
            clone.setValue( fv.getValue() );
            if ( fv.getMeasurement() != null ) {
                clone.setMeasurement( this.cloneMeasurement( fv.getMeasurement() ) );
            }
            result.add( clone );
            assert !old2cloneFV.containsKey( fv );
            old2cloneFV.put( fv, clone );
        }

        return result;
    }

    private Set<Statement> cloneStatements( FactorValue fv ) {
        Collection<Statement> ch = fv.getCharacteristics();
        // pair of original -> clone
        List<Statement> result = new ArrayList<>( ch.size() );
        for ( Statement s : ch ) {
            result.add( cloneStatement( s ) );
        }
        return new HashSet<>( result );
    }

    private Statement cloneStatement( Statement s ) {
        Statement clone = Statement.Factory.newInstance();
        clone.setName( s.getName() );
        clone.setDescription( s.getDescription() );
        clone.setOriginalValue( s.getOriginalValue() );
        clone.setSubject( s.getSubject() );
        clone.setSubjectUri( s.getSubjectUri() );
        clone.setCategory( s.getCategory() );
        clone.setCategoryUri( s.getCategoryUri() );
        clone.setEvidenceCode( s.getEvidenceCode() );
        clone.setPredicate( s.getPredicate() );
        clone.setPredicateUri( s.getPredicateUri() );
        clone.setObject( s.getObject() );
        clone.setObjectUri( s.getObjectUri() );
        clone.setSecondPredicate( s.getSecondPredicate() );
        clone.setSecondPredicateUri( s.getSecondPredicateUri() );
        clone.setSecondObject( s.getSecondObject() );
        clone.setSecondObjectUri( s.getSecondObjectUri() );
        return clone;
    }

    private Measurement cloneMeasurement( Measurement measurement ) {
        Measurement clone = Measurement.Factory.newInstance();
        clone.setKindCV( measurement.getKindCV() );
        clone.setRepresentation( measurement.getRepresentation() );
        clone.setOtherKind( measurement.getOtherKind() );
        clone.setValue( measurement.getValue() );
        clone.setType( measurement.getType() );

        return clone;
    }

    private QuantitationType cloneQt( QuantitationType qt, ExpressionExperiment split ) {
        QuantitationType clone = QuantitationType.Factory.newInstance();
        clone.setDescription( qt.getDescription() + " (created for split: " + split.getShortName() + ")" );
        clone.setName( qt.getName() );
        clone.setGeneralType( qt.getGeneralType() );
        clone.setIsBackground( qt.getIsBackground() );
        clone.setIsBackgroundSubtracted( qt.getIsBackgroundSubtracted() );
        clone.setIsBatchCorrected( qt.getIsBatchCorrected() );
        //noinspection deprecation
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

    private Set<Characteristic> cloneCharacteristics( Collection<Characteristic> ch ) {
        Set<Characteristic> result = new HashSet<>();
        for ( Characteristic c : ch ) {
            Characteristic clone = cloneCharacteristic( c );
            result.add( clone );
        }
        return result;
    }

    private Characteristic cloneCharacteristic( Characteristic c ) {
        Characteristic clone = Characteristic.Factory.newInstance();
        clone.setName( c.getName() );
        clone.setDescription( c.getDescription() );
        clone.setCategory( c.getCategory() );
        clone.setCategoryUri( c.getCategoryUri() );
        clone.setValue( c.getValue() );
        clone.setValueUri( c.getValueUri() );
        clone.setOriginalValue( c.getOriginalValue() );
        clone.setEvidenceCode( c.getEvidenceCode() );
        return clone;
    }

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

        BioMaterial sampleClone = this.cloneBioMaterial( ba.getSampleUsed() );
        clone.setSampleUsed( sampleClone );
        sampleClone.getBioAssaysUsedIn().add( clone );
        if ( ba.getAccession() != null ) {
            clone.setAccession( this.cloneAccession( ba.getAccession() ) );
        }

        return clone;
    }

    private DatabaseEntry cloneAccession( DatabaseEntry de ) {
        DatabaseEntry clone = DatabaseEntry.Factory.newInstance();
        clone.setAccession( de.getAccession() );
        clone.setAccessionVersion( de.getAccessionVersion() );
        //noinspection deprecation
        clone.setUri( de.getUri() );
        clone.setExternalDatabase( de.getExternalDatabase() );
        return clone;
    }

    private BioMaterial cloneBioMaterial( BioMaterial bm ) {
        Assert.isNull( bm.getSourceBioMaterial(), "Cannot split an experiment with biomaterials that have a source biomaterial." );
        BioMaterial clone = BioMaterial.Factory.newInstance();
        clone.setName( abbreviateWithSuffix( bm.getName(), " (Split)", "…", BioMaterial.MAX_NAME_LENGTH, true, StandardCharsets.UTF_8 ) ); // it is important we make a new name, so we don't confuse this with the previous one in findOrCreate();
        clone.setDescription( bm.getDescription() );
        clone.setCharacteristics( this.cloneCharacteristics( bm.getCharacteristics() ) );
        if ( bm.getExternalAccession() != null ) {
            clone.setExternalAccession( this.cloneAccession( bm.getExternalAccession() ) );
        }
        clone.setSourceTaxon( bm.getSourceTaxon() );
        clone.setTreatments( this.cloneTreatments( bm.getTreatments() ) );
        // Factor values are done separately
        return clone;
    }

    private Set<Treatment> cloneTreatments( Collection<Treatment> ts ) {
        Set<Treatment> result = new HashSet<>();
        for ( Treatment t : ts ) {
            Treatment clone = Treatment.Factory.newInstance();
            clone.setDescription( t.getDescription() );
            clone.setName( t.getName() );
            clone.setOrderApplied( t.getOrderApplied() );
            result.add( clone );
        }

        return result;
    }

}
