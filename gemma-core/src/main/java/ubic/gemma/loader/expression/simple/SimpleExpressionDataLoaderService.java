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
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.basecode.dataStructure.matrix.DoubleMatrixNamed;
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
    public DoubleMatrixNamed parse( InputStream data ) throws IOException {
        DoubleMatrixReader reader = new DoubleMatrixReader();
        return ( DoubleMatrixNamed ) reader.read( data );
    }

    /**
     * @param metaData
     * @param matrix
     * @return ExpressionExperiment
     */
    public ExpressionExperiment convert( SimpleExpressionExperimentMetaData metaData, DoubleMatrixNamed matrix ) {
        if ( matrix == null || metaData == null ) {
            throw new IllegalArgumentException( "One or all of method arguments was null" );
        }
        Taxon taxon = convertTaxon( metaData.getTaxon() );

        ArrayDesign arrayDesign = convertArrayDesign( metaData, matrix );

        ExpressionExperiment experiment = ExpressionExperiment.Factory.newInstance();
        experiment.setName( metaData.getName() );
        experiment.setDescription( metaData.getDescription() );
        experiment.setSource( "Import via matrix flat file" );

        if ( metaData.getPubMedId() != null ) {
            PubMedXMLFetcher pubfetch = new PubMedXMLFetcher();
            BibliographicReference ref = pubfetch.retrieveByHTTP( metaData.getPubMedId() );
            experiment.setPrimaryPublication( ref );
        }

        QuantitationType quantitationType = convertQuantitationType( metaData );
        BioAssayDimension bad = convertBioAssayDimension( experiment, arrayDesign, taxon, matrix );
        Collection<DesignElementDataVector> vectors = convertDesignElementDataVectors( experiment, bad, arrayDesign,
                quantitationType, matrix );
        experiment.setDesignElementDataVectors( vectors );
        experiment.setBioAssays( bad.getBioAssays() );

        return experiment;
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

        DoubleMatrixNamed matrix = parse( data );

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
    private ArrayDesign convertArrayDesign( SimpleExpressionExperimentMetaData metaData, DoubleMatrixNamed matrix ) {
        ArrayDesign arrayDesign = metaData.getArrayDesign();

        ArrayDesign existing = arrayDesignService.find( arrayDesign );

        if ( existing != null ) {
            log.info( "Array Design exists" );
            arrayDesignService.thaw( existing );
            return existing;
        }

        log.info( "Creating new ArrayDesign " + arrayDesign );

        for ( int i = 0; i < matrix.rows(); i++ ) {
            CompositeSequence cs = CompositeSequence.Factory.newInstance();
            cs.setName( matrix.getRowName( i ) );
            cs.setArrayDesign( arrayDesign );
            arrayDesign.getCompositeSequences().add( cs );
        }

        log.info( "New array design has " + arrayDesign.getCompositeSequences().size() + " compositeSequences" );

        return arrayDesign;

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
        result.setScale( metaData.getScale() );
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
            DoubleMatrixNamed matrix ) {

        BioAssayDimension bad = BioAssayDimension.Factory.newInstance();
        bad.setName( "For " + ee.getName() );
        bad.setDescription( "Generated from flat file" );

        for ( int i = 0; i < matrix.columns(); i++ ) {
            String columnName = matrix.getColName( i );

            BioMaterial bioMaterial = BioMaterial.Factory.newInstance();
            bioMaterial.setName( columnName );
            bioMaterial.setSourceTaxon( taxon );
            Collection<BioMaterial> bioMaterials = new HashSet<BioMaterial>();
            bioMaterials.add( bioMaterial );

            BioAssay assay = BioAssay.Factory.newInstance();
            assay.setName( columnName );
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
            QuantitationType quantitationType, DoubleMatrixNamed matrix ) {
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
