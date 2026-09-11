/*
 * The Gemma project
 *
 * Copyright (c) 2026 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package ubic.gemma.core.analysis.preprocess.batcheffects;

import cern.colt.list.DoubleArrayList;
import cern.colt.list.IntArrayList;
import org.junit.jupiter.api.Test;
import ubic.gemma.core.util.math.KruskalWallis;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.measurement.Measurement;
import ubic.gemma.model.common.measurement.MeasurementType;
import ubic.gemma.model.common.quantitationtype.PrimitiveType;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.experiment.*;

import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Unit tests for {@link BatchConfoundUtils}, covering biomaterials that carry no value for a factor.
 * <p>
 * These are built entirely from transient entities: {@link BatchConfoundUtils#test(BioAssaySet)} reads only the
 * bioassays, their samples and those samples' factor values, so no persistence is involved.
 *
 * @author gembro
 */
public class BatchConfoundUtilsTest {

    private long nextId = 1L;

    /**
     * A batch factor confounded with a continuous factor, where one sample has no value for that factor.
     * <p>
     * {@code getBioMaterialFactorMap()} pads every factor's map with a null for each sample that has no value for it,
     * and the Kruskal-Wallis leg used to unbox that null.
     */
    @Test
    public void continuousFactorWithAMissingValueExcludesThatSample() {
        ExpressionExperiment ee = newExperiment();

        ExperimentalFactor batch = newFactor( "batch", FactorType.CATEGORICAL );
        FactorValue batchA = newBatchValue( batch );
        FactorValue batchB = newBatchValue( batch );

        ExperimentalFactor age = newFactor( "age", FactorType.CONTINUOUS );

        newSample( ee, "s1", batchA, newMeasuredValue( age, 10 ) );
        newSample( ee, "s2", batchA, newMeasuredValue( age, 20 ) );
        newSample( ee, "s3", batchA, newMeasuredValue( age, 30 ) );
        newSample( ee, "s4", batchB, newMeasuredValue( age, 100 ) );
        newSample( ee, "s5", batchB, newMeasuredValue( age, 200 ) );
        // in a batch, but no value for age
        newSample( ee, "s6", batchB );

        Collection<BatchConfound> confounds = BatchConfoundUtils.test( ee );

        assertThat( confounds ).hasSize( 1 );
        BatchConfound confound = confounds.iterator().next();
        assertThat( confound.getFactor() ).isSameAs( age );

        // The statistic must be the one the five samples that do have a value produce on their own. Skipping s6
        // without shrinking the pre-sized lists would leave a sixth, fabricated 0.0 in batch 0, which moves every
        // rank: 3.0 becomes 0.43 here.
        DoubleArrayList values = new DoubleArrayList( new double[] { 10, 20, 30, 100, 200 } );
        IntArrayList batches = new IntArrayList( new int[] { 0, 0, 0, 1, 1 } );
        assertThat( confound.getChiSquare() ).isCloseTo( KruskalWallis.kwStatistic( values, batches ), within( 1e-12 ) );
        assertThat( confound.getPValue() ).isCloseTo( KruskalWallis.test( values, batches ), within( 1e-12 ) );
        assertThat( confound.getDf() ).isEqualTo( 1 );
    }

    /**
     * Once samples with no value are excluded, too few may be left to rank.
     */
    @Test
    public void continuousFactorWithTooFewUsableSamplesIsSkipped() {
        ExpressionExperiment ee = newExperiment();

        ExperimentalFactor batch = newFactor( "batch", FactorType.CATEGORICAL );
        FactorValue batchA = newBatchValue( batch );
        FactorValue batchB = newBatchValue( batch );

        ExperimentalFactor age = newFactor( "age", FactorType.CONTINUOUS );

        newSample( ee, "s1", batchA, newMeasuredValue( age, 10 ) );
        newSample( ee, "s2", batchB, newMeasuredValue( age, 100 ) );
        // no value for age, leaving two usable samples, below the three KruskalWallis.test() requires
        newSample( ee, "s3", batchA );
        newSample( ee, "s4", batchB );

        assertThat( BatchConfoundUtils.test( ee ) ).isEmpty();
    }

    /**
     * A factor whose category carries no value. {@link Characteristic#getValue()} is nullable and is null for 8,165
     * production rows.
     */
    @Test
    public void factorCategoryWithNoValueIsTolerated() {
        ExpressionExperiment ee = newExperiment();

        ExperimentalFactor batch = newFactor( "batch", FactorType.CATEGORICAL );
        FactorValue batchA = newBatchValue( batch );
        FactorValue batchB = newBatchValue( batch );

        ExperimentalFactor age = newFactor( "age", FactorType.CONTINUOUS );
        Characteristic category = Characteristic.Factory.newInstance();
        category.setCategory( "age" );
        assertThat( category.getValue() ).isNull();
        age.setCategory( category );

        newSample( ee, "s1", batchA, newMeasuredValue( age, 10 ) );
        newSample( ee, "s2", batchA, newMeasuredValue( age, 20 ) );
        newSample( ee, "s3", batchB, newMeasuredValue( age, 100 ) );
        newSample( ee, "s4", batchB, newMeasuredValue( age, 200 ) );

        Collection<BatchConfound> confounds = BatchConfoundUtils.test( ee );

        assertThat( confounds ).hasSize( 1 );
        assertThat( confounds.iterator().next().getFactor() ).isSameAs( age );
    }

    private ExpressionExperiment newExperiment() {
        ExpressionExperiment ee = ExpressionExperiment.Factory.newInstance();
        ee.setId( nextId++ );
        ee.setName( "batch confound test" );
        ee.setShortName( "GSE_BATCHCONFOUND" );
        return ee;
    }

    private ExperimentalFactor newFactor( String name, FactorType type ) {
        ExperimentalFactor ef = ExperimentalFactor.Factory.newInstance( name, type );
        ef.setId( nextId++ );
        return ef;
    }

    /**
     * A categorical factor value; only its ID reaches the confound test.
     */
    private FactorValue newBatchValue( ExperimentalFactor ef ) {
        FactorValue fv = FactorValue.Factory.newInstance( ef );
        fv.setId( nextId++ );
        ef.getFactorValues().add( fv );
        return fv;
    }

    private FactorValue newMeasuredValue( ExperimentalFactor ef, double value ) {
        FactorValue fv = FactorValue.Factory.newInstance( ef );
        fv.setId( nextId++ );
        fv.setMeasurement( Measurement.Factory.newInstance( MeasurementType.ABSOLUTE, String.valueOf( value ),
                PrimitiveType.DOUBLE ) );
        ef.getFactorValues().add( fv );
        return fv;
    }

    private void newSample( ExpressionExperiment ee, String name, FactorValue... factorValues ) {
        BioMaterial bm = BioMaterial.Factory.newInstance( name );
        bm.setId( nextId++ );
        for ( FactorValue fv : factorValues ) {
            bm.getFactorValues().add( fv );
        }
        BioAssay ba = BioAssay.Factory.newInstance( name );
        ba.setId( nextId++ );
        ba.setSampleUsed( bm );
        ee.getBioAssays().add( ba );
    }
}
