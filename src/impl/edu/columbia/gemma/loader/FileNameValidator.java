package edu.columbia.gemma.loader;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.xml.XmlBeanFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

/**
 * 
 *
 * <hr>
 * <p>Copyright (c) 2004 - 2005 Columbia University
 * @author keshav
 * @version $Id$
 */
public class FileNameValidator implements Validator {
    private String filename;
    Resource resource = new ClassPathResource("../action-servlet.xml");
    BeanFactory bf = new XmlBeanFactory(resource);
    
    /** Logger for this class and subclasses */
    protected final Log logger = LogFactory.getLog( getClass() );

    public boolean supports( Class clazz ) {
        return clazz.equals( FileName.class );
    }

    public void validate( Object obj, Errors errors ) {
        FileName fn = ( FileName ) obj;
        fn.setFileName(fn.getFileName().trim());
        
//        if ( fn.getFileName().equals(null) ) {
//            errors.rejectValue( "filename", "error.not-specified", null, "Value required." );
//        } 
          if (!bf.containsBean(fn.getFileName()+"LoaderService")){
            System.err.println("Bean does not exist");
            logger.info("bean does not exist");
            //errors.rejectValue( "fileName", "error.invalid", "Value required." );
            errors.reject("error.invalid","Valid entry required");
          } 
    }
    
    /**
     * @return Returns the filename.
     */
    public String getFilename() {
        return filename;
    }
    /**
     * @param filename The filename to set.
     */
    public void setFilename( String filename ) {
        this.filename = filename;
    }
}