/*
 * @(#)BufferedShapeMover.java  1.2 99/07/26
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

import java.awt.*;
import java.awt.event.*;
import java.applet.Applet;
import java.awt.image.*;

/**
 * @author unattributable
 * @author keshav
 * @version $Id$
 */
public class BufferedShapeMover extends Applet {

    static protected Label label;

    public void init() {
        // Initialize the layout.
        setLayout( new BorderLayout() );
        add( new BSMCanvas() );
        label = new Label( "Drag rectangle around within the area" );
        add( "South", label );
    }

    public static void main( String s[] ) {
        Frame f = new Frame( "BufferedShapeMover" );
        f.addWindowListener( new WindowAdapter() {
            public void windowClosing( WindowEvent e ) {
                System.exit( 0 );
            }
        } );
        Applet applet = new BufferedShapeMover();
        f.add( "Center", applet );
        applet.init();
        f.pack();
        f.setSize( new Dimension( 550, 250 ) );
        f.show();
    }

}

class BSMCanvas extends Canvas implements MouseListener, MouseMotionListener {

    Rectangle rect = new Rectangle( 0, 0, 100, 50 );
    BufferedImage bi;
    Graphics2D big;

    // Holds the coordinates of the user's last mousePressed event.
    int last_x, last_y;
    boolean firstTime = true;
    TexturePaint fillPolka, strokePolka;
    Rectangle area;

    // True if the user pressed, dragged or released the mouse outside of the rectangle; false otherwise.
    boolean pressOut = false;

    public BSMCanvas() {
        setBackground( Color.white );
        addMouseMotionListener( this );
        addMouseListener( this );

        // // Creates the fill texture paint pattern.
        // bi = new BufferedImage( 5, 5, BufferedImage.TYPE_INT_RGB );
        // big = bi.createGraphics();
        // big.setColor( Color.pink );
        // big.fillRect( 0, 0, 7, 7 );
        // big.setColor( Color.cyan );
        // big.fillOval( 0, 0, 3, 3 );
        // Rectangle r = new Rectangle( 0, 0, 5, 5 );
        // fillPolka = new TexturePaint( bi, r );
        // big.dispose();
        //
        // // Creates the stroke texture paint pattern.
        // bi = new BufferedImage( 5, 5, BufferedImage.TYPE_INT_RGB );
        // big = bi.createGraphics();
        // big.setColor( Color.cyan );
        // big.fillRect( 0, 0, 7, 7 );
        // big.setColor( Color.pink );
        // big.fillOval( 0, 0, 3, 3 );
        // r = new Rectangle( 0, 0, 5, 5 );
        // strokePolka = new TexturePaint( bi, r );
        // big.dispose();
        bi = new BufferedImage( 5, 5, BufferedImage.TYPE_INT_RGB );
        big = bi.createGraphics();
        Image img = getToolkit().getImage( "resources/administrator/visualization.png" );
        big.drawImage( img, 0, 0, this );
    }

    // Handles the event of the user pressing down the mouse button.
    public void mousePressed( MouseEvent e ) {

        last_x = rect.x - e.getX();
        last_y = rect.y - e.getY();

        // Checks whether or not the cursor is inside of the rectangle while the user is pressing themouse.
        if ( rect.contains( e.getX(), e.getY() ) ) {
            updateLocation( e );
        } else {
            BufferedShapeMover.label.setText( "First position the cursor on the rectangle and then drag." );
            pressOut = true;
        }
    }

    // Handles the event of a user dragging the mouse while holding down the mouse button.
    public void mouseDragged( MouseEvent e ) {

        if ( !pressOut ) {
            updateLocation( e );
        } else {
            BufferedShapeMover.label.setText( "First position the cursor on the rectangle and then drag." );
        }
    }

    // Handles the event of a user releasing the mouse button.
    public void mouseReleased( MouseEvent e ) {

        // Checks whether or not the cursor is inside of the rectangle when the user releases the
        // mouse button.
        if ( rect.contains( e.getX(), e.getY() ) ) {
            updateLocation( e );
        } else {
            BufferedShapeMover.label.setText( "First position the cursor on the rectangle and then drag." );
            pressOut = false;
        }
    }

    // This method required by MouseListener.
    public void mouseMoved( MouseEvent e ) {
    }

    // These methods are required by MouseMotionListener.
    public void mouseClicked( MouseEvent e ) {
    }

    public void mouseExited( MouseEvent e ) {
    }

    public void mouseEntered( MouseEvent e ) {
    }

    public void updateLocation( MouseEvent e ) {

        rect.setLocation( last_x + e.getX(), last_y + e.getY() );
        /*
         * Updates the label to reflect the location of the current rectangle if checkRect returns true; otherwise,
         * returns error message.
         */
        if ( checkRect() ) {
            BufferedShapeMover.label.setText( "Rectangle located at " + rect.getX() + ", " + rect.getY() );
        } else {
            BufferedShapeMover.label.setText( "Please don't try to " + " drag outside the area." );
        }
        repaint();
    }

    public void paint( Graphics g ) {
        update( g );
    }

    public void update( Graphics g ) {
        Graphics2D g2 = ( Graphics2D ) g;

        if ( firstTime ) {
            Dimension dim = getSize();
            int w = dim.width;
            int h = dim.height;
            area = new Rectangle( dim );
            bi = ( BufferedImage ) createImage( w, h );
            big = bi.createGraphics();
            rect.setLocation( w / 2 - 50, h / 2 - 25 );
            big.setStroke( new BasicStroke( 8.0f ) );
            firstTime = false;
        }

        // Clears the rectangle that was previously drawn.
        big.setColor( Color.white );
        big.clearRect( 0, 0, area.width, area.height );

        // Draws and fills the newly positioned rectangle to the buffer.
        big.setPaint( strokePolka );
        big.draw( rect );
        big.setPaint( fillPolka );
        big.fill( rect );

        // Draws the buffered image to the screen.
        g2.drawImage( bi, 0, 0, this );
    }

    /*
     * Checks if the rectangle is contained within the applet window. If the rectangle is not contained withing the
     * applet window, it is redrawn so that it is adjacent to the edge of the window and just inside the window.
     */
    boolean checkRect() {
        if ( area == null ) {
            return false;
        }
        if ( area.contains( rect.x, rect.y, 100, 50 ) ) {
            return true;
        }
        int new_x = rect.x;
        int new_y = rect.y;

        if ( ( rect.x + 100 ) > area.width ) {
            new_x = area.width - 99;
        }
        if ( rect.x < 0 ) {
            new_x = -1;
        }
        if ( ( rect.y + 50 ) > area.height ) {
            new_y = area.height - 49;
        }
        if ( rect.y < 0 ) {
            new_y = -1;
        }
        rect.setLocation( new_x, new_y );
        return false;
    }

}
