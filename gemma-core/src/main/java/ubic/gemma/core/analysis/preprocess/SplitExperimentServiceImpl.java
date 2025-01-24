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

import gemma.gsec.SecurityService;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import ubic.gemma.core.analysis.service.ExpressionDataFileService;
import ubic.gemma.core.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.core.datastructure.matrix.ExpressionDataMatrix;
import ubic.gemma.core.datastructure.matrix.ExpressionDataMatrixBuilder;
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
import ubic.gemma.model.expression.experiment.*;
import ubic.gemma.persistence.persister.Persister;
import ubic.gemma.persistence.service.expression.bioAssayData.RawExpressionDataVectorService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentSetService;
import ubic.gemma.persistence.service.expression.experiment.FactorValueService;

import java.nio.charset.StandardCharsets;
import java.util.*;

import static ubic.gemma.core.util.StringUtils.abbreviateInUTF8Bytes;

/**
 *
 * Split an experiment into multiple experiments. This is needed when a load EE (e.g. from GEO) is better represented as
 * two more distinct experiments. The decision of what to split is based on curation guidelines documented
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
    private Persister persister;

    @Autowired
    private SecurityService securityService;

    @Autowired
    private ExpressionDataFileService dataFileService;

    @Autowired
    private ExpressionExperimentSetService expressionExperimentSetService;

    @Autowired
    private FactorValueService factorValueService;

    /*
     * (non-Javadoc)
     *
     * @see ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService#split(ubic.gemma.model.
     * expression.experiment.ExpressionExperiment, ubic.gemma.model.expression.experiment.ExperimentalFactor)
     */
    @Override
    public ExpressionExperimentSet split( ExpressionExperiment toSplit, ExperimentalFactor splitOn, boolean postProcess ) {

        toSplit = eeService.thawLite( toSplit );

        if ( !toSplit.getOtherParts().isEmpty() ) {
            throw new IllegalArgumentException( "You cannot split an experiment that was already created by a split" );
        }

        if ( eeService.getArrayDesignsUsed( toSplit ).size() > 1 ) {
            throw new IllegalArgumentException( "Cannot split experiments that are on more than one platform" );
        }

        if ( ExperimentalDesignUtils.isBatchFactor( splitOn ) ) {
            throw new IllegalArgumentException( "Do not split experiments on 'batch'" );
        }

        Collection<ExpressionExperiment> result = new HashSet<>();

        String sourceShortName = toSplit.getShortName();

        Collection<QuantitationType> qts = eeService.getQuantitationTypes( toSplit );

        // Get the expression data matrices for the experiment. We'll split them and generate new vectors
        boolean foundPreferred = false;
        Map<QuantitationType, ExpressionDataMatrix<?>> qt2mat = new HashMap<>();

        if ( qts.size() > 0 ) {
            log.info( "Fetching raw expression data vectors ... " );
            for ( QuantitationType qt : qts ) {
                if ( !qt.getRepresentation().equals( PrimitiveType.DOUBLE ) ) {
                    throw new UnsupportedOperationException( "Non-double values currently not supported for experiment split" );
                }

                Collection<RawExpressionDataVector> vectors = rawExpressionDataVectorService.findAndThaw( qt );
                if ( vectors.isEmpty() ) {
                    // this is okay if the data is processed, or if we have stray orphaned QTs
                    log.debug( "No raw vectors for " + qt + "; preferred=" + qt.getIsPreferred() );
                    continue;
                }
                if ( qt.getIsPreferred() ) {
                    foundPreferred = true;
                }
                log.info( vectors.size() + " vectors for " + qt + "; preferred=" + qt.getIsPreferred() );

                qt2mat.put( qt, ExpressionDataMatrixBuilder.getMatrix( vectors ) );
            }

            if ( !foundPreferred ) {
                log.warn( "No preferred quantitation type found; post-processing of splits will be skipped" );
            }
        } else {
            log.warn( "Experiment has no QTs, probably doesn't have data, post-processing of splits will be skipped" );
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
                // TODO: support sub-biomaterials and use getAllFactorValues() instead, we also need to implement
                //       cloneBioMaterial() accordingly
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
                            // make a BioMaterial characteristic, so we don't lose the information (might be redundant)
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

            // here we're using the original bms; we'll replace them
            BioAssayDimension newBAD = makeBioAssayDimension( bms, toSplit );
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

            // remove unused factors and factor values from the design and biomaterials
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

            log.info( "Building vectors for " + qt2mat.size() + " quantitation types ..." );
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

            split = ( ExpressionExperiment ) persister.persist( split );

            split = eeService.thawLiter( split );

            // securityService.makePublic( split ); // temporary 
            result.add( split );
        }

        enforceOtherParts( result );

        for ( ExpressionExperiment split : result ) {
            eeService.update( split );

            // postprocess
            if ( foundPreferred && postProcess ) {
                try {
                    preprocessor.process( split );
                } catch ( Exception e ) {
                    log.error( "Failure while postprocessing (will continue): " + split + ": " + e.getMessage() );
                }
            } else {
                log.info( "Postprocessing skipped for " + split );
            }
        }

        /*
         * Create a new "experiment set" that groups them together (not sure if we'll keep this)
         */
        ExpressionExperimentSet g = ExpressionExperimentSet.Factory.newInstance();
        g.setDescription( "Parts of " + toSplit.getShortName() + " that were split on " + splitOn.getName() );
        g.setName( toSplit.getShortName() + " splits" );
        g.setTaxon( toSplit.getTaxon() );
        g.getExperiments().addAll( result );
        g = this.expressionExperimentSetService.create( g );

        // remove useless data files

        dataFileService.deleteAllFiles( toSplit );
        // Clean the source experiment? remove diff and coexpression analyses, PCA, correlation matrices, processed data vectors
        // delete it?
        // eeService.remove(toSplit);
        // OR perhaps only
        securityService.makePrivate( toSplit );
        // Or mark it as troubled?

        return g;
    }

    static String generateNameForSplit( ExpressionExperiment toSplit, int splitNumber, FactorValue splitValue ) {
        String template = "Split part %d of: %s [%s = %s]";
        String originalName = StringUtils.strip( toSplit.getName() );
        String factorValueString = FactorValueUtils.getSummaryString( splitValue );
        String newFullName = String.format( template, splitNumber, originalName,
                StringUtils.strip( splitValue.getExperimentalFactor().getCategory() != null ?
                        splitValue.getExperimentalFactor().getCategory().getValue() :
                        splitValue.getExperimentalFactor().getName() ),
                factorValueString );
        if ( newFullName.getBytes( StandardCharsets.UTF_8 ).length <= ExpressionExperiment.MAX_NAME_LENGTH )
            return newFullName;
        // truncate the original name
        int lengthOfEverythingElse = newFullName.getBytes( StandardCharsets.UTF_8 ).length - String.format( "%s", originalName ).getBytes( StandardCharsets.UTF_8 ).length;
        //  we want at least 100 characters of the original name
        if ( lengthOfEverythingElse > ExpressionExperiment.MAX_NAME_LENGTH - 100 ) {
            throw new IllegalArgumentException( "It's not possible to truncate the name of the split such that it won't exceed " + ExpressionExperiment.MAX_NAME_LENGTH + " characters." );
        }
        return String.format( template, splitNumber, abbreviateInUTF8Bytes( originalName, "…", ExpressionExperiment.MAX_NAME_LENGTH - lengthOfEverythingElse )
                        // remove trailing spaces
                        .replace( "\\s+…$", "…" ),
                StringUtils.strip( splitValue.getExperimentalFactor().getCategory() != null ?
                        splitValue.getExperimentalFactor().getCategory().getValue() :
                        splitValue.getExperimentalFactor().getName() ),
                factorValueString );
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
            clone.setMeasurement( this.cloneMeasurement( fv.getMeasurement() ) );
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

        if ( measurement == null ) return null;
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
        clone.setAccession( this.cloneAccession( ba.getAccession() ) );

        return clone;
    }

    private DatabaseEntry cloneAccession( DatabaseEntry de ) {
        if ( de == null ) return null;
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
        clone.setName( abbreviateInUTF8Bytes( bm.getName(), "…", BioMaterial.MAX_NAME_LENGTH - " (Split)".length() ) + " (Split)" ); // it is important we make a new name, so we don't confuse this with the previous one in findOrCreate();
        clone.setDescription( bm.getDescription() );
        clone.setCharacteristics( this.cloneCharacteristics( bm.getCharacteristics() ) );
        clone.setExternalAccession( this.cloneAccession( bm.getExternalAccession() ) );
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
