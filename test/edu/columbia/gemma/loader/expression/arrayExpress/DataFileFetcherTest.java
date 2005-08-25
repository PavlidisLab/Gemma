package edu.columbia.gemma.loader.expression.arrayExpress;

import java.io.File;
import java.util.Collection;
import java.util.List;

import edu.columbia.gemma.common.description.LocalFile;
import edu.columbia.gemma.expression.bioAssay.BioAssay;
import edu.columbia.gemma.loader.expression.ExpressionLoaderImpl;
import edu.columbia.gemma.loader.expression.mage.MageBaseTest;
import edu.columbia.gemma.loader.expression.mage.MageMLProcessor;

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
        Collection<LocalFile> files = f.fetch( "SMDB-14");
//        MageMLProcessor mageProcessor = new MageMLProcessor();
//        mageProcessor.process( f.getMageMlFile( files ) );
//
//        List<BioAssay> bioAssays = mageProcessor.getConvertedBioAssays();
//        ArrayExpressRawDataFileProcessor processor = new ArrayExpressRawDataFileProcessor( files, bioAssays );
        
    }

}
