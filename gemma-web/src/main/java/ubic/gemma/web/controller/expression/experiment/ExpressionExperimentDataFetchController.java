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
package ubic.gemma.web.controller.expression.experiment;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.AbstractController;

import ubic.gemma.analysis.preprocess.ExpressionDataMatrixBuilder;
import ubic.gemma.datastructure.matrix.ExpressionDataMatrix;
import ubic.gemma.datastructure.matrix.MatrixWriter;
import ubic.gemma.model.common.quantitationtype.PrimitiveType;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.common.quantitationtype.QuantitationTypeService;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVectorService;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;

/**
 * @spring.bean id="expressionExperimentDataFetchController"
 * @spring.property name="quantitationTypeService" ref="quantitationTypeService"
 * @spring.property name = "designElementDataVectorService" ref="designElementDataVectorService"
 * @spring.property name = "expressionExperimentService" ref="expressionExperimentService"
 * @author pavlidis
 * @version $Id$
 */
public class ExpressionExperimentDataFetchController extends AbstractController {

    private static Log log = LogFactory.getLog( ExpressionExperimentDataFetchController.class.getName() );

    /**
     * Chunks of this many vectors of data are sent to the client, so download starts pretty quickly.
     */
    private static final int BATCH_SIZE = 2000;
    
    protected DesignElementDataVectorService designElementDataVectorService;
    protected ExpressionExperimentService expressionExperimentService = null;

    QuantitationTypeService quantitationTypeService;

    public void setDesignElementDataVectorService( DesignElementDataVectorService designElementDataVectorService ) {
        this.designElementDataVectorService = designElementDataVectorService;
    }

    public void setExpressionExperimentService( ExpressionExperimentService expressionExperimentService ) {
        this.expressionExperimentService = expressionExperimentService;
    }

    public void setQuantitationTypeService( QuantitationTypeService quantitationTypeService ) {
        this.quantitationTypeService = quantitationTypeService;
    }

    /**
     * @param response
     * @param writer
     * @param batch
     * @param firstBatch
     * @throws IOException
     */
    @SuppressWarnings("unchecked")
    private void writeBatch( HttpServletResponse response, PrimitiveType representation, MatrixWriter writer,
            Collection<DesignElementDataVector> batch, boolean firstBatch ) throws IOException {
        try {
            designElementDataVectorService.thaw( batch );
            ExpressionDataMatrix expressionDataMatrix = ExpressionDataMatrixBuilder.getMatrix( representation, batch );
            writer.write( response.getWriter(), expressionDataMatrix, firstBatch );
            batch.clear();
        } catch ( Exception e ) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    private void writeJsonBatch( HttpServletResponse response, PrimitiveType representation, MatrixWriter writer,
            Collection<DesignElementDataVector> batch, boolean firstBatch ) throws IOException {
        designElementDataVectorService.thaw( batch );
        ExpressionDataMatrix expressionDataMatrix = ExpressionDataMatrixBuilder.getMatrix( representation, batch );
        writer.writeJSON( response.getWriter(), expressionDataMatrix, firstBatch );
        batch.clear();
    }

    @SuppressWarnings("unchecked")
    @Override
    protected ModelAndView handleRequestInternal( HttpServletRequest request, HttpServletResponse response )
            throws Exception {
        String qt = request.getParameter( "qt" );
        String maxRows = request.getParameter( "maxRows" );
        String format = request.getParameter( "type" );
        Long qtId = null;
        if ( StringUtils.isNotBlank( qt ) ) {
            try {
                qtId = Long.parseLong( qt );
            } catch ( NumberFormatException e ) {
                //
            }
        }

        int maxRowsInt = -1;
        if ( StringUtils.isNotBlank( maxRows ) ) {
            try {
                maxRowsInt = Integer.parseInt( maxRows );
            } catch ( NumberFormatException e ) {
                //
            }
        }

        String usedFormat = "text";

        if ( StringUtils.isNotBlank( format ) ) {
            // FIXME validate.
            usedFormat = format;
        }

        QuantitationType qType = quantitationTypeService.load( qtId );

        log.debug( "Fetching vectors" );
        // FIXME only get maxRows if defined; validate maxRows is positive.

        StopWatch timer = new StopWatch();
        timer.start();
        Collection<DesignElementDataVector> vectors = designElementDataVectorService.find( qType );
        timer.stop();
        log.info( timer.getTime() + " ms to retrieve proxy vectors; thawing " + vectors.size() + "" );

        if ( usedFormat.equals( "text" ) ) {

            response.setContentType( "application/octet-stream" );
            String filename = qType.getName().replaceAll( "\\s+", "_" ) + ".txt";
            response.setHeader( "Content-disposition", "attachment; filename=\"" + filename + "\"" );
            response.getWriter().flush();
            MatrixWriter writer = new MatrixWriter();

            Collection<DesignElementDataVector> batch = new HashSet<DesignElementDataVector>();
            boolean firstBatch = true;
            int count = 0;
            for ( DesignElementDataVector v : vectors ) {
                batch.add( v );
                if ( batch.size() == BATCH_SIZE ) {
                    writeBatch( response, qType.getRepresentation(), writer, batch, firstBatch );
                    response.getWriter().flush();
                    firstBatch = false;
                }
                if ( maxRowsInt > 0 && ++count == maxRowsInt ) {
                    break;
                }
            }

            if ( batch.size() > 0 ) {
                writeBatch( response, qType.getRepresentation(), writer, batch, firstBatch );
            }
            response.getWriter().flush();
        } else if ( usedFormat.equals( "json" ) ) {
            response.setContentType( "application/json" );
            MatrixWriter writer = new MatrixWriter();

            Collection<DesignElementDataVector> batch = new HashSet<DesignElementDataVector>();
            boolean firstBatch = true;
            int count = 0;
            for ( DesignElementDataVector v : vectors ) {
                batch.add( v );
                if ( batch.size() == BATCH_SIZE ) {
                    writeJsonBatch( response, qType.getRepresentation(), writer, batch, firstBatch );
                    response.getWriter().flush();
                    firstBatch = false;
                }
                if ( maxRowsInt > 0 && ++count == maxRowsInt ) {
                    break;
                }
            }

            if ( batch.size() > 0 ) {
                writeJsonBatch( response, qType.getRepresentation(), writer, batch, firstBatch );
            }
            response.getWriter().flush();
        }
        return null;
    }

}
