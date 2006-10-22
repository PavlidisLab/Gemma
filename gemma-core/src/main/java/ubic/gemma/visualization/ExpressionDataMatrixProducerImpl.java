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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.OutputStream;

import javax.imageio.ImageIO;
import javax.swing.JPanel;

import ubic.basecode.gui.ColorMatrix;
import ubic.basecode.gui.graphics.text.Util;

/**
 * An ExpressionDataMatrix image producer class
 * 
 * @author keshav
 * @verson $Id$
 */
public class ExpressionDataMatrixProducerImpl extends JPanel {
    /**
     * 
     */
    private static final long serialVersionUID = 7885617488725734975L;
    // TODO this class is quite similar to the JMatrixDisplay in baseCode ... maybe use that directly?
    protected boolean m_isShowLabels = false;
    protected BufferedImage m_image = null;
    protected int m_rowLabelWidth; // max
    protected int m_columnLabelHeight; // max
    ColorMatrix colorMatrix;
    protected int m_labelGutter = 5;
    protected Dimension m_cellSize = new Dimension( 10, 10 );
    protected Font m_labelFont = null;
    protected int m_fontSize = 10;
    protected int m_fontGutter;
    protected final int m_maxFontSize = 10;
    protected final int m_defaultResolution = 120;
    protected int m_resolution = m_defaultResolution;

    /**
     * @param stream
     * @param showLabels
     * @param standardize
     * @return
     * @throws java.io.IOException
     */
    public String createDynamicImage( OutputStream stream, boolean showLabels, boolean standardize )
            throws java.io.IOException {

        // JPEGImageEncoder encoder = JPEGCodec.createJPEGEncoder( stream ); //use with encoder.encode(m_image) below

        Graphics2D graphics = null;

        // Include row and column labels?
        boolean wereLabelsShown = m_isShowLabels;
        if ( !wereLabelsShown ) {
            // Labels aren't visible, so make them visible
            setLabelsVisible( true );
            initSize();
        }

        // Draw the image to a buffer
        Dimension d = getSize( showLabels ); // how big is the image with row and
        // column labels
        m_image = new BufferedImage( d.width, d.height, BufferedImage.TYPE_INT_RGB );

        graphics = m_image.createGraphics();
        drawMatrix( graphics, showLabels );
        if ( showLabels ) {
            drawRowNames( graphics );
            drawColumnNames( graphics );
        }

        ImageIO.write( m_image, "png", stream );
        // encoder.encode( m_image );

        return "image/png";
    } // end createDynamicImage

    /**
     * If this display component has already been added to the GUI, it will be resized to fit or exclude the row names
     * 
     * @param isShowLabels boolean
     */
    public void setLabelsVisible( boolean isShowLabels ) {
        m_isShowLabels = isShowLabels;
        initSize();
    }

    /**
     * Sets the display size
     */
    protected void initSize() {

        Dimension d = getSize( m_isShowLabels );
        setMinimumSize( d );
        setPreferredSize( d );
        setSize( d );
        this.revalidate();
    }

    protected Dimension getSize( boolean withLabels ) {

        if ( colorMatrix == null ) {
            return null;
        }

        // row label width and height (font-dependent)
        setFont();
        m_rowLabelWidth = m_labelGutter + Util.maxStringPixelWidth( colorMatrix.getRowNames(), m_labelFont, this );
        // m_rowLabelWidth += m_labelGutter; // this is optional (leaves some
        // space on the right)
        m_columnLabelHeight = Util.maxStringPixelWidth( colorMatrix.getColumnNames(), m_labelFont, this );
        // m_columnLabelHeight += m_labelGutter; // this is optional (leaves some
        // space on top)

        // height and width of this display component
        int height = m_cellSize.height * colorMatrix.getRowCount();
        int width = m_cellSize.width * colorMatrix.getColumnCount();

        // adjust for row and column labels
        if ( withLabels ) {
            width += m_rowLabelWidth;
            height += ( m_columnLabelHeight + m_labelGutter );
        }

        // set the sizes
        return new Dimension( width, height );

    } // end getSize

    /**
     * Sets the font used for drawing text
     */
    private void setFont() {
        int fontSize = Math.min( getFontSize(),
                ( int ) ( ( double ) m_maxFontSize / ( double ) m_defaultResolution * m_resolution ) );
        if ( ( fontSize != m_fontSize ) || ( m_labelFont == null ) ) {
            m_fontSize = fontSize;
            m_labelFont = new Font( "Ariel", Font.PLAIN, m_fontSize );
            m_fontGutter = ( int ) ( m_cellSize.height * .22 );
        }
    }

    /**
     * @return the height of the font
     */
    private int getFontSize() {
        return Math.max( m_cellSize.height, 5 );
    }

    /**
     * Draws row names (horizontally)
     * 
     * @param g Graphics
     */
    protected void drawRowNames( Graphics g ) {

        if ( colorMatrix == null ) return;

        int rowCount = colorMatrix.getRowCount();

        // draw row names
        for ( int i = 0; i < rowCount; i++ ) {
            g.setColor( Color.black );
            g.setFont( m_labelFont );
            int y = ( i * m_cellSize.height ) + m_columnLabelHeight + m_labelGutter;
            int xRatio = ( colorMatrix.getColumnCount() * m_cellSize.width ) + m_labelGutter;
            int yRatio = y + m_cellSize.height - m_fontGutter;
            String rowName = colorMatrix.getRowName( i );
            if ( null == rowName ) {
                rowName = "Undefined";
            }
            g.drawString( rowName, xRatio, yRatio );
        } // end drawing row names
    } // end rawRowName

    /**
     * Draws column names vertically (turned 90 degrees counter-clockwise)
     * 
     * @param g Graphics
     */
    protected void drawColumnNames( Graphics g ) {

        if ( colorMatrix == null ) return;

        int columnCount = colorMatrix.getColumnCount();
        for ( int j = 0; j < columnCount; j++ ) {
            // compute the coordinates
            int x = m_cellSize.width + ( j * m_cellSize.width ) - m_fontGutter;
            int y = m_columnLabelHeight;

            // get column name
            String columnName = colorMatrix.getColumnName( j );
            if ( null == columnName ) {
                columnName = "Undefined";
            }

            // set font and color
            g.setColor( Color.black );
            g.setFont( m_labelFont );

            // print the text vertically
            Util.drawVerticalString( g, columnName, m_labelFont, x, y );

        } // end for column
    } // end drawColumnNames

    /**
     * Gets called from #paintComponent and #saveImage
     * 
     * @param g Graphics
     * @param leaveRoomForLabels boolean
     */
    protected void drawMatrix( Graphics g, boolean leaveRoomForLabels ) {

        g.setColor( Color.white );
        g.fillRect( 0, 0, this.getWidth(), this.getHeight() );

        int rowCount = colorMatrix.getRowCount();
        int columnCount = colorMatrix.getColumnCount();

        // loop through the matrix, one row at a time
        for ( int i = 0; i < rowCount; i++ ) {
            int y = ( i * m_cellSize.height );
            if ( leaveRoomForLabels ) {
                y += ( m_columnLabelHeight + m_labelGutter );
            }

            // draw an entire row, one cell at a time
            for ( int j = 0; j < columnCount; j++ ) {
                int x = ( j * m_cellSize.width );
                int width = ( ( j + 1 ) * m_cellSize.width ) - x;

                Color color = colorMatrix.getColor( i, j );
                g.setColor( color );
                g.fillRect( x, y, width, m_cellSize.height );
            }

        } // end looping through the matrix, one row at a time
    } // end drawMatrix

    /**
     * @return Returns the m_matrix.
     */
    public ColorMatrix getColorMatrix() {
        return colorMatrix;
    }

    /**
     * @param m_matrix The m_matrix to set.
     */
    public void setColorMatrix( ColorMatrix colorMatrix ) {
        this.colorMatrix = colorMatrix;
    }

}
