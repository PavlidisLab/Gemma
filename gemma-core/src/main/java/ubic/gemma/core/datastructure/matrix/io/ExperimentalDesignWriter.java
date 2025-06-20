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
package ubic.gemma.core.datastructure.matrix.io;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.Assert;
import ubic.basecode.util.StringUtil;
import ubic.gemma.core.loader.expression.simple.ExperimentalDesignImporterImpl;
import ubic.gemma.core.util.BuildInfo;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.experiment.*;
import ubic.gemma.persistence.util.EntityUrlBuilder;

import java.io.IOException;
import java.io.Writer;
import java.util.*;

import static ubic.gemma.core.datastructure.matrix.io.ExpressionDataWriterUtils.appendBaseHeader;

/**
 * Output compatible with {@link ExperimentalDesignImporterImpl}.
 *
 * @author keshav
 * @see ExperimentalDesignImporterImpl
 */
public class ExperimentalDesignWriter {

    private final Log log = LogFactory.getLog( this.getClass() );

    private static final String EXPERIMENTAL_FACTOR_DESCRIPTION_LINE_INDICATOR = "#$";

    private final EntityUrlBuilder entityUrlBuilder;
    private final BuildInfo buildInfo;

    public ExperimentalDesignWriter( EntityUrlBuilder entityUrlBuilder, BuildInfo buildInfo ) {
        this.entityUrlBuilder = entityUrlBuilder;
        this.buildInfo = buildInfo;
    }

    /**
     * @param writer      writer
     * @param ee          ee
     * @param writeHeader writer header
     * @throws IOException when the write failed
     */
    public void write( Writer writer, ExpressionExperiment ee, boolean writeHeader ) throws IOException {

        Collection<BioAssay> bioAssays = ee.getBioAssays();
        this.write( writer, ee, bioAssays, writeHeader, writeHeader );
    }

    /**
     * @param writeBaseHeader comments
     * @param writeHeader     column names
     * @param ee              ee
     * @param bioAssays       bas
     * @param writer          writer
     * @throws IOException when the write failed
     */
    public void write( Writer writer, ExpressionExperiment ee, Collection<BioAssay> bioAssays, boolean writeBaseHeader,
            boolean writeHeader ) throws IOException {
        Assert.isTrue( ee.getExperimentalDesign() != null && !ee.getExperimentalDesign().getExperimentalFactors().isEmpty(),
                ee + " does not have an experimental design." );

        ExperimentalDesign ed = ee.getExperimentalDesign();

        /*
         * See BaseExpressionDataMatrix.setUpColumnElements() for how this is constructed for the DataMatrix, and for
         * some notes about complications.
         */
        Map<BioMaterial, Collection<BioAssay>> bioMaterials = new HashMap<>();
        for ( BioAssay bioAssay : bioAssays ) {
            BioMaterial bm = bioAssay.getSampleUsed();
            if ( !bioMaterials.containsKey( bm ) ) {
                bioMaterials.put( bm, new HashSet<>() );
            }
            bioMaterials.get( bm ).add( bioAssay );
        }

        Collection<ExperimentalFactor> efs = ed.getExperimentalFactors();

        List<ExperimentalFactor> orderedFactors = new ArrayList<>( efs );

        if ( writeHeader ) {
            this.writeHeader( ee, orderedFactors, writeBaseHeader, writer );
        }

        for ( BioMaterial bioMaterial : bioMaterials.keySet() ) {

            /* column 0 of the design matrix */
            String rowName = ExpressionDataWriterUtils
                    .constructSampleName( bioMaterial, bioMaterials.get( bioMaterial ) );
            writer.append( rowName );

            writer.append( "\t" );

            /* column 1 */
            String externalId = ExpressionDataWriterUtils.constructSampleExternalId( bioMaterial, bioMaterials.get( bioMaterial ) );
            if ( externalId != null ) {
                writer.append( externalId );
            } else {
                writer.append( "" );
            }

            /* columns 2 ... n where n+1 is the number of factors */
            Collection<FactorValue> candidateFactorValues = bioMaterial.getAllFactorValues();
            for ( ExperimentalFactor ef : orderedFactors ) {
                writer.append( "\t" );
                for ( FactorValue candidateFactorValue : candidateFactorValues ) {
                    if ( candidateFactorValue.getExperimentalFactor().equals( ef ) ) {
                        log.debug( candidateFactorValue.getExperimentalFactor() + " matched." );
                        String matchedFactorValue = ExpressionDataWriterUtils
                                .constructFactorValueName( candidateFactorValue );
                        writer.append( matchedFactorValue );
                        break;
                    }
                    log.debug( candidateFactorValue.getExperimentalFactor()
                            + " didn't match ... trying the next factor." );
                }
            }
            writer.append( "\n" );
        }

        writer.flush();
    }

    /**
     * Write an (R-friendly) header
     */
    private void writeHeader( ExpressionExperiment expressionExperiment, Collection<ExperimentalFactor> factors,
            boolean writeBaseHeader, Writer buf ) throws IOException {

        if ( writeBaseHeader ) {
            String experimentUrl = entityUrlBuilder.fromHostUrl().entity( expressionExperiment ).web().toUriString();
            appendBaseHeader( expressionExperiment, "Expression design", experimentUrl, buildInfo, buf );
        }

        for ( ExperimentalFactor ef : factors ) {
            buf.append( ExperimentalDesignWriter.EXPERIMENTAL_FACTOR_DESCRIPTION_LINE_INDICATOR );
            buf.append( ef.getName().replaceAll( "\\s", "." ) ).append( " :" );
            if ( ef.getCategory() != null ) {
                buf.append( " Category=" ).append( ef.getCategory().getValue().replaceAll( "\\s", "_" ) );
            }
            buf.append( " Type=" );

            if ( ef.getType().equals( FactorType.CATEGORICAL ) ) {
                buf.append( "Categorical" );
            } else {
                buf.append( "Continuous" );
            }

            buf.append( "\n" );
        }

        buf.append( "Bioassay\tExternalID" );

        for ( ExperimentalFactor ef : factors ) {
            String efName = StringUtil.makeValidForR( ef.getName() );
            buf.append( "\t" ).append( efName );
        }

        buf.append( "\n" );
    }

}
