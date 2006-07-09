/*
 * The Gemma project
 * 
 * Copyright (c) 2006 Columbia University
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
package applet;

import java.applet.Applet;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;

/**
 * @author unattributable
 * @author keshav
 * @version $Id$
 */
public class Blur extends Applet {

    private BufferedImage bi;
    float[] elements = { .1111f, .1111f, .1111f, .1111f, .1111f, .1111f, .1111f, .1111f, .1111f };

    public Blur() {

        setBackground( Color.white );

        Image img = getToolkit().getImage( "resources/administrator/visualization.png" );
        try {
            MediaTracker tracker = new MediaTracker( this );
            tracker.addImage( img, 0 );
            tracker.waitForID( 0 );
        } catch ( Exception e ) {
        }

        int iw = img.getWidth( this );
        int ih = img.getHeight( this );
        bi = new BufferedImage( iw, ih, BufferedImage.TYPE_INT_RGB );
        Graphics2D big = bi.createGraphics();
        big.drawImage( img, 0, 0, this );

    }

    public void paint( Graphics g ) {
        Graphics2D g2 = ( Graphics2D ) g;
        int w = getSize().width;
        int h = getSize().height;
        int bw = bi.getWidth( this );
        int bh = bi.getHeight( this );

        AffineTransform at = new AffineTransform();
        at.scale( w / 2.0 / bw, h / 1.0 / bh );

        BufferedImageOp biop = null;

        BufferedImage bimg = new BufferedImage( bw, bh, BufferedImage.TYPE_INT_RGB );

        Kernel kernel = new Kernel( 3, 3, elements );
        ConvolveOp cop = new ConvolveOp( kernel, ConvolveOp.EDGE_NO_OP, null );
        cop.filter( bi, bimg );
        biop = new AffineTransformOp( at, AffineTransformOp.TYPE_NEAREST_NEIGHBOR );

        g2.drawImage( bi, biop, 0, 0 );
        g2.drawImage( bimg, biop, w / 2 + 3, 0 );

    }

    public static void main( String s[] ) {
        WindowListener l = new WindowAdapter() {
            public void windowClosing( WindowEvent e ) {
                System.exit( 0 );
            }
        };
        Frame f = new Frame( "Blur" );
        f.addWindowListener( l );
        f.add( "Center", new Blur() );
        f.pack();
        f.setSize( new Dimension( 600, 300 ) );
        f.show();
    }
}
