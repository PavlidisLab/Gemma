package edu.columbia.gemma.common.protocol;

import org.springframework.beans.factory.BeanFactory;

import edu.columbia.gemma.BaseServiceTestCase;
import edu.columbia.gemma.util.SpringContextUtil;

/**
 * <hr>
 * <p>
 * Copyright (c) 2004 - 2005 Columbia University
 * 
 * @author keshav
 * @version $Id$
 */
public class ProtocolServiceTest extends BaseServiceTestCase {
    BeanFactory ctx = SpringContextUtil.getApplicationContext();
    ProtocolDao protocolDao = null;

    protected void setUp() throws Exception {
        super.setUp();

        protocolDao = ( ProtocolDao ) ctx.getBean( "protocolDao" );

    }

    /**
     * TODO add test implementation details.
     */
    public void testfindOrCreate() {
        protocolDao.findOrCreate( Protocol.Factory.newInstance() );

    }
    
    /**
     * TODO add test implementation details.
     *
     */
    public void testFind(){
        protocolDao.find(Protocol.Factory.newInstance());
    }

}
