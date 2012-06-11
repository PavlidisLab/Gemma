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
package ubic.gemma.loader.expression.simple;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ubic.basecode.dataStructure.matrix.DoubleMatrixFactory;
import ubic.basecode.dataStructure.matrix.DoubleMatrix;
import ubic.basecode.io.ByteArrayConverter;
import ubic.basecode.io.reader.DoubleMatrixReader;
import ubic.gemma.analysis.preprocess.PreprocessorService;
import ubic.gemma.genome.taxon.service.TaxonService;
import ubic.gemma.loader.entrez.pubmed.PubMedXMLFetcher;
import ubic.gemma.loader.expression.simple.model.SimpleExpressionExperimentMetaData;
import ubic.gemma.model.common.description.BibliographicReference;
import ubic.gemma.model.common.quantitationtype.GeneralType;
import ubic.gemma.model.common.quantitationtype.PrimitiveType;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.common.quantitationtype.ScaleType;
import ubic.gemma.model.common.quantitationtype.StandardQuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.RawExpressionDataVector;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.biomaterial.BioMaterialService;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExperimentalDesign;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.persistence.Persister;

/**
 * Convert a simple matrix and some meta-data into an ExpressionExperiment. Used to handle flat file conversion.
 * 
 * @author pavlidis
 * @version $Id$
 */
@Component
public class SimpleExpressionDataLoaderServiceImpl implements SimpleExpressionDataLoaderService {

    private static Log log = LogFactory.getLog( SimpleExpressionDataLoaderServiceImpl.class.getName() );

    /**
     * This method establishes the way raw input sample names are converted to (hopefully unique) biomaterial names in
     * the system.
     * 
     * @param ee expression experiment the sample belongs to.
     * @param inputSampleName The sample name as identified in the input file - the column header; this is basically the
     *        bio assay name.
     * @return String used to identify the biomaterial in the system.
     */
    public static String makeBioMaterialName( ExpressionExperiment ee, String inputSampleName ) {
        return inputSampleName + "__" + ee.getShortName();
    }

    @Autowired
    ArrayDesignService arrayDesignService;

    @Autowired
    BioMaterialService bioMaterialService;

    @Autowired
    Persister persisterHelper;

    @Autowired
    PreprocessorService preprocessorService;

    @Autowired
    TaxonService taxonService;

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.loader.expression.simple.SimpleExpressionDataLoaderService#convert(ubic.gemma.loader.expression.simple
     * .model.SimpleExpressionExperimentMetaData, ubic.basecode.dataStructure.matrix.DoubleMatrix)
     */
    @Override
    public ExpressionExperiment convert( SimpleExpressionExperimentMetaData metaData,
            DoubleMatrix<String, String> matrix ) {
        if ( matrix == null || metaData == null ) {
            throw new IllegalArgumentException( "One or all of method arguments was null" );
        }

        ExpressionExperiment experiment = ExpressionExperiment.Factory.newInstance();

        Taxon taxon = convertTaxon( metaData.getTaxon() );

        experiment.setName( metaData.getName() );
        experiment.setShortName( metaData.getShortName() );
        experiment.setDescription( metaData.getDescription() );

        experiment
                .setSource( "Import via matrix flat file."
                        + ( StringUtils.isBlank( metaData.getSourceUrl() ) ? "" : "Downloaded from "
                                + metaData.getSourceUrl() ) );

        ExperimentalDesign ed = ExperimentalDesign.Factory.newInstance();
        experiment.setExperimentalDesign( ed );

        if ( metaData.getPubMedId() != null ) {
            PubMedXMLFetcher pubfetch = new PubMedXMLFetcher();
            BibliographicReference ref = pubfetch.retrieveByHTTP( metaData.getPubMedId() );
            experiment.setPrimaryPublication( ref );
        }

        QuantitationType quantitationType = convertQuantitationType( metaData );

        /* set the quantitation types on the experiment */
        Collection<QuantitationType> qTypes = new HashSet<QuantitationType>();
        qTypes.add( quantitationType );
        experiment.setQuantitationTypes( qTypes );

        Collection<ArrayDesign> arrayDesigns = convertArrayDesigns( metaData, matrix );

        // Divide up multiple array designs into multiple BioAssayDimensions.
        Collection<RawExpressionDataVector> allVectors = new HashSet<RawExpressionDataVector>();
        Collection<BioAssay> allBioAssays = new HashSet<BioAssay>();
        Collection<Object> usedDesignElements = new HashSet<Object>();
        for ( ArrayDesign design : arrayDesigns ) {
            log.info( "Processing " + design );
            DoubleMatrix<String, String> subMatrix = getSubMatrixForArrayDesign( matrix, usedDesignElements, design );

            if ( subMatrix == null ) {
                throw new IllegalStateException( "Got a null matix" );
            }

            BioAssayDimension bad = convertBioAssayDimension( experiment, design, taxon, subMatrix );
            Collection<RawExpressionDataVector> vectors = convertDesignElementDataVectors( experiment, bad, design,
                    quantitationType, subMatrix );
            allVectors.addAll( vectors );
            allBioAssays.addAll( bad.getBioAssays() );
        }

        // sanity
        if ( usedDesignElements.size() != matrix.rows() ) {
            log.warn( "Some rows of matrix were not matched to any of the given platforms (" + matrix.rows()
                    + " rows, " + usedDesignElements.size() + " found" );
        }

        experiment.setRawExpressionDataVectors( allVectors );
        experiment.setBioAssays( allBioAssays );

        return experiment;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.loader.expression.simple.SimpleExpressionDataLoaderService#getSubMatrixForArrayDesign(ubic.basecode
     * .dataStructure.matrix.DoubleMatrix, java.util.Collection, ubic.gemma.model.expression.arrayDesign.ArrayDesign)
     */
    @Override
    public DoubleMatrix<String, String> getSubMatrixForArrayDesign( DoubleMatrix<String, String> matrix,
            Collection<Object> usedDesignElements, ArrayDesign design ) {
        List<String> designElements = new ArrayList<String>();
        List<String> columnNames = new ArrayList<String>();

        for ( String originalColumnName : matrix.getColNames() ) {
            columnNames.add( originalColumnName );
        }

        List<double[]> rows = new ArrayList<double[]>();

        Collection<Object> arrayDesignElementNames = new HashSet<Object>();
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

        if ( usedDesignElements.size() == 0 ) {
            throw new IllegalArgumentException( "No design elements matched?" );
        }

        log.info( "Found " + rows.size() + " data rows for " + design );

        if ( rows.size() == 0 ) {
            log.warn( "A platform was entered ( " + design + " ) for which there are no matching rows in the data" );
            return null;
        }

        double[][] allSubMatrixRows = new double[rows.size()][rows.iterator().next().length];
        rows.toArray( allSubMatrixRows );

        DoubleMatrix<String, String> subMatrix = DoubleMatrixFactory.fastrow( allSubMatrixRows );
        subMatrix.setRowNames( designElements );
        subMatrix.setColumnNames( columnNames );
        return subMatrix;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.loader.expression.simple.SimpleExpressionDataLoaderService#create(ubic.gemma.loader.expression.simple
     * .model.SimpleExpressionExperimentMetaData, java.io.InputStream)
     */
    @Override
    public ExpressionExperiment create( SimpleExpressionExperimentMetaData metaData, InputStream data )
            throws IOException {

        DoubleMatrix<String, String> matrix = parse( data );

        return create( metaData, matrix );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.loader.expression.simple.SimpleExpressionDataLoaderService#parse(java.io.InputStream)
     */
    @Override
    public DoubleMatrix<String, String> parse( InputStream data ) throws IOException {
        DoubleMatrixReader reader = new DoubleMatrixReader();
        return reader.read( data );
    }

    /**
     * For use in tests.
     * 
     * @param metaData
     * @param matrix
     * @return
     */
    protected ExpressionExperiment create( SimpleExpressionExperimentMetaData metaData,
            DoubleMatrix<String, String> matrix ) {
        ExpressionExperiment experiment = convert( metaData, matrix );

        validate( experiment );

        experiment = persisterHelper.persist( experiment, persisterHelper.prepare( experiment ) );

        preprocessorService.createProcessedVectors( experiment );

        return experiment;
    }

    /**
     * @param metaData
     * @param matrix
     * @return
     */
    private Collection<ArrayDesign> convertArrayDesigns( SimpleExpressionExperimentMetaData metaData,
            DoubleMatrix<String, String> matrix ) {
        Collection<ArrayDesign> arrayDesigns = metaData.getArrayDesigns();

        Collection<ArrayDesign> existingDesigns = new HashSet<ArrayDesign>();

        ArrayDesign newDesign = null;

        for ( ArrayDesign design : arrayDesigns ) {
            ArrayDesign existing = null;
            if ( arrayDesignService != null ) {
                // not sure why we need a thaw here, if it's not persistent...must check first anyway to avoid errors.
                if ( design.getId() != null ) design = arrayDesignService.thawLite( design );
                existing = arrayDesignService.find( design );
            }
            if ( existing != null ) {
                log.info( "Array Design exists" );
                if ( arrayDesignService != null ) {
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
            newArrayDesign( matrix, newDesign, metaData.isProbeIdsAreImageClones(), metaData.getTaxon() );
            existingDesigns.add( newDesign );
        }

        assert existingDesigns.size() > 0;

        return existingDesigns;

    }

    /**
     * @param ee
     * @param arrayDesign
     * @param taxon
     * @param matrix
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
            bioMaterial.setName( makeBioMaterialName( ee, columnName ) );
            bioMaterial.setSourceTaxon( taxon );
            Collection<BioMaterial> bioMaterials = new HashSet<BioMaterial>();
            bioMaterials.add( bioMaterial );

            BioAssay assay = BioAssay.Factory.newInstance();
            assay.setName( columnName.toString() );
            assay.setArrayDesignUsed( arrayDesign );
            assay.setSamplesUsed( bioMaterials );
            bad.getBioAssays().add( assay );
        }

        log.info( "Created " + bad.getBioAssays().size() + " bioAssays" );

        return bad;
    }

    /**
     * @param expressionExperiment
     * @param bioAssayDimension
     * @param arrayDesign
     * @param quantitationType
     * @param matrix
     * @return Collection<DesignElementDataVector>
     */
    private Collection<RawExpressionDataVector> convertDesignElementDataVectors(
            ExpressionExperiment expressionExperiment, BioAssayDimension bioAssayDimension, ArrayDesign arrayDesign,
            QuantitationType quantitationType, DoubleMatrix<String, String> matrix ) {
        ByteArrayConverter bArrayConverter = new ByteArrayConverter();

        Collection<RawExpressionDataVector> vectors = new HashSet<RawExpressionDataVector>();

        Map<String, CompositeSequence> csMap = new HashMap<String, CompositeSequence>();
        for ( CompositeSequence cs : arrayDesign.getCompositeSequences() ) {
            csMap.put( cs.getName(), cs );
        }

        for ( int i = 0; i < matrix.rows(); i++ ) {
            byte[] bdata = bArrayConverter.doubleArrayToBytes( matrix.getRow( i ) );

            RawExpressionDataVector vector = RawExpressionDataVector.Factory.newInstance();
            vector.setData( bdata );

            CompositeSequence cs = csMap.get( matrix.getRowName( i ) );
            if ( cs == null ) {
                continue;
            }
            vector.setDesignElement( cs );
            vector.setQuantitationType( quantitationType );
            vector.setExpressionExperiment( expressionExperiment );
            vector.setBioAssayDimension( bioAssayDimension );

            vectors.add( vector );

        }
        log.info( "Created " + vectors.size() + " data vectors" );
        return vectors;
    }

    /**
     * @param metaData
     * @return
     */
    private QuantitationType convertQuantitationType( SimpleExpressionExperimentMetaData metaData ) {
        QuantitationType result = QuantitationType.Factory.newInstance();

        result.setGeneralType( GeneralType.QUANTITATIVE );
        result.setRepresentation( PrimitiveType.DOUBLE ); // no choice here
        result.setIsPreferred( Boolean.TRUE );
        result.setIsNormalized( Boolean.TRUE );
        result.setIsBackgroundSubtracted( Boolean.TRUE );
        result.setIsBackground( false );
        result.setName( StringUtils.isBlank( metaData.getQuantitationTypeName() ) ? "QT for " + metaData.getShortName()
                : metaData.getQuantitationTypeName() );
        result.setDescription( metaData.getQuantitationTypeDescription() );
        result.setType( metaData.getType() == null ? StandardQuantitationType.AMOUNT : metaData.getType() );
        result.setIsMaskedPreferred( metaData.getIsMaskedPreferred() );

        result.setScale( metaData.getScale() == null ? ScaleType.LINEAR : metaData.getScale() );
        result.setIsRatio( metaData.getIsRatio() );

        return result;
    }

    /**
     * @param taxonName
     * @return
     */
    private Taxon convertTaxon( Taxon taxon ) {
        if ( taxon == null ) throw new IllegalArgumentException( "Taxon cannot be null" );
        if ( taxonService == null ) {
            return taxon; // for tests
        }
        return taxonService.findOrCreate( taxon );

    }

    /**
     * @param matrix
     * @param newDesign
     */
    private void newArrayDesign( DoubleMatrix<String, String> matrix, ArrayDesign newDesign,
            boolean probeNamesAreImageClones, Taxon taxon ) {
        log.info( "Creating new platform " + newDesign );

        for ( int i = 0; i < matrix.rows(); i++ ) {
            CompositeSequence cs = CompositeSequence.Factory.newInstance();
            cs.setName( matrix.getRowName( i ) );
            cs.setArrayDesign( newDesign );

            if ( probeNamesAreImageClones ) {
                provideImageClone( cs, taxon );
            }

            newDesign.getCompositeSequences().add( cs );
        }
        log.info( "New platform has " + newDesign.getCompositeSequences().size() + " elements" );
    }

    /**
     * @param cs
     * @param taxon
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

    /**
     * Check for some error conditions like biomaterial names matching in the system
     * 
     * @param experiment
     * @throws Exception if there is something wrong
     */
    private void validate( ExpressionExperiment experiment ) {

        for ( BioAssay ba : experiment.getBioAssays() ) {
            for ( BioMaterial bm : ba.getSamplesUsed() ) {

                if ( bioMaterialService.exists( bm ) ) {
                    throw new IllegalArgumentException( "There is already a biomaterial in the system matching: " + bm );
                }

            }
        }

    }

}
