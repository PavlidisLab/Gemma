package edu.columbia.gemma.loader.expression.geo;

import java.io.InputStream;

import junit.framework.TestCase;

/**
 * <hr>
 * <p>
 * Copyright (c) 2004-2005 Columbia University
 * 
 * @author pavlidis
 * @version $Id$
 */
public class GeoFamilyParserTest extends TestCase {

    InputStream is;
    GeoFamilyParser parser;

    /*
     * @see TestCase#setUp()
     */
    protected void setUp() throws Exception {
        super.setUp();

        parser = new GeoFamilyParser();
    }

    public void testParseShortFamily() throws Exception {
        is = this.getClass().getResourceAsStream( "/data/geo/soft_ex_affy.txt" );
        parser.parse( is );
    }

    public void testParseBigA() throws Exception {
        is = this.getClass().getResourceAsStream( "/data/geo/GSE1623_family.soft.txt" );
        parser.parse( is );
    }

    public void testParseBigB() throws Exception {
        is = this.getClass().getResourceAsStream( "/data/geo/GSE993_family.soft.txt" );
        parser.parse( is );
    }

}
