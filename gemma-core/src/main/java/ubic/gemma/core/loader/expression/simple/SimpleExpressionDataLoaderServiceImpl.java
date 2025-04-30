/*
 * The Gemma project
 *
 * Copyright (c) 2006 University of British Columbia
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
package ubic.gemma.core.loader.expression.simple;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import ubic.basecode.dataStructure.matrix.DoubleMatrix;
import ubic.basecode.dataStructure.matrix.DoubleMatrixFactory;
import ubic.gemma.core.analysis.preprocess.PreprocessorService;
import ubic.gemma.core.loader.entrez.pubmed.PubMedSearch;
import ubic.gemma.core.loader.expression.simple.model.*;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.common.quantitationtype.GeneralType;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.common.quantitationtype.ScaleType;
import ubic.gemma.model.common.quantitationtype.StandardQuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.RawExpressionDataVector;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExperimentalDesign;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.persistence.persister.PersisterHelper;
import ubic.gemma.persistence.service.common.description.ExternalDatabaseService;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.persistence.service.genome.taxon.TaxonService;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

/**
 * Convert a simple matrix and some meta-data into an ExpressionExperiment. Used to handle flat file conversion.
 *
 * @author pavlidis
 */
@Component
public class SimpleExpressionDataLoaderServiceImpl implements SimpleExpressionDataLoaderService, InitializingBean {

    private static final Log log = LogFactory.getLog( SimpleExpressionDataLoaderServiceImpl.class.getName() );

    @Autowired
    private ArrayDesignService arrayDesignService;
    @Autowired
    private PersisterHelper persisterHelper;
    @Autowired
    private PreprocessorService preprocessorService;
    @Autowired
    private TaxonService taxonService;
    @Autowired
    private ExternalDatabaseService externalDatabaseService;

    @Value("${entrez.efetch.apikey}")
    private String ncbiApiKey;

    private PubMedSearch pubfetch;

    @Override
    public void afterPropertiesSet() throws Exception {
        pubfetch = new PubMedSearch( ncbiApiKey );
    }

    @Override
    public ExpressionExperiment create( SimpleExpressionExperimentMetadata metaData, @Nullable DoubleMatrix<String, String> matrix ) {
        ExpressionExperiment experiment = this.convert( metaData, matrix );
        experiment = persisterHelper.persist( experiment, persisterHelper.prepare( experiment ) );
        if ( matrix != null && metaData.getQuantitationType() != null && metaData.getQuantitationType().getIsPreferred() ) {
            log.info( experiment + " has preferred raw data vectors, preprocessing it..." );
            preprocessorService.process( experiment, true, true );
        }
        return experiment;
    }

    @Override
    public ExpressionExperiment convert( SimpleExpressionExperimentMetadata metaData, @Nullable DoubleMatrix<String, String> matrix ) {
        Assert.notNull( metaData );

        ExpressionExperiment experiment = ExpressionExperiment.Factory.newInstance();

        Taxon taxon = this.convertTaxon( metaData.getTaxon() );

        experiment.setName( requireNonNull( metaData.getName(), "No name set." ) );
        experiment.setShortName( requireNonNull( metaData.getShortName(), "No short name set." ) );
        experiment.setDescription( metaData.getDescription() );
        experiment.setTaxon( taxon );

        experiment.setSource( "Import via matrix flat file." + ( StringUtils.isBlank( metaData.getSource() ) ?
                "" :
                "Downloaded from " + metaData.getSource() ) );

        if ( metaData.getAccession() != null ) {
            experiment.setAccession( convertDatabaseEntry( metaData.getAccession() ) );
        }

        ExperimentalDesign ed = ExperimentalDesign.Factory.newInstance();
        experiment.setExperimentalDesign( ed );

        if ( metaData.getPubMedId() != null ) {
            try {
                experiment.setPrimaryPublication( pubfetch.fetchById( metaData.getPubMedId() ) );
            } catch ( IOException e ) {
                log.error( "Failed to retrieve PubMed entry for " + metaData.getPubMedId() + ", the primary publication will not be updated.", e );
            }
        }

        Collection<ArrayDesign> arrayDesigns = this.convertArrayDesigns( metaData, matrix, taxon );
        Set<BioAssay> assaysFromEE;
        if ( metaData.getSamples() != null && !metaData.getSamples().isEmpty() ) {
            assaysFromEE = convertSamples( metaData.getSamples(), experiment, taxon, arrayDesigns );
        } else {
            assaysFromEE = null;
        }

        if ( matrix != null ) {
            if ( metaData.getQuantitationType() == null ) {
                throw new IllegalArgumentException( "Quantitation metadata must be populated if a data matrix is provided." );
            }
            QuantitationType quantitationType = this.convertQuantitationType( metaData.getQuantitationType(), metaData.getShortName() );
            experiment.getQuantitationTypes().add( quantitationType );

            // Divide up multiple array designs into multiple BioAssayDimensions.
            Set<RawExpressionDataVector> allVectors = new HashSet<>();
            Set<BioAssay> allBioAssays = new HashSet<>();
            Set<Object> usedDesignElements = new HashSet<>();
            for ( ArrayDesign design : arrayDesigns ) {
                SimpleExpressionDataLoaderServiceImpl.log.info( "Processing " + design );
                DoubleMatrix<String, String> subMatrix = this
                        .getSubMatrixForArrayDesign( matrix, usedDesignElements, design );

                if ( subMatrix == null ) {
                    throw new IllegalStateException( "Got a null matrix" );
                }

                BioAssayDimension bad = this.convertBioAssayDimension( experiment, design, taxon, subMatrix, assaysFromEE );
                Collection<RawExpressionDataVector> vectors = this
                        .convertDesignElementDataVectors( experiment, bad, design, quantitationType, subMatrix );
                allVectors.addAll( vectors );
                allBioAssays.addAll( bad.getBioAssays() );
            }

            // sanity
            if ( usedDesignElements.size() != matrix.rows() ) {
                SimpleExpressionDataLoaderServiceImpl.log
                        .warn( "Some rows of matrix were not matched to any of the given platforms (" + matrix.rows()
                                + " rows, " + usedDesignElements.size() + " found" );
            }

            experiment.setRawExpressionDataVectors( allVectors );
            experiment.setBioAssays( allBioAssays );
            experiment.setNumberOfSamples( allBioAssays.size() );
        } else if ( assaysFromEE != null ) {
            experiment.setBioAssays( assaysFromEE );
            experiment.setNumberOfSamples( assaysFromEE.size() );
        } else {
            throw new IllegalArgumentException( "At least one of the following must be provided: a data matrix or sample metadata." );
        }

        return experiment;
    }

    private Collection<ArrayDesign> convertArrayDesigns( SimpleExpressionExperimentMetadata metaData,
            @Nullable DoubleMatrix<String, String> matrix, Taxon taxon ) {
        Assert.isTrue( !metaData.getArrayDesigns().isEmpty(), "At least one platform is required." );
        Assert.isTrue( matrix == null || !metaData.isProbeIdsAreImageClones(),
                "Cannot create image clones if no data matrix is supplied." );
        Collection<SimplePlatformMetadata> arrayDesigns = metaData.getArrayDesigns();
        Collection<ArrayDesign> platforms = new HashSet<>();
        for ( SimplePlatformMetadata design : arrayDesigns ) {
            platforms.add( convertArrayDesign( design, taxon, matrix, metaData.isProbeIdsAreImageClones() ) );
        }
        if ( matrix != null ) {
            long numberOfNewPlatforms = platforms.stream().filter( ad -> ad.getId() == null ).count();
            if ( numberOfNewPlatforms > 1 ) {
                throw new IllegalArgumentException( "At most one new platform can be created when supplying a data matrix." );
            }
        }
        return platforms;
    }

    private ArrayDesign convertArrayDesign( SimplePlatformMetadata design, Taxon taxon, @Nullable DoubleMatrix<String, String> matrix,
            boolean probeNamesAreImageClones ) {
        ArrayDesign existing;
        // not sure why we need a thaw here, if it's not persistent...must check first anyway to avoid errors.
        if ( design.getId() != null ) {
            existing = arrayDesignService.loadOrFail( design.getId() );
        } else if ( design.getShortName() != null ) {
            // there might be no platform with that short name, which is OK since we'll create a new one
            existing = arrayDesignService.findByShortName( design.getShortName() );
        } else if ( design.getName() != null ) {
            Collection<ArrayDesign> found = arrayDesignService.findByName( design.getName() );
            if ( found.size() == 1 ) {
                existing = found.iterator().next();
            } else if ( found.size() > 1 ) {
                throw new IllegalArgumentException( "More than oen platform with name " + design.getName() + " found. Please use ID or short name to specify the platform." );
            } else {
                existing = null;
            }
        } else {
            throw new IllegalArgumentException( "No suitable identifier for finding or creating a platform: " + design + ". Assign at least a short name." );
        }
        if ( existing != null ) {
            SimpleExpressionDataLoaderServiceImpl.log.info( "Platform for " + design + " exists, thawing it fully..." );
            existing = arrayDesignService.thaw( existing );
            return existing;
        } else if ( matrix != null ) {
            SimpleExpressionDataLoaderServiceImpl.log.info( "No platform found for " + design + ", creating a new one." );
            return this.convertArrayDesignFromDataMatrix( design, taxon, matrix, probeNamesAreImageClones );
        } else {
            throw new IllegalArgumentException( "No array design found for " + design + " and no data matrix is provided to pre-populate one." );
        }
    }

    private ArrayDesign convertArrayDesignFromDataMatrix( SimplePlatformMetadata design, Taxon taxon, DoubleMatrix<String, String> matrix,
            boolean probeNamesAreImageClones ) {
        SimpleExpressionDataLoaderServiceImpl.log.info( "Creating new platform from " + design );
        ArrayDesign newDesign = new ArrayDesign();
        newDesign.setShortName( requireNonNull( design.getShortName(), "No short name set." ) );
        newDesign.setName( requireNonNull( design.getName(), "No name set." ) );
        newDesign.setPrimaryTaxon( taxon );
        newDesign.setTechnologyType( requireNonNull( design.getTechnologyType(), "No technology type set." ) );
        for ( int i = 0; i < matrix.rows(); i++ ) {
            CompositeSequence cs = CompositeSequence.Factory.newInstance();
            cs.setName( matrix.getRowName( i ) );
            cs.setArrayDesign( newDesign );
            if ( probeNamesAreImageClones ) {
                this.populateImageClone( cs, taxon );
            }
            newDesign.getCompositeSequences().add( cs );
        }
        SimpleExpressionDataLoaderServiceImpl.log
                .info( "New platform has " + newDesign.getCompositeSequences().size() + " elements" );
        return newDesign;
    }

    /**
     * This will eventually go - no special IMAGE clone support.
     */
    private void populateImageClone( CompositeSequence cs, Taxon taxon ) {
        BioSequence bs = BioSequence.Factory.newInstance();
        bs.setTaxon( taxon );
        String imageId = cs.getName();
        if ( imageId == null )
            throw new IllegalArgumentException( "CompositeSequence must have name filled in first" );
        imageId = imageId.replaceFirst( "___\\d$", "" );
        if ( !imageId.startsWith( "IMAGE:" ) ) {
            imageId = "IMAGE:" + imageId;
        }
        assert imageId.matches( "^IMAGE:\\d+$" );
        bs.setName( imageId );
        cs.setBiologicalCharacteristic( bs );
    }

    private Set<BioAssay> convertSamples( Collection<SimpleSampleMetadata> samples, ExpressionExperiment experiment, Taxon taxon, Collection<ArrayDesign> arrayDesigns ) {
        Map<Long, ArrayDesign> arrayDesignsById = new HashMap<>();
        Map<String, ArrayDesign> arrayDesignsByShortName = new HashMap<>();
        Map<String, ArrayDesign> arrayDesignsByName = new HashMap<>();
        for ( ArrayDesign ad : arrayDesigns ) {
            if ( ad.getId() != null ) {
                arrayDesignsById.put( ad.getId(), ad );
            }
            if ( ad.getShortName() != null ) {
                arrayDesignsByShortName.put( ad.getShortName(), ad );
            }
            if ( ad.getName() != null ) {
                arrayDesignsByName.put( ad.getName(), ad );
            }
        }
        return samples.stream()
                .map( s -> convertSample( s, experiment, taxon, arrayDesignsById, arrayDesignsByShortName, arrayDesignsByName ) )
                .collect( Collectors.toSet() );
    }

    private BioAssay convertSample( SimpleSampleMetadata sampleMetaData, ExpressionExperiment experiment, Taxon taxon, Map<Long, ArrayDesign> arrayDesignsById, Map<String, ArrayDesign> arrayDesignsByShortName, Map<String, ArrayDesign> arrayDesignsByName ) {
        BioMaterial bioMaterial = BioMaterial.Factory.newInstance();
        bioMaterial.setName( sampleMetaData.getName() );
        bioMaterial.setDescription( "Generated by Gemma for: " + experiment.getShortName() );
        bioMaterial.setSourceTaxon( taxon );
        bioMaterial.setCharacteristics( convertCharacteristics( sampleMetaData.getCharacteristics() ) );
        BioAssay bioAssay = BioAssay.Factory.newInstance();
        bioAssay.setArrayDesignUsed( pickArrayDesign( sampleMetaData.getPlatformUsed(), arrayDesignsById, arrayDesignsByShortName, arrayDesignsByName ) );
        bioAssay.setName( sampleMetaData.getName() );
        bioAssay.setDescription( sampleMetaData.getDescription() );
        if ( sampleMetaData.getAccession() != null ) {
            bioAssay.setAccession( convertDatabaseEntry( sampleMetaData.getAccession() ) );
        }
        bioAssay.setSampleUsed( bioMaterial );
        bioMaterial.getBioAssaysUsedIn().add( bioAssay );
        return bioAssay;
    }

    private ArrayDesign pickArrayDesign( SimplePlatformMetadata platformMetadata, Map<Long, ArrayDesign> arrayDesignsById, Map<String, ArrayDesign> arrayDesignsByShortName, Map<String, ArrayDesign> arrayDesignsByName ) {
        if ( platformMetadata.getId() != null ) {
            return requireNonNull( arrayDesignsById.get( platformMetadata.getId() ),
                    "No platform with ID " + platformMetadata.getId() );
        } else if ( platformMetadata.getShortName() != null ) {
            return requireNonNull( arrayDesignsByShortName.get( platformMetadata.getShortName() ),
                    "No platform with short name " + platformMetadata.getShortName() );
        } else if ( platformMetadata.getName() != null ) {
            return requireNonNull( arrayDesignsByName.get( platformMetadata.getName() ),
                    "No platform with name " + platformMetadata.getName() );
        } else {
            throw new IllegalArgumentException( "No suitable identifier for finding " + platformMetadata + "." );
        }
    }

    private DatabaseEntry convertDatabaseEntry( SimpleDatabaseEntry accession ) {
        Assert.isTrue( StringUtils.isNotBlank( accession.getAccession() ), "Accession cannot be blank." );
        ExternalDatabase ed;
        if ( accession.getExternalDatabaseId() != null ) {
            ed = requireNonNull( externalDatabaseService.load( accession.getExternalDatabaseId() ),
                    "No ExternalDatabase with ID " + accession.getExternalDatabaseId() );
        } else if ( accession.getExternalDatabaseName() != null ) {
            ed = requireNonNull( externalDatabaseService.findByName( accession.getExternalDatabaseName() ),
                    "No ExternalDatabase with name " + accession.getExternalDatabaseName() );
        } else {
            throw new IllegalArgumentException( "At least one external database identifier must be provided for " + accession + ". " );
        }
        return DatabaseEntry.Factory.newInstance( accession.getAccession(), ed );
    }

    private Set<Characteristic> convertCharacteristics( Collection<SimpleCharacteristic> characteristics ) {
        return characteristics.stream()
                .map( c -> Characteristic.Factory.newInstance( c.getCategory(), c.getCategoryUri(), c.getValue(), c.getValueUri() ) )
                .collect( Collectors.toSet() );
    }

    private DoubleMatrix<String, String> getSubMatrixForArrayDesign( DoubleMatrix<String, String> matrix,
            Collection<Object> usedDesignElements, ArrayDesign design ) {
        List<String> designElements = new ArrayList<>();

        List<String> columnNames = new ArrayList<>( matrix.getColNames() );

        List<double[]> rows = new ArrayList<>();

        Collection<Object> arrayDesignElementNames = new HashSet<>();
        for ( CompositeSequence cs : design.getCompositeSequences() ) {
            arrayDesignElementNames.add( cs.getName() );
        }

        for ( String object : matrix.getRowNames() ) {
            /*
             * disallow using design elements more than once; if two array designs match a given row name, we just end
             * up arbitrarily assigning it to one of the array designs.
             */
            if ( arrayDesignElementNames.contains( object ) && !usedDesignElements.contains( object ) ) {
                rows.add( matrix.getRow( matrix.getRowIndexByName( object ) ) );
                usedDesignElements.add( object );
                designElements.add( object );
            }
        }

        if ( usedDesignElements.isEmpty() ) {
            throw new IllegalArgumentException( "No design elements matched?" );
        }

        SimpleExpressionDataLoaderServiceImpl.log.info( "Found " + rows.size() + " data rows for " + design );

        if ( rows.isEmpty() ) {
            SimpleExpressionDataLoaderServiceImpl.log.warn( "A platform was entered ( " + design
                    + " ) for which there are no matching rows in the data" );
            return null;
        }

        double[][] allSubMatrixRows = new double[rows.size()][rows.iterator().next().length];
        rows.toArray( allSubMatrixRows );

        DoubleMatrix<String, String> subMatrix = DoubleMatrixFactory.fastrow( allSubMatrixRows );
        subMatrix.setRowNames( designElements );
        subMatrix.setColumnNames( columnNames );
        return subMatrix;
    }


    private BioAssayDimension convertBioAssayDimension( ExpressionExperiment ee, ArrayDesign arrayDesign, Taxon taxon,
            DoubleMatrix<String, String> matrix, @Nullable Collection<BioAssay> bioAssaysFromEE ) {
        Map<String, BioAssay> assayByName;
        if ( bioAssaysFromEE != null ) {
            assayByName = bioAssaysFromEE.stream()
                    .collect( Collectors.toMap( BioAssay::getName, ba -> ba ) );
        } else {
            assayByName = null;
        }

        List<BioAssay> bioAssays = new ArrayList<>( matrix.columns() );
        for ( int i = 0; i < matrix.columns(); i++ ) {
            String columnName = matrix.getColName( i );
            BioAssay assay;
            if ( bioAssaysFromEE != null ) {
                assay = requireNonNull( assayByName.get( columnName ),
                        "No sample for " + columnName + " found. If you provide sample metadata, every sample mentioned in the data matrix must be declared." );
            } else {
                BioMaterial bioMaterial = BioMaterial.Factory.newInstance();
                bioMaterial.setName( columnName );
                bioMaterial.setDescription( "Generated by Gemma for: " + ee.getShortName() );
                bioMaterial.setSourceTaxon( taxon );
                assay = BioAssay.Factory.newInstance();
                assay.setName( columnName );
                assay.setArrayDesignUsed( arrayDesign );
                assay.setSampleUsed( bioMaterial );
                assay.setIsOutlier( false );
                assay.setSequencePairedReads( false );
                bioMaterial.getBioAssaysUsedIn().add( assay );
            }
            bioAssays.add( assay );
        }

        SimpleExpressionDataLoaderServiceImpl.log.info( "Generated " + bioAssays.size() + " bioAssays" );

        return BioAssayDimension.Factory.newInstance( bioAssays );
    }

    private Collection<RawExpressionDataVector> convertDesignElementDataVectors(
            ExpressionExperiment expressionExperiment, BioAssayDimension bioAssayDimension, ArrayDesign arrayDesign,
            QuantitationType quantitationType, DoubleMatrix<String, String> matrix ) {
        Collection<RawExpressionDataVector> vectors = new HashSet<>();

        Map<String, CompositeSequence> csMap = new HashMap<>();
        for ( CompositeSequence cs : arrayDesign.getCompositeSequences() ) {
            csMap.put( cs.getName(), cs );
        }

        for ( int i = 0; i < matrix.rows(); i++ ) {
            CompositeSequence cs = csMap.get( matrix.getRowName( i ) );
            if ( cs == null ) {
                continue;
            }
            RawExpressionDataVector vector = RawExpressionDataVector.Factory.newInstance();
            vector.setDesignElement( cs );
            vector.setQuantitationType( quantitationType );
            vector.setExpressionExperiment( expressionExperiment );
            vector.setBioAssayDimension( bioAssayDimension );
            vector.setDataAsDoubles( matrix.getRow( i ) );
            vectors.add( vector );

        }
        SimpleExpressionDataLoaderServiceImpl.log.info( "Created " + vectors.size() + " data vectors" );
        return vectors;
    }

    private QuantitationType convertQuantitationType( SimpleQuantitationTypeMetadata metaData, String shortName ) {
        QuantitationType result = QuantitationType.Factory.newInstance();

        result.setName( StringUtils.isNotBlank( metaData.getName() ) ? metaData.getName() : "QT for " + shortName );
        result.setDescription( metaData.getDescription() );
        result.setGeneralType( GeneralType.QUANTITATIVE );
        result.setType( metaData.getType() != null ? metaData.getType() : StandardQuantitationType.AMOUNT );
        result.setRepresentation( metaData.getRepresentation() ); // no choice here
        result.setScale( metaData.getScale() != null ? metaData.getScale() : ScaleType.LINEAR );

        result.setIsNormalized( Boolean.TRUE );
        result.setIsBackgroundSubtracted( Boolean.TRUE );
        result.setIsBackground( false );
        result.setIsPreferred( metaData.getIsPreferred() );
        result.setIsRatio( metaData.getIsRatio() );
        result.setIsBatchCorrected( false );
        result.setIsRecomputedFromRawData( false );

        return result;
    }

    private Taxon convertTaxon( SimpleTaxonMetadata metaData ) {
        if ( metaData.getId() != null ) {
            return requireNonNull( taxonService.load( metaData.getId() ) );
        } else if ( metaData.getNcbiId() != null ) {
            return requireNonNull( taxonService.findByNcbiId( metaData.getNcbiId() ) );
        } else if ( metaData.getName() != null ) {
            Taxon found;
            if ( ( found = taxonService.findByCommonName( metaData.getName() ) ) != null ) {
                return found;
            }
            if ( ( found = taxonService.findByScientificName( metaData.getName() ) ) != null ) {
                return found;
            }
            throw new IllegalArgumentException( "No taxon with common name or scientific name matching " + metaData.getName() + " were found." );
        } else {
            throw new IllegalArgumentException( "At least one taxon identifier must be provided." );
        }
    }
}
