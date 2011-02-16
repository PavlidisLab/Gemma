/*
 * The Gemma project
 * 
 * Copyright (c) 2008 University of British Columbia
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
package ubic.gemma.datastructure.matrix;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.gemma.loader.expression.simple.ExperimentalDesignImporterImpl;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.experiment.ExperimentalDesign;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.FactorType;
import ubic.gemma.model.expression.experiment.FactorValue;

/**
 * Output compatible with {@link ExperimentalDesignImporterImpl}.
 * 
 * @author keshav
 * @version $Id$
 * @see ExperimentalDesignImporterImpl
 */
public class ExperimentalDesignWriter {

    private Log log = LogFactory.getLog( this.getClass() );

    /**
     * @param writer
     * @param ExpressionExperiment ee
     * @param writeHeader
     * @param sortByDesign whether the design should be arranged in the order defined by
     *        ExpressionDataMatrixColumnSort.orderByExperimentalDesign
     * @throws IOException
     */
    public void write( Writer writer, ExpressionExperiment ee, boolean writeHeader, boolean sortByDesign )
            throws IOException {

        ExperimentalDesign ed = ee.getExperimentalDesign();

        /*
         * See BaseExpressionDataMatrix.setUpColumnElements() for how this is constructed for the DataMatrix, and for
         * some notes about complications.
         */
        Collection<BioAssay> bioAssays = ee.getBioAssays();
        Map<BioMaterial, Collection<BioAssay>> bioMaterials = new HashMap<BioMaterial, Collection<BioAssay>>();
        for ( BioAssay bioAssay : bioAssays ) {
            Collection<BioMaterial> biomaterials = bioAssay.getSamplesUsed();
            BioMaterial bm = biomaterials.iterator().next();
            if ( !bioMaterials.containsKey( bm ) ) {
                bioMaterials.put( bm, new HashSet<BioAssay>() );
            }
            bioMaterials.get( bm ).add( bioAssay );
        }

        // List<BioMaterial> orderedBioMaterials = ExpressionDataMatrixColumnSort.orderByExperimentalDesign(
        // new ArrayList<BioMaterial>( bioMaterials.keySet() ), null );

        Collection<ExperimentalFactor> efs = ed.getExperimentalFactors();

        List<ExperimentalFactor> orderedFactors = new ArrayList<ExperimentalFactor>();
        orderedFactors.addAll( efs );

        StringBuffer buf = new StringBuffer();

        if ( writeHeader ) {
            writeHeader( writer, ee, orderedFactors, writeHeader, buf );
        }

        for ( BioMaterial bioMaterial : bioMaterials.keySet() ) {

            /* column 0 of the design matrix */
            String rowName = ExpressionDataWriterUtils.constructBioAssayName( bioMaterial, bioMaterials
                    .get( bioMaterial ) );
            buf.append( rowName );

            /* columns 1 ... n where n is the number of factors */
            Collection<FactorValue> candidateFactorValues = bioMaterial.getFactorValues();
            for ( ExperimentalFactor ef : orderedFactors ) {
                buf.append( "\t" );
                for ( FactorValue candidateFactorValue : candidateFactorValues ) {
                    if ( candidateFactorValue.getExperimentalFactor().equals( ef ) ) {
                        log.debug( candidateFactorValue.getExperimentalFactor() + " matched." );
                        String matchedFactorValue = ExpressionDataWriterUtils
                                .constructFactorValueName( candidateFactorValue );
                        buf.append( matchedFactorValue );
                        break;
                    }
                    log.debug( candidateFactorValue.getExperimentalFactor()
                            + " didn't match ... trying the next factor." );
                }
            }
            buf.append( "\n" );
        }

        if ( log.isDebugEnabled() ) log.debug( buf.toString() );

        writer.write( buf.toString() );

    }

    /**
     * @param writer
     * @param expressionExperiment
     * @param factors
     * @param writeHeader
     * @param buf
     */
    private void writeHeader( Writer writer, ExpressionExperiment expressionExperiment,
            Collection<ExperimentalFactor> factors, boolean writeHeader, StringBuffer buf ) {

        ExpressionDataWriterUtils.appendBaseHeader( expressionExperiment, true, buf );

        for ( ExperimentalFactor ef : factors ) {
            buf.append( ExperimentalDesignImporterImpl.EXPERIMENTAL_FACTOR_DESCRIPTION_LINE_INDICATOR );
            buf.append( ef.getName() + " :" );
            if ( ef.getCategory() != null ) {
                buf.append( " Category=" + ef.getCategory().getValue() );
            }
            buf.append( " Type=" );

            if ( ef.getType().equals( FactorType.CATEGORICAL ) ) {
                buf.append( "Categorical" );
            } else {
                buf.append( "Continuous" );
            }

            buf.append( "\n" );
        }

        buf.append( "Bioassay" );

        for ( ExperimentalFactor ef : factors ) {
            String efName = ef.getName();
            // efName = efName.replaceAll( "\\s", "_" );
            buf.append( "\t" + efName );
        }

        buf.append( "\n" );
    }

}
