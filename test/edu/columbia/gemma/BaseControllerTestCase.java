package edu.columbia.gemma;

import junit.framework.TestCase;

import org.springframework.beans.factory.BeanFactory;

import edu.columbia.gemma.util.SpringContextUtil;

/**
 * <hr>
 * <p>
 * Copyright (c) 2004 - 2005 Columbia University
 * 
 * @author keshav
 * @author raible
 * @version $Id$
 */
public class BaseControllerTestCase extends TestCase {
    protected final static BeanFactory ctx = SpringContextUtil.getApplicationContext();
}
