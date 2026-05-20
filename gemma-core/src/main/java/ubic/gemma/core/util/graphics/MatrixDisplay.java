/*
 * The baseCode project
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
package ubic.gemma.core.util.graphics;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

import javax.imageio.ImageIO;
import javax.swing.JPanel;

import org.apache.commons.lang3.StringUtils;

import ubic.gemma.core.util.matrix.DoubleMatrix;
import ubic.gemma.core.util.graphics.text.Util;

/**
 * A visual component for displaying a color matrix
 * 
 * @author Will Braynen
 * 
 */
public class MatrixDisplay<R, C> extends JPanel {

    private static final int DEFAULT_SCALE_BAR_WIDTH = 100;

    private static final long serialVersionUID = -8078532270193813539L;

    public static <R, C> MatrixDisplay<R, C> newInstance( ColorMatrix<R, C> matrix ) {
        return new MatrixDisplay<R, C>( matrix );
    }

    // data fields
    ColorMatrix<R, C> colorMatrix; // reference to standardized or unstandardized matrix
    boolean m_isShowingStandardizedMatrix = false;
    ColorMatrix<R, C> m_standardizedMatrix;

    ColorMatrix<R, C> m_unstandardizedMatrix;

    final int SCALE_BAR_ROOM = 40;

    protected Dimension m_cellSize = new Dimension( 10, 10 ); // in pixels
    protected int m_columnLabelHeight; // max
    protected final int m_defaultResolution = 120;
    protected int m_fontGutter;
    protected int m_fontSize = 10;
    protected boolean m_isShowLabels = false;
    protected boolean m_isShowScale = false;
    protected Font m_labelFont = null;
    protected int m_labelGutter = 5;
    protected int m_maxColumnLength = 0;
    protected final int m_maxFontSize = 10;
    //
    protected int m_ratioWidth = 0;

    protected int m_resolution = m_defaultResolution;

    protected int m_rowLabelWidth; // max

    protected int m_textSize = 0;

    public MatrixDisplay( ColorMatrix<R, C> matrix ) {
        init( matrix );
    }

    public MatrixDisplay( DoubleMatrix<R, C> matrix ) {
        this( new ColorMatrix<R, C>( matrix ) );
    }

    public Color getColor( int row, int column ) {
        return colorMatrix.getColor( row, column );
    } // end getColor

    /**
     * @return the current color map
     */
    public Color[] getColorMap() {
        return colorMatrix.getColorMap();
    }

    public ColorMatrix<R, C> getColorMatrix() {
        return colorMatrix;
    }

    public int getColumnCount() {
        return colorMatrix.getColumnCount();
    }

    public Object getColumnName( int column ) {
        return colorMatrix.getColumnName( column );
    }

    public String[] getColumnNames() {
        return colorMatrix.getColumnNames();
    }

    public double getDisplayMax() {
        return colorMatrix.getDisplayMax();
    }

    public double getDisplayMin() {
        return colorMatrix.getDisplayMin();
    }

    public double getDisplayRange() {
        return colorMatrix.getDisplayMax() - getDisplayMin();
    }

    public DoubleMatrix<R, C> getMatrix() {
        return colorMatrix.getMatrix();
    }

    /**
     * @return the largest value in the matrix
     */
    public double getMax() {
        return colorMatrix.getMax();
    }

    /**
     * @return the m_maxColumnLength
     */
    public int getMaxColumnLength() {
        return m_maxColumnLength;
    }

    /**
     * @return the smallest value in the matrix
     */
    public double getMin() {
        return colorMatrix.getMin();
    }

    /**
     * @return the color used for missing values
     */
    public Color getMissingColor() {
        return colorMatrix.getMissingColor();
    }

    public double getRawValue( int row, int column ) {
        return m_unstandardizedMatrix.getValue( row, column );
    }

    public double[] getRow( int row ) {
        return colorMatrix.getRow( row );
    }

    public double[] getRowByName( R rowName ) {
        return colorMatrix.getRowByName( rowName );
    }

    public int getRowCount() {
        return colorMatrix.getRowCount();
    }

    public int getRowHeight() {
        return m_cellSize.height;
    }

    public int getRowIndexByName( R rowName ) {
        return colorMatrix.getRowIndexByName( rowName );
    }

    public Object getRowName( int row ) {
        return colorMatrix.getRowName( row );
    }

    public String[] getRowNames() {
        return colorMatrix.getRowNames();
    }

    public boolean getStandardizedEnabled() {

        return m_isShowingStandardizedMatrix;
    }

    public double getValue( int row, int column ) {
        return colorMatrix.getValue( row, column );
    } // end getValue

    public void init( ColorMatrix<R, C> matrix ) {

        m_unstandardizedMatrix = colorMatrix = matrix;
        initSize();

        // create a standardized copy of the matrix
        m_standardizedMatrix = matrix.clone();
        m_standardizedMatrix.standardize();

    }

    /**
     * 
     */
    public void resetRowKeys() {
        colorMatrix.resetRowKeys();
    }

    /**
     * @param outPngFilename String
     * @param showLabels boolean
     * @param standardize normalize to deviation 1, mean 0. FIXME this is not used?
     * @throws IOException
     */
    public void saveImage( ColorMatrix<R, C> matrix, String outPngFilename, boolean showLabels, boolean showScalebar,
            boolean standardize ) throws java.io.IOException {

        Graphics2D g = null;

        // Include row and column labels?
        boolean wereLabelsShown = m_isShowLabels;
        if ( !wereLabelsShown ) {
            // Labels aren't visible, so make them visible
            setLabelsVisible( true );
            initSize();
        }

        // Draw the image to a buffer
        Dimension d = computeSize( showLabels, showScalebar ); // how big is the image with row and
        // column labels
        BufferedImage m_image = new BufferedImage( d.width, d.height, BufferedImage.TYPE_INT_RGB );
        g = m_image.createGraphics();
        g.setBackground( Color.white );
        g.clearRect( 0, 0, d.width, d.height );

        drawMatrix( matrix, g, showLabels, showScalebar );
        if ( showLabels ) {
            drawRowNames( g, showScalebar );
            drawColumnNames( g, showScalebar );
        }
        if ( showScalebar ) {
            drawScaleBar( g, d, matrix.getDisplayMin(), matrix.getDisplayMax() );
        }

        // Write the image to a png file
        ImageIO.write( m_image, "png", new File( outPngFilename ) );

        // Restore state: make the image as it was before
        if ( !wereLabelsShown ) {
            // Labels weren't visible to begin with, so hide them
            setLabelsVisible( false );
            initSize();
        }
    } // end saveImage

    /**
     * Saves the image to a png file.
     * 
     * @param outPngFilename String
     * @throws IOException
     */
    public void saveImage( String outPngFilename ) throws java.io.IOException {
        saveImage( this.colorMatrix, outPngFilename, m_isShowLabels, m_isShowScale, m_isShowingStandardizedMatrix );
    }

    /**
     * @param outPngFilename
     * @param showLabels
     * @throws java.io.IOException
     */
    public void saveImage( String outPngFilename, boolean showLabels, boolean showScale ) throws java.io.IOException {
        saveImage( this.colorMatrix, outPngFilename, showLabels, showScale, m_isShowingStandardizedMatrix );
    }

    /**
     * @param stream
     * @param showLabels
     * @param standardize
     * @throws IOException
     */
    public void saveImageToPng( ColorMatrix<R, C> matrix, OutputStream stream, boolean showLabels,
            boolean showScalebar, boolean standardize ) throws IOException {

        boolean wasScalebarShown = m_isShowScale;
        if ( !wasScalebarShown ) {
            setScaleBarVisible( true );
            initSize();
        }

        // Include row and column labels?
        boolean wereLabelsShown = m_isShowLabels;
        if ( !wereLabelsShown ) {
            // Labels aren't visible, so make them visible
            setLabelsVisible( true );
            initSize();
        }

        try {
            writeToPng( matrix, stream, showLabels, showScalebar );
        } finally {
            // Restore state: make the image as it was before
            if ( !wereLabelsShown ) {
                // Labels weren't visible to begin with, so hide them
                setLabelsVisible( false );
                initSize();
            }

            if ( !wasScalebarShown ) {
                setScaleBarVisible( false );
                initSize();
            }
        }
    } // end saveImage

    public void setCellSize( Dimension d ) {

        m_cellSize = d;
        initSize();
    }

    /**
     * @param colorMap an array of colors which define the midpoints in the color map; this can be one of the constants
     *        defined in the ColorMap class, like ColorMap.REDGREEN_COLORMAP and ColorMap.BLACKBODY_COLORMAP
     */
    public void setColorMap( Color[] colorMap ) {

        m_standardizedMatrix.setColorMap( colorMap );
        m_unstandardizedMatrix.setColorMap( colorMap );
    }

    /**
     * @param min
     * @param max
     */
    public void setDisplayRange( double min, double max ) {
        colorMatrix.setDisplayRange( min, max );
    }

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
     * @param matrix the new matrix to use; will resize this display component as necessary
     */
    public void setMatrix( ColorMatrix<R, C> matrix ) {
        colorMatrix = matrix;
        initSize();
    }

    /**
     * @param columnLength the m_maxColumnLength to set
     */
    public void setMaxColumnLength( int columnLength ) {
        m_maxColumnLength = columnLength;
    }

    public void setRowHeight( int height ) {

        m_cellSize.height = height;
        initSize();
    }

    public void setRowKeys( int[] rowKeys ) {
        colorMatrix.setRowKeys( rowKeys );
    }

    public void setScaleBarVisible( boolean isShowScale ) {
        m_isShowScale = isShowScale;
        initSize();
    }

    public void setStandardizedEnabled( boolean showStandardizedMatrix ) {
        m_isShowingStandardizedMatrix = showStandardizedMatrix;
        if ( showStandardizedMatrix ) {
            colorMatrix = m_standardizedMatrix;
        } else {
            colorMatrix = m_unstandardizedMatrix;
        }
    } // end setStandardizedEnabled

    /**
     * @param matrix
     * @param stream
     * @param showLabels
     * @param showScalebar
     */
    public void writeToPng( ColorMatrix<R, C> matrix, OutputStream stream, boolean showLabels, boolean showScalebar )
            throws IOException {
        // Draw the image to a buffer
        boolean oldLabelSate = this.m_isShowLabels;
        if ( !oldLabelSate ) {
            this.setLabelsVisible( true );
        }

        Dimension d = computeSize( showLabels, showScalebar );
        BufferedImage m_image = new BufferedImage( d.width, d.height, BufferedImage.TYPE_INT_RGB );
        Graphics2D g = m_image.createGraphics();
        g.setBackground( Color.white );
        g.clearRect( 0, 0, d.width, d.height );
        drawMatrix( matrix, g, showLabels, showScalebar );
        if ( showLabels ) {
            drawRowNames( g, showScalebar );
            drawColumnNames( g, showScalebar );
        }
        if ( showScalebar ) {
            drawScaleBar( g, d, matrix.getDisplayMin(), matrix.getDisplayMax() );
        }

        // Write the buffered image to the output steam.

        ImageIO.write( m_image, "png", stream );

        this.setLabelsVisible( oldLabelSate );
    }

    /**
     * compute the size of the matrix in pixels.
     * 
     * @param withLabels
     * @return
     */
    protected Dimension computeSize( boolean showLabels, boolean showScalebar ) {

        if ( colorMatrix == null ) {
            return null;
        }

        // row label width and height (font-dependent)
        setFont();
        m_rowLabelWidth = m_labelGutter
                + Util.maxStringPixelWidth( colorMatrix.getRowNames(), this.getFontMetrics( m_labelFont ) );
        m_rowLabelWidth += m_labelGutter; // this is optional (leaves some space on the right)
        if ( m_maxColumnLength > 0 ) {
            String[] cols = colorMatrix.getColumnNames();
            for ( int i = 0; i < cols.length; i++ ) {
                cols[i] = padColumnString( cols[i] );

            }
            // fix column height to ~5 pixels per character. This prevents a slightly different column height
            // for different letters.
            m_columnLabelHeight = 5 * m_maxColumnLength;
        } else {
            m_columnLabelHeight = Util.maxStringPixelWidth( colorMatrix.getColumnNames(),
                    this.getFontMetrics( m_labelFont ) );
        }

        // m_columnLabelHeight += m_labelGutter; // this is optional (leaves some
        // space on top)

        // height and width of this display component
        int height = m_cellSize.height * colorMatrix.getRowCount();
        int width = m_cellSize.width * colorMatrix.getColumnCount();

        // adjust for row and column labels
        if ( showLabels ) {
            width += m_rowLabelWidth;
            height += m_columnLabelHeight + m_labelGutter;
        }

        if ( showScalebar ) {
            height += SCALE_BAR_ROOM; /* scale bar height */
            // if ( width < DEFAULT_SCALE_BAR_WIDTH ) { /* scale bar width */
            // width = DEFAULT_SCALE_BAR_WIDTH;
            // }
        }

        // set the sizes
        return new Dimension( width, height );

    } // end getSize

    /**
     * Draws column names vertically (turned 90 degrees counter-clockwise)
     * 
     * @param g Graphics
     */
    protected void drawColumnNames( Graphics g, boolean leaveRoomForScalebar ) {

        if ( colorMatrix == null ) return;
        Color oldColor = g.getColor();

        int y = m_columnLabelHeight;
        if ( leaveRoomForScalebar ) {
            y += SCALE_BAR_ROOM;
        }

        g.setColor( Color.white );
        g.fillRect( 0, 0, this.getWidth(), y );

        g.setColor( Color.black );
        g.setFont( m_labelFont );

        int columnCount = colorMatrix.getColumnCount();
        for ( int j = 0; j < columnCount; j++ ) {
            // compute the coordinates
            int x = m_cellSize.width + j * m_cellSize.width - m_fontGutter;

            Object columnName = colorMatrix.getColumnName( j );
            if ( null == columnName ) {
                columnName = "Undefined";
            }

            // fix the name length as 20 characters
            // add ellipses (...) if > 20
            // spacepad to 20 if < 20
            String columnNameString = columnName.toString();
            if ( m_maxColumnLength > 0 ) {
                columnNameString = padColumnString( columnNameString );
            }

            // print the text vertically
            Util.drawVerticalString( g, columnNameString, m_labelFont, x, y );

        }
        g.setColor( oldColor );
    } // end drawColumnNames

    /**
     * Gets called from #paintComponent and #saveImage. Does not draw the labels.
     * 
     * @param g Graphics
     * @param leaveRoomForLabels boolean
     */
    protected void drawMatrix( ColorMatrix<R, C> matrix, Graphics g, boolean leaveRoomForLabels,
            boolean leaveRoomForScalebar ) {

        // fill in background with white.
        Color oldColor = g.getColor();
        g.setColor( Color.white );
        g.fillRect( 0, 0, this.getWidth(), this.getHeight() );

        int rowCount = matrix.getRowCount();
        int columnCount = matrix.getColumnCount();

        // loop through the matrix, one row at a time
        for ( int i = 0; i < rowCount; i++ ) {
            int y = i * m_cellSize.height;
            if ( leaveRoomForLabels ) {
                y += m_columnLabelHeight + m_labelGutter;
            }
            if ( leaveRoomForScalebar ) {
                y += SCALE_BAR_ROOM;
            }

            // draw an entire row, one cell at a time
            for ( int j = 0; j < columnCount; j++ ) {
                int x = j * m_cellSize.width;
                int width = ( j + 1 ) * m_cellSize.width - x;

                Color color = matrix.getColor( i, j );
                g.setColor( color );
                g.fillRect( x, y, width, m_cellSize.height );
            }

        }
        g.setColor( oldColor );
    }

    /**
     * Draws row names (horizontally)
     * 
     * @param g Graphics
     * @param showScalebar
     */
    protected void drawRowNames( Graphics g, boolean showScalebar ) {

        if ( colorMatrix == null ) return;

        Color oldColor = g.getColor();

        g.setColor( Color.white );
        g.fillRect( colorMatrix.getColumnCount() * m_cellSize.width, 0,
                colorMatrix.getColumnCount() * m_cellSize.width, this.getHeight() );

        int rowCount = colorMatrix.getRowCount();
        int xLabelStartPosition = colorMatrix.getColumnCount() * m_cellSize.width + m_labelGutter;
        g.setColor( Color.black );
        g.setFont( m_labelFont );

        for ( int i = 0; i < rowCount; i++ ) {
            int y = i * m_cellSize.height + m_columnLabelHeight + m_labelGutter;
            int yLabelPosition = y + m_cellSize.height - m_fontGutter;
            if ( showScalebar ) {
                yLabelPosition += SCALE_BAR_ROOM;
            }

            Object rowName = colorMatrix.getRowName( i );
            if ( null == rowName ) {
                rowName = "Undefined";
            }

            g.drawString( rowName.toString(), xLabelStartPosition, yLabelPosition );

        } // end drawing row names
        g.setColor( oldColor );
    } // end rawRowName

    /**
     * @param g
     * @param d
     */
    protected void drawScaleBar( Graphics g, Dimension d, double displayMin, double displayMax ) {
        /*
         * FIXME this is all a bit of a hack
         */
        g.setColor( Color.white );
        int upperLeftScalebarGutter = 10;
        int scaleBarHeight = 10; // these and text height have to total < SCALE_BAR_ROOM
        int desiredScaleBarLength = ( int ) Math.min( DEFAULT_SCALE_BAR_WIDTH, d.getWidth() );

        if ( desiredScaleBarLength < 10 ) {
            return;
        }

        g.drawRect( upperLeftScalebarGutter, upperLeftScalebarGutter, desiredScaleBarLength, upperLeftScalebarGutter );
        JGradientLabel scalebar = new JGradientLabel( new ColorMap( this.getColorMap() ).getPalette() );
        scalebar.setBackground( Color.white );
        scalebar.setSize( new Dimension( desiredScaleBarLength, scaleBarHeight ) );
        int actualWidth = scalebar.drawAtLocation( g, upperLeftScalebarGutter, upperLeftScalebarGutter );
        g.setColor( Color.black );
        g.drawString( String.format( "%.2g", displayMin ), 0, upperLeftScalebarGutter + scaleBarHeight + m_fontGutter
                + g.getFontMetrics().getHeight() );
        g.drawString( String.format( "%.2g", displayMax ), actualWidth, upperLeftScalebarGutter + scaleBarHeight
                + m_fontGutter + g.getFontMetrics().getHeight() );
        g.drawRect( upperLeftScalebarGutter, upperLeftScalebarGutter, actualWidth, scaleBarHeight );
    }

    /**
     * Sets the display size
     */
    protected void initSize() {

        Dimension d = getSize();
        setMinimumSize( d );
        setPreferredSize( d );
        setSize( d );
        this.revalidate();
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.swing.JComponent#paintComponent(java.awt.Graphics)
     */
    @Override
    protected void paintComponent( Graphics g ) {

        super.paintComponent( g );
        drawMatrix( colorMatrix, g, m_isShowLabels, m_isShowScale );

        if ( m_isShowLabels ) {
            drawRowNames( g, m_isShowScale );
            drawColumnNames( g, m_isShowScale );
        }
        if ( m_isShowScale ) {
            drawScaleBar( g, this.getSize(), colorMatrix.getDisplayMin(), colorMatrix.getDisplayMax() );
        }
    } // end paintComponent

    /**
     * @return the height of the font
     */
    private int getFontSize() {
        return Math.max( m_cellSize.height, 5 );
    }

    /**
     * Pads a string to the maxColumnLength. If it is over the maxColumnLength, it abbreviates it to the maxColumnLength
     * 
     * @param str
     * @return
     */
    private String padColumnString( String str ) {
        String paddedstr = StringUtils.abbreviate( str, m_maxColumnLength );
        paddedstr = StringUtils.rightPad( str, m_maxColumnLength, " " );
        return paddedstr;
    }

    /**
     * Sets the font used for drawing text
     */
    private void setFont() {
        int fontSize = Math.min( getFontSize(),
                ( int ) ( ( double ) m_maxFontSize / ( double ) m_defaultResolution * m_resolution ) );
        if ( fontSize != m_fontSize || m_labelFont == null ) {
            m_fontSize = fontSize;
            m_labelFont = new Font( "Ariel", Font.PLAIN, m_fontSize );
            m_fontGutter = ( int ) ( m_cellSize.height * .22 );
        }
    }

} // end class MatrixDisplay

