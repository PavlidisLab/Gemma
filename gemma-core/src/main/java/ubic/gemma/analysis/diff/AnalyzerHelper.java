/*
 * The Gemma project
 * 
 * Copyright (c) 2007 University of British Columbia
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
package ubic.gemma.analysis.diff;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.gemma.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.datastructure.matrix.ExpressionDataMatrix;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.FactorValue;

/**
 * A helper class for the analyzers. This class contains helper methods commonly needed when performing an analysis.
 * 
 * @author keshav
 * @version $Id$
 */
public class AnalyzerHelper {

    private static Log log = LogFactory.getLog( AnalyzerHelper.class );

    /**
     * Returns true if the block design is complete and there are at least 2 biological replicates for each "group",
     * false otherwise.
     * 
     * @param expressionExperiment
     * @return boolean
     */
    public static boolean blockComplete( ExpressionExperiment expressionExperiment, QuantitationType quantitationType,
            BioAssayDimension bioAssayDimension ) {

        Exception ex = null;
        try {
            checkBlockDesign( expressionExperiment, quantitationType, bioAssayDimension );
            checkBiologicalReplicates( expressionExperiment, quantitationType, bioAssayDimension );
        } catch ( Exception e ) {
            e.printStackTrace();
            ex = e;
        } finally {
            if ( ex != null ) return false;
        }

        return true;
    }

    /**
     * Determines if each biomaterial in the expression experiment for the given quantitation type for the given
     * bioassay dimension has a factor value from each of the experimental factors.
     * 
     * @param expressionExperiment
     * @return
     * @throws Exception
     */
    protected static void checkBlockDesign( ExpressionExperiment expressionExperiment,
            QuantitationType quantitationType, BioAssayDimension bioAssayDimension ) throws Exception {

        ExpressionDataMatrix matrix = new ExpressionDataDoubleMatrix( expressionExperiment, bioAssayDimension,
                quantitationType );

        /* first, get all the biomaterials */
        Collection<BioMaterial> biomaterials = getBioMaterialsForAssays( matrix );

        /*
         * second, make sure each biomaterial has factor values from one experimental factor paired with factor values
         * from the other experimental factors
         */
        Collection<ExperimentalFactor> efs = expressionExperiment.getExperimentalDesign().getExperimentalFactors();

        for ( ExperimentalFactor ef : efs ) {
            checkBlockDesign( biomaterials, ef.getFactorValues() );
        }
    }

    /**
     * Determines if each biomaterial has factor value from an experimental factor equal to experimental factor of one
     * of the supplied factor values.
     * 
     * @param biomaterials
     * @param factorValues
     * @throws Exception
     */
    protected static void checkBlockDesign( Collection<BioMaterial> biomaterials, Collection<FactorValue> factorValues )
            throws Exception {

        ExperimentalFactor ef = factorValues.iterator().next().getExperimentalFactor();

        for ( BioMaterial m : biomaterials ) {

            Collection<FactorValue> factorValuesFromBioMaterial = m.getFactorValues();

            if ( factorValuesFromBioMaterial.size() < 2 )
                throw new Exception( "Biomaterial must have more than 1 factor value." );

            boolean match = false;
            for ( FactorValue fv : factorValuesFromBioMaterial ) {
                if ( fv.getExperimentalFactor() == ef ) {
                    match = true;
                    break;
                }
            }
            if ( !match ) {
                throw new Exception( "None of the factor values have an experimental factor that matches " + ef );
            }
        }
    }

    /**
     * Checks if there are at at least 2 biological replicates for all of the groups.
     * 
     * @param expressionExperiment
     * @param quantitationType
     * @param bioAssayDimension
     * @throws Exception
     */
    protected static void checkBiologicalReplicates( ExpressionExperiment expressionExperiment,
            QuantitationType quantitationType, BioAssayDimension bioAssayDimension ) throws Exception {

        ExpressionDataMatrix matrix = new ExpressionDataDoubleMatrix( expressionExperiment, bioAssayDimension,
                quantitationType );

        /* first, get all the biomaterials */
        Collection<BioMaterial> biomaterials = getBioMaterialsForAssays( matrix );

        Collection<BioMaterial> copyOfBiomaterials = biomaterials;

        /* second, make sure we have biological replicates */
        for ( BioMaterial biomaterial : biomaterials ) {

            Collection<FactorValue> factorValues = biomaterial.getFactorValues();

            boolean match = false;
            for ( BioMaterial m : copyOfBiomaterials ) {

                if ( biomaterial.equals( m ) ) continue;

                Collection<FactorValue> fvs = m.getFactorValues();

                if ( fvs.equals( factorValues ) ) {
                    log.debug( "Replicate found for biomaterial " + biomaterial + "." );
                    match = true;
                    break;
                }
            }
            if ( !match ) throw new Exception( "No replicate found for biomaterial " + biomaterial + "." );
        }
    }

    /**
     * Returns a collection of all the different types of biomaterials across all bioassays in the experiment. If there
     * is more than one biomaterial per bioassay, a {@link RuntimeException} is thrown.
     * 
     * @param expressionExperiment
     * @return Collection<BioMaterial>
     */
    public static Collection<BioMaterial> getBioMaterialsForBioAssays( ExpressionDataMatrix matrix ) {

        Collection<BioMaterial> biomaterials = null;

        Exception ex = null;
        try {
            biomaterials = getBioMaterialsForAssays( matrix );
        } catch ( Exception e ) {
            ex = e;
        } finally {
            if ( ex != null ) throw new RuntimeException( ex );
        }

        return biomaterials;
    }

    /**
     * @param expressionExperiment
     * @return
     * @throws Exception
     */
    protected static Collection<BioMaterial> getBioMaterialsForAssays( ExpressionDataMatrix matrix ) throws Exception {

        Collection<BioMaterial> biomaterials = new ArrayList<BioMaterial>();

        /* look for 1 bioassay/matrix column and 1 biomaterial/bioassay */
        Collection<BioAssay> assays = new ArrayList<BioAssay>();
        for ( int i = 0; i < matrix.columns(); i++ ) {
            Collection<BioAssay> bioassays = matrix.getBioAssaysForColumn( i );
            if ( bioassays.size() != 1 )
                throw new Exception( "Invalid number of bioassays for column " + i
                        + " of the matrix.  Expecting 1, got " + bioassays.size() + "." );
            assays.add( bioassays.iterator().next() );
        }

        for ( BioAssay assay : assays ) {
            Collection<BioMaterial> materials = assay.getSamplesUsed();
            if ( materials.size() != 1 )
                throw new Exception( "Invalid number of biomaterials. Expecting 1 biomaterial/bioassay, got "
                        + materials.size() + "." );

            biomaterials.addAll( materials );

        }

        return biomaterials;
    }

    /**
     * Returns the factors that can be used by R for a two way anova. Each sample must have a factor value equal to one
     * of the supplied factor values.
     * 
     * @param factorValues
     * @param samplesUsed
     * @return
     */
    public static List<String> getRFactorsFromFactorValuesForTwoWayAnova( Collection<FactorValue> factorValues,
            Collection<BioMaterial> samplesUsed ) {

        List<String> rFactors = new ArrayList<String>();

        for ( BioMaterial sampleUsed : samplesUsed ) {

            Collection<FactorValue> factorValuesFromBioMaterial = sampleUsed.getFactorValues();

            boolean match = false;

            for ( FactorValue factorValueFromBioMaterial : factorValuesFromBioMaterial ) {

                for ( FactorValue f : factorValues ) {
                    if ( factorValueFromBioMaterial.getValue() != f.getValue() ) {
                        continue;
                    }

                    else if ( factorValueFromBioMaterial.getValue() == f.getValue() ) {
                        rFactors.add( factorValueFromBioMaterial.getValue() );
                        match = true;
                        break;
                    }
                }
            }
            if ( !match )
                throw new RuntimeException(
                        "None of the Factor values of the biomaterial match the supplied factor values." );
        }

        return rFactors;
    }

    /**
     * Returns the factors that can be used by R for a one way anova. This can also be used for t-tests. There
     * requirement here is that there is only one factor value per biomaterial, and all factor values are from the same
     * experimental factor.
     * 
     * @param factorValues
     * @param samplesUsed
     * @return
     */
    public static List<String> getRFactorsFromFactorValuesForOneWayAnova( Collection<FactorValue> factorValues,
            Collection<BioMaterial> samplesUsed ) {

        // TODO Use the experimental factor as input as this will assure all factor values are from the same
        // experimental factor.

        List<String> rFactors = new ArrayList<String>();

        for ( BioMaterial sampleUsed : samplesUsed ) {
            Collection<FactorValue> factorValuesFromBioMaterial = sampleUsed.getFactorValues();

            if ( factorValuesFromBioMaterial.size() != 1 ) {
                throw new RuntimeException( "Only supports 1 factor value per biomaterial." );
            }

            FactorValue fv = factorValuesFromBioMaterial.iterator().next();

            for ( FactorValue f : factorValues ) {
                if ( fv.getValue() == f.getValue() ) {
                    log.debug( "factor value match" );
                    break;
                }

            }

            rFactors.add( fv.getValue() );
        }
        return rFactors;
    }
}
