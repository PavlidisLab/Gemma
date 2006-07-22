/*
 * ImageServlet
 *
 * Copyright (c) 2000 Ken McCrary, All Rights Reserved.
 *
 * Permission to use, copy, modify, and distribute this software
 * and its documentation for NON-COMMERCIAL purposes and without
 * fee is hereby granted provided that this copyright notice
 * appears in all copies.
 *
 * KEN MCCRARY MAKES NO REPRESENTATIONS OR WARRANTIES ABOUT THE
 * SUITABILITY OF THE SOFTWARE, EITHER EXPRESS OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE, OR NON-INFRINGEMENT. KEN MCCRARY
 * SHALL NOT BE LIABLE FOR ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT
 * OF USING, MODIFYING OR DISTRIBUTING THIS SOFTWARE OR ITS DERIVATIVES.
 */
package ubic.gemma.visualization;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Simple servlet to use with Image I/O producer QueryString should be the name of a class implementing ImageProducer
 * 
 * @author Ken McCrary
 * @author keshav
 */
public class ImageServlet extends HttpServlet {
    public void doGet( HttpServletRequest request, HttpServletResponse response ) throws IOException, ServletException {
        try {
            ImageProducer imageProducer = new StockGraphProducer(); // (ImageProducer)
            // Class.forName(request.getQueryString()).newInstance();
            String type = imageProducer.createImage( response.getOutputStream() );
            response.setContentType( type );
        } catch ( Exception e ) {
            throw new ServletException( e );
        }
    }
}
