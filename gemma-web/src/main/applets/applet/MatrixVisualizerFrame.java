package applet;

import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import javax.swing.JFrame;
import javax.swing.JTable;

public class MatrixVisualizerFrame extends JFrame {

    public MatrixVisualizerFrame( String name ) {
        WindowListener l = new WindowAdapter() {
            public void windowClosing( WindowEvent e ) {
                System.exit( 0 );
            }
        };

        setLayout( new GridLayout() );
        addWindowListener( l );
        add( "Center", new HtmlMatrixVisualizerApplet() );

        JTable table = new JTable( 10, 1 );
        add( table );

        pack();
        setSize( new Dimension( 600, 300 ) );
        show();
    }

}
