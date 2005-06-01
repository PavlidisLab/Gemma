package edu.columbia.gemma;

import java.util.ResourceBundle;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import edu.columbia.gemma.util.SpringContextUtil;

import junit.framework.TestCase;

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
