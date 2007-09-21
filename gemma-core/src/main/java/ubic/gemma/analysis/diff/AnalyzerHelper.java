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

import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
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
     * @param expressionExperiment
     * @return
     * @throws Exception
     */
    private static Collection<BioMaterial> getBioMaterialsForAssaysWithoutReplicates(
            ExpressionExperiment expressionExperiment ) throws Exception {

        Collection<BioMaterial> biomaterials = new ArrayList<BioMaterial>();

        Collection<BioAssay> allAssays = expressionExperiment.getBioAssays();

        for ( BioAssay assay : allAssays ) {
            Collection<BioMaterial> samplesUsed = assay.getSamplesUsed();
            if ( samplesUsed.size() > 1 ) throw new Exception( "Supports one biomaterial/bioassay." );
            biomaterials.addAll( samplesUsed );
        }

        return biomaterials;
    }

    /**
     * Returns a collection of all the different types of biomaterials across all bioassays in the experiment. If there
     * is more than one biomaterial per bioassay, a {@link RuntimeException} is thrown.
     * 
     * @param expressionExperiment
     * @return Collection<BioMaterial>
     */
    public static Collection<BioMaterial> getBioMaterialsForBioAssaysWithoutReplicates(
            ExpressionExperiment expressionExperiment ) {

        Collection<BioMaterial> biomaterials = null;

        Exception ex = null;
        try {
            biomaterials = getBioMaterialsForAssaysWithoutReplicates( expressionExperiment );
        } catch ( Exception e ) {
            ex = e;
        } finally {
            if ( ex != null ) throw new RuntimeException( ex );
        }

        return biomaterials;
    }

    /**
     * Returns true if the block design is complete, false otherwise.
     * 
     * @param expressionExperiment
     * @return boolean
     */
    public static boolean blockComplete( ExpressionExperiment expressionExperiment ) {

        Exception ex = null;
        try {
            getBioMaterialsForAssaysWithoutReplicates( expressionExperiment );
        } catch ( Exception e ) {
            ex = e;
        } finally {
            if ( ex != null ) return false;
        }

        return true;
    }

    /**
     * Supports 1 factor value per biomaterial and returns a list of values. This is useful for R calls, which takes can
     * take an R-factor for methods like the {@link TTestAnalyzer}.
     * 
     * @param factorValues
     * @param samplesUsed
     * @return
     */
    public static List<String> getRFactorsFromFactorValues( Collection<FactorValue> factorValues,
            Collection<BioMaterial> samplesUsed ) {
        // TODO if this is really just for R, move to an R helper.
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
