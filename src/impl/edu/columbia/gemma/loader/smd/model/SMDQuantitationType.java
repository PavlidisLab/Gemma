package edu.columbia.gemma.loader.smd.model;

/**
 * <hr>
 * <p>
 * Copyright (c) 2004 Columbia University
 * 
 * @author pavlidis
 * @version $Id$
 */
public class SMDQuantitationType {

    private String name;

    public SMDQuantitationType() {

    }

    public SMDQuantitationType( String name ) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName( String name ) {
        this.name = name;
    }
}
