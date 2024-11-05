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
import ubic.basecode.dataStructure.matrix.DoubleMatrix;
import ubic.basecode.dataStructure.matrix.DoubleMatrixFactory;
import ubic.basecode.io.reader.DoubleMatrixReader;
import ubic.gemma.core.analysis.preprocess.PreprocessorService;
import ubic.gemma.core.loader.entrez.pubmed.PubMedXMLFetcher;
import ubic.gemma.core.loader.expression.simple.model.SimpleExpressionExperimentMetaData;
import ubic.gemma.model.common.quantitationtype.*;
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
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.persistence.service.genome.taxon.TaxonService;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

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

    @Value("${entrez.efetch.apikey")
    private String ncbiApiKey;

    private PubMedXMLFetcher pubfetch;

    @Override
    public void afterPropertiesSet() throws Exception {
        pubfetch = new PubMedXMLFetcher( ncbiApiKey );
    }

    @Override
    public ExpressionExperiment convert( SimpleExpressionExperimentMetaData metaData,
            DoubleMatrix<String, String> matrix ) {
        if ( matrix == null || metaData == null ) {
            throw new IllegalArgumentException( "One or all of method arguments was null" );
        }

        ExpressionExperiment experiment = ExpressionExperiment.Factory.newInstance();

        Taxon taxon = this.convertTaxon( metaData.getTaxon() );

        experiment.setName( metaData.getName() );
        experiment.setShortName( metaData.getShortName() );
        experiment.setDescription( metaData.getDescription() );
        experiment.setTaxon( taxon );

        experiment.setSource( "Import via matrix flat file." + ( StringUtils.isBlank( metaData.getSourceUrl() ) ?
                "" :
                "Downloaded from " + metaData.getSourceUrl() ) );

        ExperimentalDesign ed = ExperimentalDesign.Factory.newInstance();
        experiment.setExperimentalDesign( ed );

        if ( metaData.getPubMedId() != null ) {
            try {
                experiment.setPrimaryPublication( pubfetch.retrieveByHTTP( metaData.getPubMedId() ) );
            } catch ( IOException e ) {
                log.error( "Failed to retrieve PubMed entry for " + metaData.getPubMedId() + ", the primary publication will not be updated.", e );
            }
        }

        QuantitationType quantitationType = this.convertQuantitationType( metaData );

        /* set the quantitation types on the experiment */
        Set<QuantitationType> qTypes = new HashSet<>();
        qTypes.add( quantitationType );
        experiment.setQuantitationTypes( qTypes );

        Collection<ArrayDesign> arrayDesigns = this.convertArrayDesigns( metaData, matrix );

        // Divide up multiple array designs into multiple BioAssayDimensions.
        Set<RawExpressionDataVector> allVectors = new HashSet<>();
        Set<BioAssay> allBioAssays = new HashSet<>();
        Set<Object> usedDesignElements = new HashSet<>();
        for ( ArrayDesign design : arrayDesigns ) {
            SimpleExpressionDataLoaderServiceImpl.log.info( "Processing " + design );
            DoubleMatrix<String, String> subMatrix = this
                    .getSubMatrixForArrayDesign( matrix, usedDesignElements, design );

            if ( subMatrix == null ) {
                throw new IllegalStateException( "Got a null matix" );
            }

            BioAssayDimension bad = this.convertBioAssayDimension( experiment, design, taxon, subMatrix );
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

        return experiment;
    }

    @Override
    public DoubleMatrix<String, String> getSubMatrixForArrayDesign( DoubleMatrix<String, String> matrix,
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

    @Override
    public ExpressionExperiment create( SimpleExpressionExperimentMetaData metaData, InputStream data )
            throws IOException {

        DoubleMatrix<String, String> matrix = this.parse( data );

        return this.create( metaData, matrix );
    }

    @Override
    public DoubleMatrix<String, String> parse( InputStream data ) throws IOException {
        DoubleMatrixReader reader = new DoubleMatrixReader();
        return reader.read( data );
    }

    /**
     * For use in tests.
     */
    private ExpressionExperiment create( SimpleExpressionExperimentMetaData metaData,
            DoubleMatrix<String, String> matrix ) {
        ExpressionExperiment experiment = this.convert( metaData, matrix );

        experiment = persisterHelper.persist( experiment, persisterHelper.prepare( experiment ) );

        assert experiment.getShortName() != null;

        preprocessorService.process( experiment, true, true );

        return experiment;
    }

    private Collection<ArrayDesign> convertArrayDesigns( SimpleExpressionExperimentMetaData metaData,
            DoubleMatrix<String, String> matrix ) {
        Collection<ArrayDesign> arrayDesigns = metaData.getArrayDesigns();

        Collection<ArrayDesign> existingDesigns = new HashSet<>();

        ArrayDesign newDesign = null;

        for ( ArrayDesign design : arrayDesigns ) {
            ArrayDesign existing = null;
            if ( arrayDesignService != null ) {
                // not sure why we need a thaw here, if it's not persistent...must check first anyway to avoid errors.
                if ( design.getId() != null )
                    design = arrayDesignService.thawLite( design );
                existing = arrayDesignService.find( design );
            }
            if ( existing != null ) {
                SimpleExpressionDataLoaderServiceImpl.log.info( "Array Design exists" );
                if ( arrayDesignService != null ) {
                    // FIXME: use thaw for collection of platforms
                    existing = arrayDesignService.thaw( existing );
                }
                existingDesigns.add( existing );
            } else {
                if ( newDesign != null ) {
                    throw new IllegalArgumentException( "More than one of the array designs isn't in the system" );
                }
                newDesign = design;
            }
        }

        if ( newDesign != null ) {
            this.newArrayDesign( matrix, newDesign, metaData.isProbeIdsAreImageClones(), metaData.getTaxon() );
            existingDesigns.add( newDesign );
        }

        assert existingDesigns.size() > 0;

        return existingDesigns;

    }

    /**
     * @return BioAssayDimension
     */
    private BioAssayDimension convertBioAssayDimension( ExpressionExperiment ee, ArrayDesign arrayDesign, Taxon taxon,
            DoubleMatrix<String, String> matrix ) {

        BioAssayDimension bad = BioAssayDimension.Factory.newInstance();
        bad.setName( "For " + ee.getShortName() );
        bad.setDescription( "Generated from flat file" );
        for ( int i = 0; i < matrix.columns(); i++ ) {
            String columnName = matrix.getColName( i );

            BioMaterial bioMaterial = BioMaterial.Factory.newInstance();
            bioMaterial.setName( columnName );
            bioMaterial.setDescription( "Generated by Gemma for: " + ee.getShortName() );
            bioMaterial.setSourceTaxon( taxon );

            BioAssay assay = BioAssay.Factory.newInstance();
            assay.setName( columnName );
            assay.setArrayDesignUsed( arrayDesign );
            assay.setSampleUsed( bioMaterial );
            assay.setIsOutlier( false );
            assay.setSequencePairedReads( false );
            bad.getBioAssays().add( assay );
        }

        SimpleExpressionDataLoaderServiceImpl.log.info( "Generated " + bad.getBioAssays().size() + " bioAssays" );

        return bad;
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

    private QuantitationType convertQuantitationType( SimpleExpressionExperimentMetaData metaData ) {
        QuantitationType result = QuantitationType.Factory.newInstance();

        result.setGeneralType( GeneralType.QUANTITATIVE );
        result.setRepresentation( PrimitiveType.DOUBLE ); // no choice here
        result.setIsPreferred( Boolean.TRUE );
        result.setIsNormalized( Boolean.TRUE );
        result.setIsBackgroundSubtracted( Boolean.TRUE );
        result.setIsBackground( false );
        result.setName( StringUtils.isBlank( metaData.getQuantitationTypeName() ) ?
                "QT for " + metaData.getShortName() :
                metaData.getQuantitationTypeName() );
        result.setDescription( metaData.getQuantitationTypeDescription() );
        result.setType( metaData.getType() == null ? StandardQuantitationType.AMOUNT : metaData.getType() );
        result.setIsMaskedPreferred( metaData.getIsMaskedPreferred() );

        result.setScale( metaData.getScale() == null ? ScaleType.LINEAR : metaData.getScale() );
        result.setIsRatio( metaData.getIsRatio() );

        result.setIsBatchCorrected( false );
        result.setIsRecomputedFromRawData( false );

        return result;
    }

    private Taxon convertTaxon( Taxon taxon ) {
        if ( taxon == null )
            throw new IllegalArgumentException( "Taxon cannot be null" );
        if ( taxonService == null ) {
            return taxon; // for tests
        }
        return taxonService.findOrCreate( taxon );

    }

    private void newArrayDesign( DoubleMatrix<String, String> matrix, ArrayDesign newDesign,
            boolean probeNamesAreImageClones, Taxon taxon ) {
        SimpleExpressionDataLoaderServiceImpl.log.info( "Creating new platform " + newDesign );

        for ( int i = 0; i < matrix.rows(); i++ ) {
            CompositeSequence cs = CompositeSequence.Factory.newInstance();
            cs.setName( matrix.getRowName( i ) );
            cs.setArrayDesign( newDesign );

            if ( probeNamesAreImageClones ) {
                this.provideImageClone( cs, taxon );
            }

            newDesign.getCompositeSequences().add( cs );
        }
        SimpleExpressionDataLoaderServiceImpl.log
                .info( "New platform has " + newDesign.getCompositeSequences().size() + " elements" );
    }

    /**
     * This will eventually go - no special IMAGE clone support.
     */
    private void provideImageClone( CompositeSequence cs, Taxon taxon ) {
        BioSequence bs = BioSequence.Factory.newInstance();
        bs.setTaxon( taxon );
        String imageId = cs.getName();
        if ( imageId == null )
            throw new IllegalArgumentException( "ComposisteSequence must have name filled in first" );
        imageId = imageId.replaceFirst( "___\\d$", "" );
        if ( !imageId.startsWith( "IMAGE:" ) ) {
            imageId = "IMAGE:" + imageId;
        }
        assert imageId.matches( "^IMAGE:\\d+$" );
        bs.setName( imageId );
        cs.setBiologicalCharacteristic( bs );
    }

}
