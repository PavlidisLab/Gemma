package ubic.gemma.model.expression.bioAssayData;

import ubic.gemma.model.common.description.*;
import ubic.gemma.model.common.quantitationtype.*;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignValueObject;
import ubic.gemma.model.expression.arrayDesign.TechnologyType;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssay.BioAssayValueObject;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.biomaterial.BioMaterialValueObject;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.BioAssaySetValueObject;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentSubsetValueObject;
import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;
import ubic.gemma.model.genome.Taxon;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Utilities for converting {@link DoubleVectorValueObject} to {@link BulkExpressionDataVector} instances.
 * @author poirigui
 */
public class DoubleVectorValueObjectUtils {

    /**
     * Convert a collection of {@link DoubleVectorValueObject}.
     */
    public static Collection<ProcessedExpressionDataVector> toBulkVectors( Collection<DoubleVectorValueObject> vectors ) {
        if ( vectors.isEmpty() ) {
            return Collections.emptyList();
        }
        Map<BioAssaySetValueObject, ExpressionExperiment> eevo2ee = new HashMap<>();
        Map<QuantitationTypeValueObject, QuantitationType> qtvo2qt = new HashMap<>();
        Map<BioAssayDimensionValueObject, BioAssayDimension> badvo2bad = new HashMap<>();
        Map<ExternalDatabaseValueObject, ExternalDatabase> edvo2ed = new HashMap<>();
        Map<ArrayDesignValueObject, ArrayDesign> advo2ad = new HashMap<>();
        Map<BioMaterialValueObject, BioMaterial> bmvo2bm = new HashMap<>();
        return vectors.stream()
                .map( vec -> toBulkVector( vec, eevo2ee, qtvo2qt, badvo2bad, edvo2ed, advo2ad, bmvo2bm ) )
                .collect( Collectors.toList() );
    }

    private static ProcessedExpressionDataVector toBulkVector( DoubleVectorValueObject doubleVectorValueObject, Map<BioAssaySetValueObject, ExpressionExperiment> eevo2ee, Map<QuantitationTypeValueObject, QuantitationType> qtvo2qt, Map<BioAssayDimensionValueObject, BioAssayDimension> badvo2bad, Map<ExternalDatabaseValueObject, ExternalDatabase> edvo2ed, Map<ArrayDesignValueObject, ArrayDesign> advo2ad, Map<BioMaterialValueObject, BioMaterial> bmvo2bm ) {
        ProcessedExpressionDataVector vec = new ProcessedExpressionDataVector();
        vec.setId( doubleVectorValueObject.getId() );
        vec.setExpressionExperiment( eevo2ee.computeIfAbsent( doubleVectorValueObject.getExpressionExperiment(), DoubleVectorValueObjectUtils::toExpressionExperiment ) );
        vec.setQuantitationType( qtvo2qt.computeIfAbsent( doubleVectorValueObject.getQuantitationType(), DoubleVectorValueObjectUtils::toQT ) );
        vec.setBioAssayDimension( badvo2bad.computeIfAbsent( doubleVectorValueObject.getBioAssayDimension(), bioAssayDimensionValueObject -> toBAD( bioAssayDimensionValueObject, edvo2ed, advo2ad, bmvo2bm ) ) );
        CompositeSequence de = new CompositeSequence();
        de.setId( doubleVectorValueObject.getDesignElement().getId() );
        de.setName( doubleVectorValueObject.getDesignElement().getName() );
        vec.setDesignElement( de );
        vec.setDataAsDoubles( doubleVectorValueObject.getData() );
        vec.setRankByMax( doubleVectorValueObject.getRankByMax() );
        vec.setRankByMean( doubleVectorValueObject.getRankByMean() );
        return vec;
    }

    private static ExpressionExperiment toExpressionExperiment( BioAssaySetValueObject basSetVo ) {
        if ( basSetVo instanceof ExpressionExperimentValueObject ) {
            ExpressionExperimentValueObject eeVo = ( ExpressionExperimentValueObject ) basSetVo;
            ExpressionExperiment ee = new ExpressionExperiment();
            ee.setId( eeVo.getId() );
            ee.setShortName( eeVo.getShortName() );
            ExternalDatabase ed = ExternalDatabase.Factory.newInstance( eeVo.getExternalDatabase(), DatabaseType.EXPRESSION );
            ed.setWebUri( eeVo.getExternalDatabaseUri() );
            ee.setAccession( DatabaseEntry.Factory.newInstance( eeVo.getAccession(), ed ) );
            ee.setName( eeVo.getName() );
            ee.setDescription( eeVo.getDescription() );
            ee.setNumberOfSamples( eeVo.getNumberOfBioAssays() );
            // TODO: characteristics
            return ee;
        } else if ( basSetVo instanceof ExpressionExperimentSubsetValueObject ) {
            ExpressionExperiment ee = new ExpressionExperiment();
            ee.setId( ( ( ExpressionExperimentSubsetValueObject ) basSetVo ).getSourceExperimentId() );
            ee.setShortName( ( ( ExpressionExperimentSubsetValueObject ) basSetVo ).getSourceExperimentShortName() );
            return ee;
        } else {
            throw new UnsupportedOperationException( "Unsupported BioAssaySet VO type: " + basSetVo.getClass().getName() );
        }
    }

    private static QuantitationType toQT( QuantitationTypeValueObject quantitationTypeValueObject ) {
        QuantitationType qt = QuantitationType.Factory.newInstance();
        qt.setId( quantitationTypeValueObject.getId() );
        qt.setName( quantitationTypeValueObject.getName() );
        qt.setDescription( quantitationTypeValueObject.getDescription() );
        qt.setGeneralType( GeneralType.valueOf( quantitationTypeValueObject.getGeneralType() ) );
        qt.setType( StandardQuantitationType.valueOf( quantitationTypeValueObject.getType() ) );
        qt.setScale( ScaleType.valueOf( quantitationTypeValueObject.getScale() ) );
        qt.setRepresentation( PrimitiveType.valueOf( quantitationTypeValueObject.getRepresentation() ) );
        qt.setIsPreferred( quantitationTypeValueObject.getIsPreferred() );
        qt.setIsMaskedPreferred( quantitationTypeValueObject.getIsMaskedPreferred() );
        qt.setIsNormalized( quantitationTypeValueObject.getIsNormalized() );
        qt.setIsBatchCorrected( quantitationTypeValueObject.getIsBatchCorrected() );
        qt.setIsBackground( quantitationTypeValueObject.getIsBackground() );
        qt.setIsBackgroundSubtracted( quantitationTypeValueObject.getIsBackgroundSubtracted() );
        return qt;
    }

    private static BioAssayDimension toBAD( BioAssayDimensionValueObject bioAssayDimensionValueObject, Map<ExternalDatabaseValueObject, ExternalDatabase> edvo2ed, Map<ArrayDesignValueObject, ArrayDesign> advo2ad, Map<BioMaterialValueObject, BioMaterial> bmvo2bm ) {
        BioAssayDimension bad = BioAssayDimension.Factory.newInstance();
        for ( BioAssayValueObject bavo : bioAssayDimensionValueObject.getBioAssays() ) {
            bad.getBioAssays().add( toBioAssay( bavo, edvo2ed, advo2ad, bmvo2bm ) );
        }
        return bad;
    }

    private static BioAssay toBioAssay( BioAssayValueObject bavo, Map<ExternalDatabaseValueObject, ExternalDatabase> edvo2ed, Map<ArrayDesignValueObject, ArrayDesign> advo2ad, Map<BioMaterialValueObject, BioMaterial> bmvo2bm ) {
        BioAssay ba = BioAssay.Factory.newInstance();
        ba.setId( bavo.getId() );
        ba.setShortName( bavo.getShortName() );
        if ( bavo.getAccession() != null ) {
            ba.setAccession( toDatabaseEntry( bavo.getAccession(), edvo2ed, DatabaseType.EXPRESSION ) );
        }
        ba.setName( bavo.getName() );
        ba.setDescription( bavo.getDescription() );
        ba.setIsOutlier( bavo.isOutlier() );
        ba.setArrayDesignUsed( advo2ad.computeIfAbsent( bavo.getArrayDesign(), advo -> toArrayDesign( advo, edvo2ed ) ) );
        BioMaterial bm = bmvo2bm.computeIfAbsent( bavo.getSample(), DoubleVectorValueObjectUtils::toSample );
        ba.setSampleUsed( bm );
        bm.getBioAssaysUsedIn().add( ba );
        if ( bavo.getOriginalPlatform() != null ) {
            ba.setOriginalPlatform( advo2ad.computeIfAbsent( bavo.getOriginalPlatform(), advo -> toArrayDesign( advo, edvo2ed ) ) );
        }
        return ba;
    }

    private static BioMaterial toSample( BioMaterialValueObject sample ) {
        BioMaterial bm = BioMaterial.Factory.newInstance();
        bm.setId( sample.getId() );
        bm.setName( sample.getName() );
        bm.setDescription( sample.getDescription() );
        return bm;
    }

    private static ArrayDesign toArrayDesign( ArrayDesignValueObject advo, Map<ExternalDatabaseValueObject, ExternalDatabase> edvo2ed ) {
        ArrayDesign ad = ArrayDesign.Factory.newInstance();
        ad.setId( advo.getId() );
        ad.setShortName( advo.getShortName() );
        Taxon taxon;
        if ( advo.getTaxonObject() != null ) {
            taxon = Taxon.Factory.newInstance();
            taxon.setId( advo.getTaxonObject().getId() );
            taxon.setCommonName( advo.getTaxonObject().getCommonName() );
            taxon.setScientificName( advo.getTaxonObject().getScientificName() );
            taxon.setNcbiId( advo.getTaxonObject().getNcbiId() );
            taxon.setIsGenesUsable( advo.getTaxonObject().getIsGenesUsable() );
            taxon.setExternalDatabase( toExternalDatabase( advo.getTaxonObject().getExternalDatabase(), edvo2ed, DatabaseType.GENOME ) );
        } else {
            taxon = Taxon.Factory.newInstance();
            taxon.setId( advo.getTaxonID() );
            taxon.setCommonName( advo.getTaxon() );
        }
        ad.setPrimaryTaxon( taxon );
        if ( advo.getExternalReferences() != null ) {
            ad.setExternalReferences( advo.getExternalReferences().stream()
                    .map( ( DatabaseEntryValueObject accession ) -> toDatabaseEntry( accession, edvo2ed, DatabaseType.SEQUENCE ) )
                    .collect( Collectors.toSet() ) );
        }
        ad.setName( advo.getName() );
        ad.setDescription( advo.getDescription() );
        ad.setTechnologyType( TechnologyType.valueOf( advo.getTechnologyType() ) );
        return ad;
    }

    private static DatabaseEntry toDatabaseEntry( DatabaseEntryValueObject accession, Map<ExternalDatabaseValueObject, ExternalDatabase> edvo2ed, DatabaseType databaseType ) {
        DatabaseEntry dbentry = DatabaseEntry.Factory.newInstance();
        dbentry.setId( accession.getId() );
        dbentry.setAccession( accession.getAccession() );
        dbentry.setExternalDatabase( edvo2ed.computeIfAbsent( accession.getExternalDatabase(), edvo -> toExternalDatabase( edvo, edvo2ed, databaseType ) ) );
        return dbentry;
    }

    private static ExternalDatabase toExternalDatabase( ExternalDatabaseValueObject externalDatabaseValueObject, Map<ExternalDatabaseValueObject, ExternalDatabase> edvo2ed, DatabaseType type ) {
        ExternalDatabase ed = ExternalDatabase.Factory.newInstance();
        ed.setId( externalDatabaseValueObject.getId() );
        ed.setName( externalDatabaseValueObject.getName() );
        ed.setDescription( externalDatabaseValueObject.getDescription() );
        ed.setWebUri( externalDatabaseValueObject.getUri() );
        ed.setReleaseUrl( externalDatabaseValueObject.getReleaseUrl() );
        ed.setReleaseVersion( externalDatabaseValueObject.getReleaseVersion() );
        ed.setLastUpdated( externalDatabaseValueObject.getLastUpdated() );
        ed.setType( type );
        for ( ExternalDatabaseValueObject subDatabase : externalDatabaseValueObject.getExternalDatabases() ) {
            ed.getExternalDatabases().add( edvo2ed.computeIfAbsent( subDatabase, sd -> toExternalDatabase( sd, edvo2ed, type ) ) );
        }
        return ed;
    }
}
