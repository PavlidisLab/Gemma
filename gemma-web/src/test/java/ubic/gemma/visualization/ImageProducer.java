/*
 * ImageProducer
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

import java.io.OutputStream;
import java.io.IOException;

/**
 * Image producers implement this interface to provide a way for the Servlet to indicate the Stream where the image
 * should be output
 * 
 * @author Ken McCrary
 * @author keshav
 */
public interface ImageProducer {
    /**
     * Request the producer create an image
     * 
     * @param stream stream to write image into
     * @return image type
     */
    public String createImage( OutputStream stream ) throws IOException;
}
