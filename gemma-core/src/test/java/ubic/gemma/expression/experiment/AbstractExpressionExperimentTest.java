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

import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.math.RandomUtils;

import ubic.basecode.io.ByteArrayConverter;
import ubic.gemma.loader.util.persister.PersisterHelper;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExperimentalDesign;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.testing.BaseTransactionalSpringContextTest;

/**
 * A utility to load expression experiment test data.
 * 
 * @author keshav
 * @version $Id$
 */
public abstract class AbstractExpressionExperimentTest extends BaseTransactionalSpringContextTest {

    /**
     * 
     */
    private static final int NUM_EXPERIMENTAL_DESIGNS = 2;
    /**
     * 
     */
    private static final int NUM_FACTOR_VALUES = 2;
    /**
     * 
     */
    private static final int NUM_EXPERIMENTAL_FACTORS = 3;

    /**
     * @throws Exception
     */
    @Override
    public void onSetUpInTransaction() throws Exception {
        super.onSetUpInTransaction();

    }

    /**
     * @return Collection
     */
    private Collection<BioAssay> getBioAssays( ArrayDesign ad ) {
        Collection<BioAssay> baCol = new HashSet<BioAssay>();
        for ( int i = 0; i < TEST_ELEMENT_COLLECTION_SIZE; i++ ) {
            BioAssay ba = this.getTestPersistentBioAssay( ad );
            baCol.add( ba );
        }

        return baCol;
    }

    /**
     * @return Collection
     */
    private Collection<FactorValue> getFactorValues() {
        Collection<FactorValue> fvCol = new HashSet<FactorValue>();
        for ( int i = 0; i < NUM_FACTOR_VALUES; i++ ) {
            FactorValue fv = FactorValue.Factory.newInstance();
            fv.setValue( "Factor value " + RandomStringUtils.randomNumeric( RANDOM_STRING_LENGTH ) );
            fvCol.add( fv );
        }
        return fvCol;
    }

    /**
     * @return
     */
    private Collection<ExperimentalFactor> getExperimentalFactors() {
        Collection<ExperimentalFactor> efCol = new HashSet<ExperimentalFactor>();
        for ( int i = 0; i < NUM_EXPERIMENTAL_FACTORS; i++ ) {
            ExperimentalFactor ef = ExperimentalFactor.Factory.newInstance();
            ef.setName( "Experimental Factor " + RandomStringUtils.randomNumeric( RANDOM_STRING_LENGTH ) );
            ef.setDescription( i + ": A test experimental factor" );
            log.debug( "experimental factor => factor values" );
            ef.setFactorValues( getFactorValues() );
            efCol.add( ef );
        }
        return efCol;
    }

    /**
     * @return
     */
    private Collection<ExperimentalDesign> getExperimentalDesigns() {
        Collection<ExperimentalDesign> edCol = new HashSet<ExperimentalDesign>();
        for ( int i = 0; i < NUM_EXPERIMENTAL_DESIGNS; i++ ) {
            ExperimentalDesign ed = ExperimentalDesign.Factory.newInstance();
            ed.setName( "Experimental Design " + RandomStringUtils.randomNumeric( 10 ) );
            ed.setDescription( i + ": A test experimental design." );

            log.debug( "experimental design => experimental factors" );
            ed.setExperimentalFactors( getExperimentalFactors() ); // set test experimental factors

            edCol.add( ed ); // add experimental designs
        }
        return edCol;
    }

    private Collection<DesignElementDataVector> getDesignElementDataVectors( ExpressionExperiment ee, ArrayDesign ad ) {

        Collection<DesignElementDataVector> vectors = new HashSet<DesignElementDataVector>();
        for ( CompositeSequence cs : ad.getCompositeSequences() ) {
            DesignElementDataVector vector = DesignElementDataVector.Factory.newInstance();
            double[] data = new double[TEST_ELEMENT_COLLECTION_SIZE / 2];
            for ( int j = 0; j < data.length; j++ ) {
                data[j] = RandomUtils.nextDouble();
            }
            ByteArrayConverter bconverter = new ByteArrayConverter();
            byte[] bdata = bconverter.doubleArrayToBytes( data );
            vector.setData( bdata );

            vector.setDesignElement( cs );

            assert cs.getArrayDesign() != null;

            vector.setExpressionExperiment( ee );

            Collection<BioAssay> bioAssays = getBioAssays( ad );

            BioAssayDimension bad = BioAssayDimension.Factory.newInstance( bioAssays );

            vector.setQuantitationType( this.getTestPersistentQuantitationType() );
            vector.setBioAssayDimension( bad );

            // we're only creating one vector here, but each design element can have more than one.
            vectors.add( vector );
            cs.setDesignElementDataVectors( vectors );
        }
        return vectors;
    }

    /**
     * Add an expressionExperiment to the database for testing purposes. Includes associations.
     */
    protected ExpressionExperiment setExpressionExperimentDependencies() {
        ExpressionExperiment ee = ExpressionExperiment.Factory.newInstance();
        ArrayDesign ad = this.getTestPersistentArrayDesign( TEST_ELEMENT_COLLECTION_SIZE, false );

        ee.setName( "Expression Experiment " + RandomStringUtils.randomNumeric( RANDOM_STRING_LENGTH ) );

        ee.setDescription( "A test expression experiment" );

        ee.setSource( "http://www.ncbi.nlm.nih.gov/geo/" );

        DatabaseEntry de1 = this.getTestPersistentDatabaseEntry();

        log.debug( "expression experiment => database entry" );
        ee.setAccession( de1 );

        log.debug( "expression experiment => bioassays" );
        ee.setBioAssays( getBioAssays( ad ) );

        log.debug( ee + " => experimentalDesigns designs" );
        ee.setExperimentalDesigns( getExperimentalDesigns() );

        log.debug( "expression experiment -> owner " );

        ee.setOwner( this.getTestPersistentContact() );

        log.debug( "expression experiment => design element data vectors" );
        ee.setDesignElementDataVectors( getDesignElementDataVectors( ee, ad ) );

        log.debug( "Loading test expression experiment." );

        PersisterHelper ph = ( PersisterHelper ) getBean( "persisterHelper" );

        assert ph != null;

        return ( ExpressionExperiment ) ph.persist( ee );
    }

}
