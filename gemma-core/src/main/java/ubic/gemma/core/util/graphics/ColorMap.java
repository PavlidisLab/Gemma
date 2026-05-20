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

/**
 * @author Will Braynen
 * 
 */
public class ColorMap {
    public static final Color DARK_RED = new Color( 128, 0, 0 );

    public static final Color[] BLACKBODY_COLORMAP = { Color.black, DARK_RED, Color.orange, Color.yellow, Color.white };
    public static final Color[] GREENRED_COLORMAP = { Color.green, Color.black, Color.red };

    public static final int m_defaultSuggestedNumberOfColors = 32;
    public static final Color[] REDGREEN_COLORMAP = { Color.red, Color.black, Color.green };
    // color map
    protected Color[] m_colorPalette;
    protected Color[] m_currentColorMap = GREENRED_COLORMAP; // reference to a
    protected Color[] m_customColorMap;

    /** last color in the current color map */
    protected Color m_maxColor;
    /** first color in the current color map */
    protected Color m_minColor;

    public ColorMap() {

        this( m_defaultSuggestedNumberOfColors );
    }

    public ColorMap( Color[] colorMap ) {

        this( m_defaultSuggestedNumberOfColors, colorMap );
    }

    public ColorMap( int suggestedNumberOfColors ) {

        this( suggestedNumberOfColors, GREENRED_COLORMAP );
    }

    /**
     * Pre-condition: suggestedNumberOfColors > colorMap.length
     * 
     * @param suggestedNumberOfColors int
     * @param colorMap Color[]
     */
    public ColorMap( int suggestedNumberOfColors, Color[] colorMap ) {

        m_currentColorMap = colorMap;
        m_colorPalette = createColorPalette( suggestedNumberOfColors, colorMap );
    }

    public Color getColor( int i ) {

        return m_colorPalette[i];
    }

    public Color[] getPalette() {

        return m_colorPalette;
    }

    /**
     * @return the number of colors in the palette
     */
    public int getPaletteSize() {

        return m_colorPalette.length;
    }

    /**
     * Allocates colors across a range.
     * 
     * @param suggestedNumberOfColors palette resolution; if colorPalette.length does not evenly divide into this
     *        number, the actual number of colors in the palette will be rounded down.
     * @param colorMap the simplest color map is { minColor, maxColor }; you might, however, want to go through
     *        intermediate colors instead of following a straight-line route through the color space.
     * @return Color[] the color palette
     */
    protected Color[] createColorPalette( int suggestedNumberOfColors, Color[] colorMap ) {

        Color[] colorPalette;
        Color minColor;
        Color maxColor;

        // number of segments is one less than the number of points
        // dividing the line into segments; the color map contains points,
        // not segments, so for example, if the color map is trivially
        // { minColor, maxColor }, then there is only one segment
        int totalSegments = m_currentColorMap.length - 1;

        // allocate colors across a range; distribute evenly
        // between intermediate points, if there are any
        int colorsPerSegment = suggestedNumberOfColors / totalSegments;

        // make sure all segments are equal by rounding down
        // the total number of colors if necessary
        int totalColors = totalSegments * colorsPerSegment;

        // create color map to return
        colorPalette = new Color[totalColors];

        for ( int segment = 0; segment < totalSegments; segment++ ) {
            // the minimum color for each segment as defined by the current color
            // map
            minColor = colorMap[segment];
            int r = minColor.getRed();
            int g = minColor.getGreen();
            int b = minColor.getBlue();

            // the maximum color for each segment and the step sizes
            maxColor = colorMap[segment + 1];
            int redStepSize = getStepSize( r, maxColor.getRed(), colorsPerSegment );
            int greenStepSize = getStepSize( g, maxColor.getGreen(), colorsPerSegment );
            int blueStepSize = getStepSize( b, maxColor.getBlue(), colorsPerSegment );

            for ( int k, i = 0; i < colorsPerSegment; i++ ) {
                // clip
                r = Math.min( r, 255 );
                g = Math.min( g, 255 );
                b = Math.min( b, 255 );

                // but also make sure it's not less than zero
                r = Math.max( r, 0 );
                g = Math.max( g, 0 );
                b = Math.max( b, 0 );

                k = segment * colorsPerSegment + i;
                colorPalette[k] = new Color( r, g, b );

                r += redStepSize;
                g += greenStepSize;
                b += blueStepSize;
            }
        }

        return colorPalette;

    } // end createColorPalette

    /**
     * Calculate how fast we have to change color components. Assume min and max colors are different!
     * 
     * @param minColor red, green, or blue component of the RGB color
     * @param maxColor red, green, or blue component of the RGB color
     * @param totalColors int
     * @return positive or negative step size
     */
    protected int getStepSize( int minColor, int maxColor, int totalColors ) {

        int colorRange = maxColor - minColor;
        double stepSize = colorRange / ( double ) ( 1 == totalColors ? 1 : totalColors - 1 );
        return ( int ) Math.round( stepSize );
    }
}