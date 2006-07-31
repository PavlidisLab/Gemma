/*
 * The Gemma project
 * 
 * Copyright (c) 2006 Columbia University
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
package ubic.gemma.expression.experiment;

import java.util.Collection;
import java.util.HashSet;

import org.apache.commons.lang.math.RandomUtils;

import ubic.basecode.io.ByteArrayConverter;
import ubic.gemma.model.common.auditAndSecurity.Contact;
import ubic.gemma.model.common.auditAndSecurity.ContactService;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.common.description.ExternalDatabaseService;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignDao;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.biomaterial.BioMaterialDao;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.designElement.CompositeSequenceService;
import ubic.gemma.model.expression.experiment.ExperimentalDesign;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.TaxonService;
import ubic.gemma.testing.BaseTransactionalSpringContextTest;

/**
 * A utility to load expression experiment test data.
 * 
 * @author keshav
 * @version $Id$
 */
public class ExpressionExperimentTestHelper extends BaseTransactionalSpringContextTest {

    /**
     * @return
     */
    @SuppressWarnings("unchecked")
    private Collection<CompositeSequence> getCompositeSequences() {
        Collection<CompositeSequence> csCol = new HashSet();
        for ( int i = 0; i < testNumCollectionElements; i++ ) {
            CompositeSequence cs = CompositeSequence.Factory.newInstance();
            cs.setName( i + "_at" );
            csCol.add( cs );
        }
        return csCol;
    }

    /**
     * @return
     */
    @SuppressWarnings("unchecked")
    private Collection<ArrayDesign> getArrayDesignsUsed() {
        Collection<ArrayDesign> adCol = new HashSet();
        ArrayDesignService adService = ( ArrayDesignService ) this.getBean( "arrayDesignService" );
        for ( int i = 0; i < testNumCollectionElements; i++ ) {
            ArrayDesign ad = ArrayDesign.Factory.newInstance();
            ad.setName( "Array Design " + i );
            ad.setDescription( i + ": A test array design." );
            ad.setAdvertisedNumberOfDesignElements( i + 100 );
            ad.setCompositeSequences( getCompositeSequences() );

            ad = adService.findOrCreate( ad );

            adCol.add( ad );
        }
        return adCol;
    }

    /**
     * @return
     */
    @SuppressWarnings("unchecked")
    private Collection<BioMaterial> getBioMaterials() {
        BioMaterial bm = BioMaterial.Factory.newInstance();

        Taxon t = Taxon.Factory.newInstance();
        t.setScientificName( "Mus musculus" );
        TaxonService ts = ( TaxonService ) getBean( "taxonService" );
        t = ts.findOrCreate( t );
        bm.setSourceTaxon( t );

        ExternalDatabaseService eds = ( ExternalDatabaseService ) getBean( "externalDatabaseService" );
        ExternalDatabase ed = ExternalDatabase.Factory.newInstance();
        ed.setName( "PubMed" );
        ed = eds.findOrCreate( ed );

        DatabaseEntry de = DatabaseEntry.Factory.newInstance();
        de.setAccession( " Biomaterial accession " );
        de.setExternalDatabase( ed );
        bm.setExternalAccession( de );

        bm.setName( " BioMaterial " );
        bm.setDescription( " A test biomaterial" );

        /*
         * FIXME - change to use the service, not the dao. will not do until merging Gemma V01_MVN1 because I am trying
         * to reduce the number of model changes.
         */
        BioMaterialDao bmDao = ( BioMaterialDao ) this.getBean( "bioMaterialDao" );
        // bmDao.findOrCreate( bm ); - FIXME this is what I want to use, but there is a problem with this:
        // TransientObjectException. The error is consistent. If you have A => B -> C where => is composition and -> is
        // association
        bm = ( BioMaterial ) bmDao.create( bm );
        Collection<BioMaterial> bmCol = new HashSet();
        bmCol.add( bm );
        return bmCol;
    }

    /**
     * @return Collection
     */
    @SuppressWarnings("unchecked")
    private Collection<BioAssay> getBioAssays() {
        Collection<BioAssay> baCol = new HashSet();
        for ( int i = 0; i < testNumCollectionElements; i++ ) {
            BioAssay ba = BioAssay.Factory.newInstance();
            ba.setName( "Bioassay " + i );
            ba.setDescription( i + ": A test bioassay." );
            ba.setSamplesUsed( getBioMaterials() );
            // ba.setArrayDesignsUsed( getArrayDesignsUsed() );

            if ( i < ( testNumCollectionElements - 5 ) ) {
                ExternalDatabaseService eds = ( ExternalDatabaseService ) getBean( "externalDatabaseService" );
                ExternalDatabase ed = ExternalDatabase.Factory.newInstance();
                ed.setName( "PubMed" );
                ed = eds.findOrCreate( ed );

                DatabaseEntry de = DatabaseEntry.Factory.newInstance();
                de.setExternalDatabase( ed );
                de.setAccession( i + ": Accession added from ExpressionExperimentControllerIntegrationTest" );
                ba.setAccession( de );
            }

            baCol.add( ba );
        }

        return baCol;
    }

    /**
     * @return Collection
     */
    @SuppressWarnings("unchecked")
    private Collection<FactorValue> getFactorValues() {
        Collection<FactorValue> fvCol = new HashSet();
        for ( int i = 0; i < testNumCollectionElements; i++ ) {
            FactorValue fv = FactorValue.Factory.newInstance();
            fv.setValue( "Factor value " + i );
            fvCol.add( fv );
        }
        return fvCol;
    }

    /**
     * @return
     */
    @SuppressWarnings("unchecked")
    private Collection<ExperimentalFactor> getExperimentalFactors() {
        Collection<ExperimentalFactor> efCol = new HashSet();
        for ( int i = 0; i < testNumCollectionElements; i++ ) {
            ExperimentalFactor ef = ExperimentalFactor.Factory.newInstance();
            ef.setName( "Experimental Factor " + i );
            ef.setDescription( i + ": A test experimental factor" );
            // FIXME - another
            // OntologyEntry oe = OntologyEntry.Factory.newInstance();
            // oe.setAccession( "oe:" + i );
            // oe.setDescription( "Ontology Entry " + i );
            // log.debug( "ontology entry => experimental factor." );
            // ef.setCategory( oe );
            // ef.setAnnotations(oeCol);
            log.debug( "experimental factor => factor values" );
            ef.setFactorValues( getFactorValues() );
            efCol.add( ef );
        }
        return efCol;
    }

    /**
     * @return
     */
    @SuppressWarnings("unchecked")
    private Collection getExperimentalDesigns() {
        Collection<ExperimentalDesign> edCol = new HashSet();
        for ( int i = 0; i < testNumCollectionElements; i++ ) {
            ExperimentalDesign ed = ExperimentalDesign.Factory.newInstance();
            ed.setName( "Experimental Design " + i );
            ed.setDescription( i + ": A test experimental design." );

            log.debug( "experimental design => experimental factors" );
            ed.setExperimentalFactors( getExperimentalFactors() ); // set test experimental factors

            edCol.add( ed ); // add experimental designs
        }
        return edCol;
    }

    @SuppressWarnings("unchecked")
    private Collection<DesignElementDataVector> getDesignElementDataVectors( ExpressionExperiment ee ) {

        Collection<DesignElementDataVector> vectorCol = new HashSet();
        for ( int i = 0; i < testNumCollectionElements; i++ ) {
            DesignElementDataVector vector = DesignElementDataVector.Factory.newInstance();
            double[] data = new double[testNumCollectionElements / 2];
            for ( int j = 0; j < data.length; j++ ) {
                data[j] = ( double ) RandomUtils.nextDouble();
            }
            ByteArrayConverter bconverter = new ByteArrayConverter();
            byte[] bdata = bconverter.doubleArrayToBytes( data );
            vector.setData( bdata );

            CompositeSequenceService csService = ( CompositeSequenceService ) this.getBean( "compositeSequenceService" );
            CompositeSequence cs = CompositeSequence.Factory.newInstance();
            cs.setName( i + "_at" );
            cs.setDescription( "A test design element " + i + " created from ExpressionExperimentTestHelper" );

            ArrayDesign ad = ArrayDesign.Factory.newInstance();
            ad.setName( "arrayDesign_" + i );
            ArrayDesignDao adDao = ( ArrayDesignDao ) this.getBean( "arrayDesignDao" );
            ad = adDao.findOrCreate( ad );

            cs.setArrayDesign( ad );

            cs = csService.findOrCreate( cs );
            vector.setDesignElement( cs );
            vector.setExpressionExperiment( ee );
            // TODO make QuantitationType and BioAssayDimension (via DesignElementDataVector) mandatory in model.
            // vector.setQuantitationType( QuantitationType.Factory.newInstance() );
            vectorCol.add( vector );
        }
        return vectorCol;
    }

    /**
     * Add an expressionExperiment to the database for testing purposes. Includes associations.
     */
    @SuppressWarnings("unchecked")
    public void setExpressionExperimentDependencies() {
        /*
         * Create & Persist the expression experiment and dependencies
         */
        ExpressionExperiment ee = ExpressionExperiment.Factory.newInstance();
        ee.setName( "Expression Experiment" );

        ee.setDescription( "A test expression experiment" );

        ee.setSource( "http://www.ncbi.nlm.nih.gov/geo/" );

        ExternalDatabaseService eds = ( ExternalDatabaseService ) getBean( "externalDatabaseService" );
        ExternalDatabase ed = ExternalDatabase.Factory.newInstance();
        ed.setName( "PubMed" );
        ed = eds.findOrCreate( ed );

        DatabaseEntry de1 = DatabaseEntry.Factory.newInstance();
        de1.setUri( "http://www.ncbi.nlm.nih.gov/geo/" );
        de1.setAccession( "GDS1234" );

        log.debug( "database entry -> external database" );
        de1.setExternalDatabase( ed );

        log.debug( "expression experiment => database entry" );
        ee.setAccession( de1 );

        log.debug( "expression experiment => bioassays" );
        ee.setBioAssays( getBioAssays() );

        log.debug( "expression experiment => experimental designs" );
        ee.setExperimentalDesigns( getExperimentalDesigns() );

        log.debug( "expression experiment -> owner " );
        Contact c = Contact.Factory.newInstance();
        c.setName( "Foo Bar" );
        c.setAddress( "414 West 120th Street Apt. 402, New York, NY, 10027" );
        c.setPhone( "917 363 6904" );
        c.setFax( "212 851 4664" );
        c.setEmail( "keshav@cu-genome.org" );
        ContactService cs = ( ContactService ) getBean( "contactService" );
        c = cs.findOrCreate( c );
        ee.setOwner( c );

        log.debug( "expression experiment => design element data vectors" );
        ee.setDesignElementDataVectors( getDesignElementDataVectors( ee ) );

        ExpressionExperimentService ees = ( ExpressionExperimentService ) getBean( "expressionExperimentService" );
        log.debug( "Loading test expression experiment." );
        ee = ees.create( ee ); // FIXME - again, I would like to use findOrCreate
    }
}
