/*
 * The Gemma project
 * 
 * Copyright (c) 2006 University of British Columbia
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
package ubic.gemma.visualization;

import java.awt.Frame;
import java.awt.image.renderable.ParameterBlock;
import java.io.IOException;

import javax.media.jai.Interpolation;
import javax.media.jai.JAI;
import javax.media.jai.RenderedOp;
import javax.media.jai.widget.ScrollingImagePanel;

import com.sun.media.jai.codec.FileSeekableStream;

/**
 * @author keshav
 */
public class JAISampleProgram {

    public static void main( String[] args ) {
        if ( args.length != 1 ) {
            System.err.println( "Usage: java JAISampleProgram" + " <inputImageFilename>" );
            System.exit( -1 );
        }

        /* input stream from the specified file */
        FileSeekableStream stream = null;
        try {
            stream = new FileSeekableStream( args[0] );
        } catch ( IOException e ) {
            e.printStackTrace();
            System.exit( 0 );
        }

        /* decode image file and store in JAI operator */
        RenderedOp image1 = JAI.create( "stream", stream );

        /* create interpolation object to be used for scaling */
        Interpolation interp = Interpolation.getInstance( Interpolation.INTERP_BILINEAR );

        /* to do something to an image, store image and params in Parameter block */
        ParameterBlock params = new ParameterBlock();
        params.addSource( image1 );
        params.add( 2.0f ); // x coord
        params.add( 2.0f ); // y coord
        params.add( 0.0f ); // x trans
        params.add( 0.0f ); // y trans
        params.add( interp ); // interpolation method

        /* scale image 1 and store in operator 2 */
        RenderedOp image2 = JAI.create( "scale", params );

        int width = image2.getWidth();
        int height = image2.getHeight();

        /* attach to scrolling image panel */
        ScrollingImagePanel panel = new ScrollingImagePanel( image2, width*2, height*2 );

        /* attach panel to frame */
        Frame window = new Frame( "JAI Sample Program" );
        window.add( panel );
        window.pack();
        window.show();

    }

}
