/*
 * StockGraphProducer
 *
 * Copyright (c) 2000 Ken McCrary, All Rights Reserved.
 *
 * Permission to use, copy, modify, and distribute this software
 * and its documentation for NON-COMMERCIAL purposes and without
 * fee is hereby granted provided that this copyright notice
 * appears in all copies.
 *
 * KEN MCCRARY MAKES NO REPRESENTATIONS OR WARRANTIES ABOUT THE
 * SUITABILITY OF THE SOFTWARE, EITHER EXPRESS OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE, OR NON-INFRINGEMENT. KEN MCCRARY
 * SHALL NOT BE LIABLE FOR ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT
 * OF USING, MODIFYING OR DISTRIBUTING THIS SOFTWARE OR ITS DERIVATIVES.
 */
package ubic.gemma.visualization;

import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.awt.image.BufferedImage;
import java.awt.Graphics2D;
import java.awt.Color;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.FontMetrics;

import com.sun.image.codec.jpeg.JPEGCodec;
import com.sun.image.codec.jpeg.JPEGImageEncoder;

/**
 * Draw a simple stock price graph for a one week period The stock is Sun Microsystems for a week in March, 2000.
 * 
 * @author Ken McCrary
 * @author keshav
 */
public class StockGraphProducer implements ImageProducer {
    private static int ImageWidth = 300;
    private static int ImageHeight = 300;

    private static int VertInset = 25;
    private static int HorzInset = 25;
    private static int HatchLength = 10;

    /**
     * Request the producer create an image
     * 
     * @param stream stream to write image into
     * @return image type
     */
    public String createImage( OutputStream stream ) throws IOException {
        plottedPrices = new Point2D.Double[5];
        int prices[] = { 105, 100, 97, 93, 93 };

        JPEGImageEncoder encoder = JPEGCodec.createJPEGEncoder( stream );

        BufferedImage bi = new BufferedImage( ImageWidth + 10, ImageHeight, BufferedImage.TYPE_BYTE_INDEXED );

        graphics = bi.createGraphics();
        graphics.setColor( Color.white );
        graphics.fillRect( 0, 0, bi.getWidth(), bi.getHeight() );

        graphics.setColor( Color.red );

        createVerticalAxis();
        createHorizontalAxis();

        graphics.setColor( Color.green );

        plotPrices( prices );

        encoder.encode( bi );

        return "image/jpg";
    }

    /**
     * Create the vertical axis
     */
    void createVerticalAxis() {

        vertAxis = new Line2D.Double( HorzInset, VertInset, HorzInset, ImageHeight - VertInset );
        graphics.draw( vertAxis );

        // Draw the vertical hatch marks
        int verticalLength = ImageHeight - ( VertInset * 2 );
        int interval = verticalLength / 5;

        Line2D.Double vertHatch1 = new Line2D.Double( vertAxis.getP1().getX() - HatchLength / 2, vertAxis.getP1()
                .getY(), vertAxis.getP1().getX() + HatchLength / 2, vertAxis.getP1().getY() );

        graphics.draw( vertHatch1 );

        Line2D.Double vertHatch2 = new Line2D.Double( vertAxis.getP1().getX() - HatchLength / 2, vertAxis.getP1()
                .getY()
                + interval, vertAxis.getP1().getX() + HatchLength / 2, vertAxis.getP1().getY() + interval );

        graphics.draw( vertHatch2 );

        Line2D.Double vertHatch3 = new Line2D.Double( vertAxis.getP1().getX() - HatchLength / 2, vertAxis.getP1()
                .getY()
                + interval * 2, vertAxis.getP1().getX() + HatchLength / 2, vertAxis.getP1().getY() + interval * 2 );

        graphics.draw( vertHatch3 );

        Line2D.Double vertHatch4 = new Line2D.Double( vertAxis.getP1().getX() - HatchLength / 2, vertAxis.getP1()
                .getY()
                + interval * 3, vertAxis.getP1().getX() + HatchLength / 2, vertAxis.getP1().getY() + interval * 3 );

        graphics.draw( vertHatch4 );

        Line2D.Double vertHatch5 = new Line2D.Double( vertAxis.getP1().getX() - HatchLength / 2, vertAxis.getP1()
                .getY()
                + interval * 4, vertAxis.getP1().getX() + HatchLength / 2, vertAxis.getP1().getY() + interval * 4 );

        graphics.draw( vertHatch5 );

        verticalAxisTicks = new Line2D.Double[5];
        verticalAxisTicks[0] = vertHatch1;
        verticalAxisTicks[1] = vertHatch2;
        verticalAxisTicks[2] = vertHatch3;
        verticalAxisTicks[3] = vertHatch4;
        verticalAxisTicks[4] = vertHatch5;

        verticalYTop = vertHatch1.getP1().getY();
        verticalYBottom = vertHatch5.getP1().getY();
    }

    /**
     * Create the horizontal axis
     */
    void createHorizontalAxis() {
        horAxis = new Line2D.Double( HorzInset, ImageHeight - VertInset, ImageWidth - HorzInset, ImageHeight
                - VertInset );
        graphics.draw( horAxis );

        int horLength = ImageWidth - ( HorzInset * 2 );
        int horInterval = horLength / 5;

        assignVerticalRange( 90, 110, 5 );

        // Draw the horizontal hatches

        Line2D.Double horHatch1 = new Line2D.Double( horAxis.getP1().getX() + horInterval, horAxis.getP1().getY()
                - HatchLength / 2, horAxis.getP1().getX() + horInterval, horAxis.getP1().getY() + HatchLength / 2 );

        graphics.draw( horHatch1 );

        decorateVerticalLine( graphics, horHatch1, "M" );

        Line2D.Double horHatch2 = new Line2D.Double( horAxis.getP1().getX() + horInterval * 2, horAxis.getP1().getY()
                - HatchLength / 2, horAxis.getP1().getX() + horInterval * 2, horAxis.getP1().getY() + HatchLength / 2 );

        graphics.draw( horHatch2 );

        Line2D.Double horHatch3 = new Line2D.Double( horAxis.getP1().getX() + horInterval * 3, horAxis.getP1().getY()
                - HatchLength / 2, horAxis.getP1().getX() + horInterval * 3, horAxis.getP1().getY() + HatchLength / 2 );

        graphics.draw( horHatch3 );

        Line2D.Double horHatch4 = new Line2D.Double( horAxis.getP1().getX() + horInterval * 4, horAxis.getP1().getY()
                - HatchLength / 2, horAxis.getP1().getX() + horInterval * 4, horAxis.getP1().getY() + HatchLength / 2 );

        graphics.draw( horHatch4 );

        Line2D.Double horHatch5 = new Line2D.Double( horAxis.getP1().getX() + horInterval * 5, horAxis.getP1().getY()
                - HatchLength / 2, horAxis.getP1().getX() + horInterval * 5, horAxis.getP1().getY() + HatchLength / 2 );

        horizontalAxisTicks = new double[5];
        horizontalAxisTicks[0] = horHatch1.getX1();
        horizontalAxisTicks[1] = horHatch2.getX1();
        horizontalAxisTicks[2] = horHatch3.getX1();
        horizontalAxisTicks[3] = horHatch4.getX1();
        horizontalAxisTicks[4] = horHatch5.getX1();

        graphics.draw( horHatch5 );

        // Add text to hatches
        decorateVerticalLine( graphics, horHatch1, "M" );
        decorateVerticalLine( graphics, horHatch2, "T" );
        decorateVerticalLine( graphics, horHatch3, "W" );
        decorateVerticalLine( graphics, horHatch4, "T" );
        decorateVerticalLine( graphics, horHatch5, "F" );

    }

    /**
     * Plot the five closing stock prices
     */
    void plotPrices( int[] prices ) {
        // ***************************************************************
        // calculatePriceRatio will determine the percentage of the
        // Y axis the price is, then multiply by the Y axis length.
        //
        // ***************************************************************
        double yAxisLength = verticalYBottom - verticalYTop;

        double mondayPrice = calculatePriceRatio( prices[0] ) * yAxisLength + VertInset;
        double tuesdayPrice = calculatePriceRatio( prices[1] ) * yAxisLength + VertInset;
        double wednsdayPrice = calculatePriceRatio( prices[2] ) * yAxisLength + VertInset;
        double thursdayPrice = calculatePriceRatio( prices[3] ) * yAxisLength + VertInset;
        double fridayPrice = calculatePriceRatio( prices[4] ) * yAxisLength + VertInset;

        Point2D.Double day1 = new Point2D.Double( horizontalAxisTicks[0], mondayPrice );
        Point2D.Double day2 = new Point2D.Double( horizontalAxisTicks[1], tuesdayPrice );
        Point2D.Double day3 = new Point2D.Double( horizontalAxisTicks[2], wednsdayPrice );
        Point2D.Double day4 = new Point2D.Double( horizontalAxisTicks[3], thursdayPrice );
        Point2D.Double day5 = new Point2D.Double( horizontalAxisTicks[4], fridayPrice );

        Line2D.Double line1 = new Line2D.Double( day1, day2 );
        Line2D.Double line2 = new Line2D.Double( day2, day3 );
        Line2D.Double line3 = new Line2D.Double( day3, day4 );
        Line2D.Double line4 = new Line2D.Double( day4, day5 );

        graphics.draw( line1 );
        graphics.draw( line2 );
        graphics.draw( line3 );
        graphics.draw( line4 );

    }

    /**
     * Determine the location of the price in the range of price data
     */
    double calculatePriceRatio( int price ) {
        double totalDataRange = highVerticalRange - lowVerticalRange;
        double pointDelta = highVerticalRange - price;
        double ratio = pointDelta / totalDataRange;

        return ratio;
    }

    /**
     * Assignes the range for the vertical axis
     */
    void assignVerticalRange( int low, int high, int increment ) {
        lowVerticalRange = low;
        highVerticalRange = high;

        int current = low;
        int hatchCount = verticalAxisTicks.length - 1;

        // ***************************************************************
        // Label each vertical tick starting with the low value and
        // increasing by increment value
        // ***************************************************************
        while ( current <= high ) {
            decorateHorizontalLine( graphics, verticalAxisTicks[hatchCount], new Integer( current ).toString() );
            current += increment;
            hatchCount--;
        }
    }

    /**
     * Adds decorating text to the enpoint of a horizontal line
     */
    void decorateHorizontalLine( Graphics2D graphics, Line2D.Double line, String text ) {
        double endX = line.getX1();
        double endY = line.getY1();
        double baseX = endX;
        double baseY = endY;

        // ***************************************************************
        // The text should be slightly to the left of the line
        // and centered
        // ***************************************************************
        FontMetrics metrics = graphics.getFontMetrics();
        baseX -= metrics.stringWidth( text );
        baseY += metrics.getAscent() / 2;
        graphics.drawString( text, new Float( baseX ).floatValue(), new Float( baseY ).floatValue() );
    }

    /**
     * Adds decorating text to the enpoint of a vertical line
     */
    void decorateVerticalLine( Graphics2D graphics, Line2D.Double line, String text ) {
        double endX = line.getX2();
        double endY = line.getY2();
        double baseX = endX;
        double baseY = endY;

        // ***************************************************************
        // Center the text over the line
        // ***************************************************************
        FontMetrics metrics = graphics.getFontMetrics();
        baseX -= metrics.stringWidth( text ) / 2;
        baseY += metrics.getAscent();

        graphics.drawString( text, new Float( baseX ).floatValue(), new Float( baseY ).floatValue() );

    }

    /**
     * Test Entrypoint
     */
    public static void main( String[] args ) {

        try {
            FileOutputStream f = new FileOutputStream( "stockgraph.jpg" );
            StockGraphProducer producer = new StockGraphProducer();
            producer.createImage( f );
            f.close();
        } catch ( Exception e ) {
            e.printStackTrace();
        }
    }

    private Line2D.Double vertAxis;
    private Line2D.Double horAxis;
    private double[] horizontalAxisTicks;
    private int highVerticalRange;
    private int lowVerticalRange;
    private Graphics2D graphics;
    private Line2D.Double[] verticalAxisTicks;
    private Point2D.Double[] plottedPrices;
    private double verticalYTop;
    private double verticalYBottom;

}
