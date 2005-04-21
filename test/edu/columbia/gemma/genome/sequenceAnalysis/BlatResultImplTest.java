package edu.columbia.gemma.genome.sequenceAnalysis;

import junit.framework.TestCase;

/**
 * <hr>
 * <p>
 * Copyright (c) 2004-2005 Columbia University
 * 
 * @author pavlidis
 * @version $Id$
 */
public class BlatResultImplTest extends TestCase {

    BlatResultImpl brtest = null;

    /*
     * @see TestCase#setUp()
     */
    protected void setUp() throws Exception {
        super.setUp();
        brtest = (BlatResultImpl)BlatResult.Factory.newInstance();
      
    }

    /*
     * @see TestCase#tearDown()
     */
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testScore() {
        brtest.setMatches(49);
        brtest.setQueryGapCount(0);
        brtest.setTargetGapCount(2);
        brtest.setMismatches(1);
        brtest.setQuerySize(50);
        double actualReturn = brtest.score();
        double expectedReturn = 47.0/50.0;
        assertEquals(expectedReturn, actualReturn, 0.001);
    }

}
