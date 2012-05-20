/*
 * The Gemma project
 * 
 * Copyright (c) 2012 University of British Columbia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package ubic.gemma.model;

import ubic.gemma.model.expression.designElement.CompositeSequence;

/**
 * @author anton
 * @version $Id$
 */
public class CompositeSequenceValueObject {
    private Long id;
    private String name;
    private String description;
    private Long arrayDesignId;

    public Long getArrayDesignId() {
        return arrayDesignId;
    }

    public void setArrayDesignId( Long arrayDesignId ) {
        this.arrayDesignId = arrayDesignId;
    }

    public Long getId() {
        return id;
    }

    public void setId( Long id ) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName( String name ) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription( String description ) {
        this.description = description;
    }

    public static CompositeSequenceValueObject fromEntity( CompositeSequence cs ) {
        CompositeSequenceValueObject vo = new CompositeSequenceValueObject();
        vo.setDescription( cs.getDescription() );
        vo.setId( cs.getId() );
        vo.setName( cs.getName() );
        vo.setArrayDesignId( cs.getArrayDesign().getId() );
        return vo;
    }

}
