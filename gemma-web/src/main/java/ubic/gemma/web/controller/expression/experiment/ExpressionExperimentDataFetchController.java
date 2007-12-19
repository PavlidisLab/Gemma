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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.AbstractController;

import ubic.gemma.analysis.service.ExpressionDataFileService;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.common.quantitationtype.QuantitationTypeService;

/**
 * For the download of data files from the browser. We can send the 'raw' data for any one quantitation type, with gene
 * annotations.
 * 
 * @spring.bean id="expressionExperimentDataFetchController"
 * @spring.property name="quantitationTypeService" ref="quantitationTypeService"
 * @spring.property name = "expressionDataFileService" ref="expressionDataFileService"
 * @author pavlidis
 * @version $Id$
 */
public class ExpressionExperimentDataFetchController extends AbstractController {

    private static Log log = LogFactory.getLog( ExpressionExperimentDataFetchController.class.getName() );

    private ExpressionDataFileService expressionDataFileService;
    QuantitationTypeService quantitationTypeService;

    public void setQuantitationTypeService( QuantitationTypeService quantitationTypeService ) {
        this.quantitationTypeService = quantitationTypeService;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected ModelAndView handleRequestInternal( HttpServletRequest request, HttpServletResponse response )
            throws Exception {
        String qt = request.getParameter( "qt" );
        String format = request.getParameter( "type" );
        Long qtId = null;
        if ( StringUtils.isNotBlank( qt ) ) {
            try {
                qtId = Long.parseLong( qt );
            } catch ( NumberFormatException e ) {
                throw new RuntimeException( "Quantitation type ID " + qt + " was invalid: not a number" );
            }
        }

        String usedFormat = "text";

        if ( StringUtils.isNotBlank( format ) ) {
            if ( !( format.equals( "text" ) || format.equals( "json" ) ) ) {
                throw new RuntimeException( "Format " + format + " is not recognized." );
            }
            usedFormat = format;
        }

        QuantitationType qType = quantitationTypeService.load( qtId );
        if ( qType == null ) {
            throw new RuntimeException( "Quantitation type ID " + qt + " was invalid: doesn't exist in system" );
        }

        InputStream reader = null;
        File f = null;
        if ( usedFormat.equals( "text" ) ) {

            f = expressionDataFileService.writeOrLocateDataFile( qType, false );

            try {
                reader = new BufferedInputStream( new FileInputStream( f ) );
            } catch ( FileNotFoundException fnfe ) {
                throw new RuntimeException( "Data file " + f + " can't be found" );
            }
            response.setHeader( "Content-disposition", "attachment; filename=" + f.getName() );
            response.setContentType( "application/octet-stream" );

        } else if ( usedFormat.equals( "json" ) ) {

            f = expressionDataFileService.writeOrLocateJSONDataFile( qType, false );

            try {
                reader = new BufferedInputStream( new FileInputStream( f ) );
            } catch ( FileNotFoundException fnfe ) {
                throw new RuntimeException( "Data file " + f + " can't be found" );
            }

            response.setHeader( "Content-disposition", "attachment; filename=" + f.getName() );
            response.setContentType( "application/json" );
        }

        writeToClient( response, reader, f );

        return null;
    }

    /**
     * @param response
     * @param reader
     * @param f
     */
    private void writeToClient( HttpServletResponse response, InputStream reader, File f ) {
        assert reader != null;
        try {
            OutputStream outputStream = response.getOutputStream();
            byte[] buf = new byte[1024];
            int len;
            while ( ( len = reader.read( buf ) ) > 0 ) {
                outputStream.write( buf, 0, len );
            }
            reader.close();

        } catch ( IOException ioe ) {
            log.warn( "Failure during streaming of data file " + f + " Error: " + ioe );
        }
    }

    public void setExpressionDataFileService( ExpressionDataFileService expressionDataFileService ) {
        this.expressionDataFileService = expressionDataFileService;
    }

}
