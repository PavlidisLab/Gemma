package edu.columbia.gemma.loader.expression.arrayExpress;

import edu.columbia.gemma.loader.expression.ExpressionLoaderImpl;
import edu.columbia.gemma.loader.expression.mage.MageBaseTest;

/**
 * <hr>
 * <p>
 * Copyright (c) 2004-2005 Columbia University
 * 
 * @author pavlidis
 * @version $Id$
 */
public class DataFileFetcherTest extends MageBaseTest {

    ExpressionLoaderImpl ml;

    /*
     * (non-Javadoc)
     * 
     * @see junit.framework.TestCase#setUp()
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    /*
     * Test method for 'edu.columbia.gemma.loader.expression.arrayExpress.DataFileFetcher.fetch(String)'
     */
    public void testFetch() throws Exception {
        DataFileFetcher f = new DataFileFetcher();
        f.fetch( "SMDB-14" );

    }

}
