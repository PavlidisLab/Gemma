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
     * Returns true if the block design is complete, false otherwise.
     * 
     * @param expressionExperiment
     * @return boolean
     */
    public static boolean blockComplete( ExpressionExperiment expressionExperiment, QuantitationType quantitationType,
            BioAssayDimension bioAssayDimension ) {

        Exception ex = null;
        try {
            checkBlockDesign( expressionExperiment, quantitationType, bioAssayDimension );
        } catch ( Exception e ) {
            ex = e;
        } finally {
            if ( ex != null ) return false;
        }

        return true;
    }

    /**
     * @param expressionExperiment
     * @return
     * @throws Exception
     */
    private static Collection<BioMaterial> checkBlockDesign( ExpressionExperiment expressionExperiment,
            QuantitationType quantitationType, BioAssayDimension bioAssayDimension ) throws Exception {

        ExpressionDataMatrix matrix = new ExpressionDataDoubleMatrix( expressionExperiment, bioAssayDimension,
                quantitationType );

        /* first, get all the biomaterials */
        Collection<BioMaterial> biomaterials = getBioMaterialsForAssays( matrix );

        /* second, make sure each biomaterial has a factor value from one of the experimental factors */
        Collection<ExperimentalFactor> efs = expressionExperiment.getExperimentalDesign().getExperimentalFactors();

        for ( BioMaterial m : biomaterials ) {

            Collection<FactorValue> factorValuesFromBioMaterial = m.getFactorValues();

            for ( FactorValue fv : factorValuesFromBioMaterial ) {

                boolean matched = false;
                for ( ExperimentalFactor ef : efs ) {
                    if ( fv.getExperimentalFactor() == ef ) {
                        matched = true;
                        break;
                    }
                }
                if ( !matched )
                    throw new Exception(
                            "Biomaterial does not have a factor value from one of the experimental factors in the experimental design." );
            }
        }

        /* third, make sure you have replicates */
        // TODO implement this part
        return biomaterials;
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
    private static Collection<BioMaterial> getBioMaterialsForAssays( ExpressionDataMatrix matrix ) throws Exception {

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
                throw new RuntimeException( "Only supports one factor value per biomaterial." );
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
