/*
 * @(#)Blur.java	1.2 99/07/26 
 * 
 * Copyright 1997, 1998, 1999 Sun Microsystems, Inc. All Rights
 * Reserved.
 * 
 * Sun grants you ("Licensee") a non-exclusive, royalty free,
 * license to use, modify and redistribute this software in source and
 * binary code form, provided that i) this copyright notice and license 
 * appear on all copies of the software; and ii) Licensee does not utilize
 * the software in a manner which is disparaging to Sun.
 *
 * This software is provided "AS IS," without a warranty of any
 * kind. ALL EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND
 * WARRANTIES, INCLUDING ANY IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE OR NON-INFRINGEMENT, ARE HEREBY EXCLUDED. SUN
 * AND ITS LICENSORS SHALL NOT BE LIABLE FOR ANY DAMAGES SUFFERED BY
 * LICENSEE AS A RESULT OF USING, MODIFYING OR DISTRIBUTING THE SOFTWARE
 * OR ITS DERIVATIVES. IN NO EVENT WILL SUN OR ITS LICENSORS BE LIABLE FOR
 * ANY LOST REVENUE, PROFIT OR DATA, OR FOR DIRECT, INDIRECT, SPECIAL,
 * CONSEQUENTIAL, INCIDENTAL OR PUNITIVE DAMAGES, HOWEVER CAUSED AND
 * REGARDLESS OF THE THEORY OF LIABILITY, ARISING OUT OF THE USE OF
 * OR INABILITY TO USE SOFTWARE, EVEN IF SUN HAS BEEN ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGES.
 *
 * This software is not designed or intended for use in on-line
 * control of aircraft, air traffic, aircraft navigation or aircraft
 * communications; or in the design, construction, operation or
 * maintenance of any nuclear facility. Licensee represents and warrants
 * that it will not use or redistribute the Software for such purposes.
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
import java.net.MalformedURLException;
import java.net.URL;

public class Blur extends Applet {

    private BufferedImage bi;
    float[] elements = { .1111f, .1111f, .1111f, .1111f, .1111f, .1111f, .1111f, .1111f, .1111f };

    public Blur() {

        setBackground( Color.white );

        // this works, but not in a web context because of the applet security ... you cannot read and write files from
        // the filesystem without a certificate(obviously, you would have to remove build/Gemma from the path)
        // Image img = getToolkit().getImage( "build/Gemma/resources/administrator/visualization.png" );
        URL url = null;
        try {
            //System.err.println( this.getParameter( "visualization" ) );FIXME use parameters from jsp:param
            url = new URL( "http://localhost:8080/Gemma/resources/administrator/visualization.png" );
        } catch ( MalformedURLException e1 ) {
            System.err.println( "couldn't read url: " + url );
            e1.printStackTrace();
        }
        Image img = getToolkit().getImage( url );
        try {
            MediaTracker tracker = new MediaTracker( this );
            tracker.addImage( img, 0 );
            tracker.waitForID( 0 );
        } catch ( Exception e ) {
        }

        int iw = img.getWidth( this );
        int ih = img.getHeight( this );
        // bi = new BufferedImage( iw, ih, BufferedImage.TYPE_INT_RGB ); kk
        bi = new BufferedImage( 100, 100, BufferedImage.TYPE_INT_RGB );
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
