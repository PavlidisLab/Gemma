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

import ubic.basecode.dataStructure.matrix.DoubleMatrix2DNamedFactory;
import ubic.basecode.dataStructure.matrix.DoubleMatrixNamed2D;
import ubic.basecode.io.ByteArrayConverter;
import ubic.basecode.io.reader.DoubleMatrixReader;
import ubic.gemma.loader.entrez.pubmed.PubMedXMLFetcher;
import ubic.gemma.loader.expression.simple.model.SimpleExpressionExperimentMetaData;
import ubic.gemma.model.common.description.BibliographicReference;
import ubic.gemma.model.common.quantitationtype.GeneralType;
import ubic.gemma.model.common.quantitationtype.PrimitiveType;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.TaxonService;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.persistence.PersisterHelper;

/**
 * Convert a simple matrix and some meta-data into an ExpressionExperiment. Used to handle flat file conversion.
 * 
 * @author pavlidis
 * @version $Id$
 * @spring.bean id="simpleExpressionDataLoaderService"
 * @spring.property name="persisterHelper" ref="persisterHelper"
 * @spring.property name="arrayDesignService" ref="arrayDesignService"
 * @spring.property name="taxonService" ref="taxonService"
 */
public class SimpleExpressionDataLoaderService {

    private static Log log = LogFactory.getLog( SimpleExpressionDataLoaderService.class.getName() );

    PersisterHelper persisterHelper;

    ArrayDesignService arrayDesignService;

    TaxonService taxonService;

    /**
     * @param data
     * @return DoubleMatrixNamed
     * @throws IOException
     */
    public DoubleMatrixNamed2D parse( InputStream data ) throws IOException {
        DoubleMatrixReader reader = new DoubleMatrixReader();
        return ( DoubleMatrixNamed2D ) reader.read( data );
    }

    /**
     * @param metaData
     * @param matrix
     * @return ExpressionExperiment
     */
    public ExpressionExperiment convert( SimpleExpressionExperimentMetaData metaData, DoubleMatrixNamed2D matrix ) {
        if ( matrix == null || metaData == null ) {
            throw new IllegalArgumentException( "One or all of method arguments was null" );
        }
        Taxon taxon = convertTaxon( metaData.getTaxon() );

        ExpressionExperiment experiment = ExpressionExperiment.Factory.newInstance();
        experiment.setName( metaData.getName() );
        experiment.setShortName( experiment.getName() );
        experiment.setDescription( metaData.getDescription() );

        experiment
                .setSource( "Import via matrix flat file."
                        + ( StringUtils.isBlank( metaData.getSourceUrl() ) ? "" : "Downloaded from "
                                + metaData.getSourceUrl() ) );

        // ExperimentalDesign ed = ExperimentalDesign.Factory.newInstance();
        // ed.setName( metaData.getExperimentalDesignName() );
        // ed.setDescription( metaData.getExperimentalDesignDescription() );
        // ExperimentalFactor ef = ExperimentalFactor.Factory.newInstance();
        // ef.setName( "Placeholder" );
        // ef.setDescription( "Awaiting curation" );
        // FactorValue fv = FactorValue.Factory.newInstance( ef );
        // fv.setValue( "default" );
        // ef.getFactorValues().add( fv );
        // ed.getExperimentalFactors().add( ef );
        // experiment.setExperimentalDesign( ed );

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
        Collection<DesignElementDataVector> allVectors = new HashSet<DesignElementDataVector>();
        Collection<BioAssay> allBioAssays = new HashSet<BioAssay>();
        Collection<Object> usedDesignElements = new HashSet<Object>();
        for ( ArrayDesign design : arrayDesigns ) {
            log.info( "Processing " + design );
            DoubleMatrixNamed2D subMatrix = getSubMatrixForArrayDesign( matrix, usedDesignElements, design );
            BioAssayDimension bad = convertBioAssayDimension( experiment, design, taxon, subMatrix );
            Collection<DesignElementDataVector> vectors = convertDesignElementDataVectors( experiment, bad, design,
                    quantitationType, subMatrix );
            allVectors.addAll( vectors );
            allBioAssays.addAll( bad.getBioAssays() );
        }

        // sanity
        if ( usedDesignElements.size() != matrix.rows() ) {
            log.warn( "Some rows of matrix were not matched to any of the given array designs (" + matrix.rows()
                    + " rows, " + usedDesignElements.size() + " found" );
        }

        experiment.setDesignElementDataVectors( allVectors );
        experiment.setBioAssays( allBioAssays );

        return experiment;
    }

    /**
     * @param matrix
     * @param usedDesignElements
     * @param design
     * @return
     */
    private DoubleMatrixNamed2D getSubMatrixForArrayDesign( DoubleMatrixNamed2D matrix,
            Collection<Object> usedDesignElements, ArrayDesign design ) {
        List<Object> designElements = new ArrayList<Object>();
        List<Object> columnNames = new ArrayList<Object>();

        for ( Object originalColumnName : matrix.getColNames() ) {
            columnNames.add( originalColumnName + " on " + design.getName() );
        }

        List<double[]> rows = new ArrayList<double[]>();

        Collection<Object> arrayDesignElementNames = new HashSet<Object>();
        for ( CompositeSequence cs : design.getCompositeSequences() ) {
            arrayDesignElementNames.add( cs.getName() );
        }

        for ( Object object : matrix.getRowNames() ) {
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

        log.info( "Found " + rows.size() + " data rows for " + design );

        if ( rows.size() == 0 ) {
            throw new RuntimeException( "An array design was entered ( " + design
                    + " ) for which there are no matching rows in the data" );
        }

        double[][] allSubMatrixRows = new double[rows.size()][rows.iterator().next().length];
        rows.toArray( allSubMatrixRows );

        DoubleMatrixNamed2D subMatrix = DoubleMatrix2DNamedFactory.fastrow( allSubMatrixRows );
        subMatrix.setRowNames( designElements );
        subMatrix.setColumnNames( columnNames );
        return subMatrix;
    }

    /**
     * Parses, converts (into Gemma objects), and loads data into the database.
     * 
     * @param metaData
     * @param data tab-delimited file with row names corresponding to CompositeSequence names and column names
     *        corresponding to BioAssay names.
     * @return
     * @throws IOException
     */
    public ExpressionExperiment load( SimpleExpressionExperimentMetaData metaData, InputStream data )
            throws IOException {

        DoubleMatrixNamed2D matrix = parse( data );

        ExpressionExperiment experiment = convert( metaData, matrix );

        return ( ExpressionExperiment ) persisterHelper.persist( experiment );
    }

    /**
     * @param taxonName
     * @return
     */
    private Taxon convertTaxon( Taxon taxon ) {
        return taxonService.findOrCreate( taxon );
    }

    /**
     * @param metaData
     * @param matrix
     * @return
     */
    private Collection<ArrayDesign> convertArrayDesigns( SimpleExpressionExperimentMetaData metaData,
            DoubleMatrixNamed2D matrix ) {
        Collection<ArrayDesign> arrayDesigns = metaData.getArrayDesigns();

        Collection<ArrayDesign> existingDesigns = new HashSet<ArrayDesign>();
        ArrayDesign newDesign = null;

        for ( ArrayDesign design : arrayDesigns ) {
            if ( design != null ) arrayDesignService.thaw( design );
            ArrayDesign existing = arrayDesignService.find( design );
            if ( existing != null ) {
                log.info( "Array Design exists" );
                arrayDesignService.thaw( existing );
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
     * @param matrix
     * @param newDesign
     */
    private void newArrayDesign( DoubleMatrixNamed2D matrix, ArrayDesign newDesign, boolean probeNamesAreImageClones,
            Taxon taxon ) {
        log.info( "Creating new ArrayDesign " + newDesign );

        for ( int i = 0; i < matrix.rows(); i++ ) {
            CompositeSequence cs = CompositeSequence.Factory.newInstance();
            cs.setName( matrix.getRowName( i ).toString() );
            cs.setArrayDesign( newDesign );

            if ( probeNamesAreImageClones ) {
                provideImageClone( cs, taxon );
            }

            newDesign.getCompositeSequences().add( cs );
        }
        log.info( "New array design has " + newDesign.getCompositeSequences().size() + " compositeSequences" );
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
     * @param metaData
     * @return
     */
    private QuantitationType convertQuantitationType( SimpleExpressionExperimentMetaData metaData ) {
        QuantitationType result = QuantitationType.Factory.newInstance();
        result.setName( metaData.getQuantitationTypeName() );
        result.setDescription( metaData.getQuantitationTypeDescription() );
        result.setGeneralType( GeneralType.QUANTITATIVE );
        result.setType( metaData.getType() );
        result.setRepresentation( PrimitiveType.DOUBLE ); // no choice here
        result.setIsPreferred( Boolean.TRUE );
        result.setIsNormalized( Boolean.TRUE );
        result.setIsBackgroundSubtracted( Boolean.TRUE );
        result.setScale( metaData.getScale() );
        result.setIsRatio( metaData.getIsRatio() );
        result.setIsBackground( false );
        return result;
    }

    /**
     * @param ee
     * @param arrayDesign
     * @param taxon
     * @param matrix
     * @return BioAssayDimension
     */
    private BioAssayDimension convertBioAssayDimension( ExpressionExperiment ee, ArrayDesign arrayDesign, Taxon taxon,
            DoubleMatrixNamed2D matrix ) {

        BioAssayDimension bad = BioAssayDimension.Factory.newInstance();
        bad.setName( "For " + ee.getName() );
        bad.setDescription( "Generated from flat file" );

        // FactorValue factorValue = ee.getExperimentalDesign().getExperimentalFactors().iterator().next()
        // .getFactorValues().iterator().next();

        for ( int i = 0; i < matrix.columns(); i++ ) {
            Object columnName = matrix.getColName( i );

            BioMaterial bioMaterial = BioMaterial.Factory.newInstance();
            bioMaterial.setName( columnName.toString() );
            bioMaterial.setSourceTaxon( taxon );
            // bioMaterial.getFactorValues().add( factorValue );
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
    private Collection<DesignElementDataVector> convertDesignElementDataVectors(
            ExpressionExperiment expressionExperiment, BioAssayDimension bioAssayDimension, ArrayDesign arrayDesign,
            QuantitationType quantitationType, DoubleMatrixNamed2D matrix ) {
        ByteArrayConverter bArrayConverter = new ByteArrayConverter();

        Collection<DesignElementDataVector> vectors = new HashSet<DesignElementDataVector>();

        Map<String, CompositeSequence> csMap = new HashMap<String, CompositeSequence>();
        for ( CompositeSequence cs : arrayDesign.getCompositeSequences() ) {
            csMap.put( cs.getName(), cs );
        }

        for ( int i = 0; i < matrix.rows(); i++ ) {
            byte[] bdata = bArrayConverter.doubleArrayToBytes( matrix.getRow( i ) );

            DesignElementDataVector vector = DesignElementDataVector.Factory.newInstance();
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
     * @param arrayDesignService the arrayDesignService to set
     */
    public void setArrayDesignService( ArrayDesignService arrayDesignService ) {
        this.arrayDesignService = arrayDesignService;
    }

    /**
     * @param persisterHelper the persisterHelper to set
     */
    public void setPersisterHelper( PersisterHelper persisterHelper ) {
        this.persisterHelper = persisterHelper;
    }

    /**
     * @param taxonService the taxonService to set
     */
    public void setTaxonService( TaxonService taxonService ) {
        this.taxonService = taxonService;
    }

}
