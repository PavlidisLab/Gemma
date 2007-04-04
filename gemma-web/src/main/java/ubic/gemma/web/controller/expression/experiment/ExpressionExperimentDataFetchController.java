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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.AbstractController;

import ubic.gemma.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.datastructure.matrix.MatrixWriter;
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

    QuantitationTypeService quantitationTypeService;
    protected DesignElementDataVectorService designElementDataVectorService;
    protected ExpressionExperimentService expressionExperimentService = null;

    public void setExpressionExperimentService( ExpressionExperimentService expressionExperimentService ) {
        this.expressionExperimentService = expressionExperimentService;
    }

    public void setQuantitationTypeService( QuantitationTypeService quantitationTypeService ) {
        this.quantitationTypeService = quantitationTypeService;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected ModelAndView handleRequestInternal( HttpServletRequest request, HttpServletResponse response )
            throws Exception {
        String qt = request.getParameter( "qt" );
        Long qtId = null;
        if ( StringUtils.isNotBlank( qt ) ) {
            try {
                qtId = Long.parseLong( qt );
            } catch ( NumberFormatException e ) {
                //
            }
        }

        QuantitationType qType = quantitationTypeService.load( qtId );

        log.info( "Fetching vectors" );
        Collection<DesignElementDataVector> vectors = designElementDataVectorService.find( qType );
        log.info( "Thawing " + vectors.size() + " vectors" );

        response.setContentType( "text/plain" );
        MatrixWriter writer = new MatrixWriter();

        int BATCH_SIZE = 200;
        Collection<DesignElementDataVector> batch = new HashSet<DesignElementDataVector>();
        boolean firstBatch = true;
        for ( DesignElementDataVector v : vectors ) {
            batch.add( v );
            if ( batch.size() == BATCH_SIZE ) {
                writeBatch( response, writer, batch, firstBatch );
                response.getWriter().flush();
                firstBatch = false;
            }
        }

        if ( batch.size() > 0 ) {
            writeBatch( response, writer, batch, firstBatch );
        }
        response.getWriter().flush();

        return null;
    }

    /**
     * @param response
     * @param writer
     * @param batch
     * @param firstBatch
     * @throws IOException
     */
    @SuppressWarnings("unchecked")
    private void writeBatch( HttpServletResponse response, MatrixWriter writer,
            Collection<DesignElementDataVector> batch, boolean firstBatch ) throws IOException {
        designElementDataVectorService.thaw( batch );
        ExpressionDataDoubleMatrix expressionDataMatrix = new ExpressionDataDoubleMatrix( batch );
        writer.write( response.getWriter(), expressionDataMatrix, firstBatch );
        batch.clear();
    }

    public void setDesignElementDataVectorService( DesignElementDataVectorService designElementDataVectorService ) {
        this.designElementDataVectorService = designElementDataVectorService;
    }

}
