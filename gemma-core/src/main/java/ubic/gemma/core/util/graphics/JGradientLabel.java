/*
 * The baseCode project
 * 
 * Copyright (c) 2011 University of British Columbia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package ubic.gemma.core.util.graphics;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;

import javax.swing.JLabel;

/**
 * Color gradient without any text
 * 
 * @author wbraynen
 * 
 */
class JGradientLabel extends JLabel {

    private static final int MINHEIGHT = 10;
    private static final int MINWIDTH = 100;
    private static final long serialVersionUID = 348823467068723730L;
    private Color[] m_colorMap;

    /**
     * Creates a new instance of JGradientLabel
     * 
     * @param colorMap Color[]
     */
    public JGradientLabel( Color[] colorMap ) {

        if ( 0 == colorMap.length ) {
            // if there are no colors, default to grey for both colors
            colorMap = new Color[2];
            colorMap[0] = colorMap[1] = Color.LIGHT_GRAY;
        } else if ( 1 == colorMap.length ) {
            // if there is only one color, make the second color the same
            Color color = colorMap[0];
            colorMap = new Color[2];
            colorMap[0] = colorMap[1] = color;
        }

        m_colorMap = colorMap;

        Dimension d = new Dimension( MINWIDTH, MINHEIGHT );
        setSize( d );
        this.setText( "Scale bar should be here" ); // forces repaint.
        setMinimumSize( d );
    }

    /**
     * @param g
     * @param x
     * @param y
     * @return actual width
     */
    public int drawAtLocation( Graphics g, int x, int y ) {
        Graphics2D g2 = ( Graphics2D ) g;
        Color oldColor = g2.getColor();
        final int width = getWidth();
        final int height = getHeight();

        assert width > 0;
        assert height > 0;

        int numColors = m_colorMap.length;

        int intervalWidth = ( int ) Math.ceil( width / ( double ) numColors );

        int currentX = x;
        for ( int i = 0; i < numColors; i++ ) {

            Color color1 = m_colorMap[i];

            g2.setPaint( color1 );
            g2.fillRect( currentX, y, intervalWidth, height );

            currentX += ( double ) width / numColors;
        }
        g2.setColor( oldColor );
        return currentX - x;
    }

    public Color[] getColorMap() {
        return m_colorMap;
    }

    public void setColorMap( Color[] mColorMap ) {
        m_colorMap = mColorMap;
        this.repaint();
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.swing.JComponent#paintComponent(java.awt.Graphics)
     */
    @Override
    protected void paintComponent( Graphics g ) {
        super.paintComponent( g );
        drawAtLocation( g, 0, 0 );
    }
}