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
package ubic.gemma.core.util.graphics.text;

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;

/**
 * @author Will Braynen
 * 
 */
public class Util {

    /**
     * Draws a string vertically, turned 90 degrees counter-clockwise. Read carefully what the <i>x </i> and <i>y </i>
     * coordinates means; chances are that if you draw to (x,y) = (0,0), you won't see anything.
     * 
     * @param g the graphics context on which to draw
     * @param text the string to draw
     * @param font the font to use
     * @param x the <i>x </i> coordinate where you want to place the baseline of the text.
     * @param y the <i>y </i> coordinate where you want to place the first letter of the text.
     */
    public static void drawVerticalString( Graphics g, String text, Font font, int x, int y ) {
        Graphics2D g2 = ( Graphics2D ) g;
        AffineTransform fontAT = new AffineTransform();
        // fontAT.shear(0.2, 0.0); // slant text backwards
        fontAT.setToRotation( Math.PI * 3.0f / 2.0f ); // counter-clockwise 90
        // degrees
        FontRenderContext frc = g2.getFontRenderContext();
        Font theDerivedFont = font.deriveFont( fontAT );
        TextLayout tstring = new TextLayout( text, theDerivedFont, frc );
        tstring.draw( g2, x, y );

    } // end drawVerticalString

    /**
     * @param strings an array of strings whose pixels widths to compare
     * @param fm FontMetrics object for the Component or Graphics.
     * @return the largest pixel width of a string in the <code>strings</code> array.
     */
    public static int maxStringPixelWidth( String[] strings, FontMetrics fm ) {

        // the number of chars in the longest string
        int maxWidth = 0;
        int width;
        String s;
        for ( int i = 0; i < strings.length; i++ ) {
            s = strings[i];
            width = stringPixelWidth( s, fm );
            if ( maxWidth < width ) {
                maxWidth = width;
            }
        }

        return maxWidth;
    } // end getMaxPixelWidth

    /**
     * @param text the string whose pixel width is to be measured
     * @param fm FontMetrics object for the Component or Graphics.
     * @return the pixel width of the string for the specified font.
     */
    public static int stringPixelWidth( String text, FontMetrics fm ) {
        return fm.charsWidth( text.toCharArray(), 0, text.length() );

    } // end stringPixelWidth
}