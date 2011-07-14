/*
 * The Gemma project
 * 
 * Copyright (c) 2006 University of British Columbia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package ubic.gemma.web.common;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import ubic.gemma.model.common.quantitationtype.QuantitationType;

/**
 * @version $Id$
 * @author thea
 */
public class QuantitationTypeValueObject {
    /**
     * The serial version UID of this class. Needed for serialization.
     */
    private static final long serialVersionUID = -9139694738279728432L;
     
    private String name;
    
    private String description;
    
    private String generalType;
   
    private boolean isBackground;

    private boolean isBackgroundSubtracted;

    private boolean isBatchCorrected;

    private boolean isMaskedPreferred;

    private boolean isNormalized;

    private boolean isPreferred;

    private boolean isRatio;

    private String representation;

    private String scale;

    private String type;

    /**
     * No-arg constructor added to satisfy javabean contract
     * @author thea
     */
     public QuantitationTypeValueObject() {}


     /**
      * Constructor to build value object from QuantitationType
      * 
      * @param gs
      */
     public QuantitationTypeValueObject( QuantitationType qt ) {
         this.setName( qt.getName() );
         this.setDescription( qt.getDescription() );
         this.setGeneralType( qt.getGeneralType().toString() );
         this.setIsBackground( qt.getIsBackground() );
         this.setIsBackgroundSubtracted( qt.getIsBackgroundSubtracted() );
         this.setIsBatchCorrected( qt.getIsBatchCorrected() );
         this.setIsMaskedPreferred( qt.getIsMaskedPreferred() );
         this.setIsNormalized( qt.getIsNormalized() );
         this.setIsPreferred( qt.getIsPreferred() );
         this.setIsRatio( qt.getIsRatio() );
         this.setRepresentation( qt.getRepresentation().toString() );
         this.setScale( qt.getScale().toString() );
         this.setType( qt.getType().toString() );
     }
     
     public static Collection<QuantitationTypeValueObject> convert2ValueObjects( Collection<QuantitationType> qts) {
         List<QuantitationTypeValueObject> results = new ArrayList<QuantitationTypeValueObject>();

         for ( QuantitationType qt : qts ) {
             
             results.add( new QuantitationTypeValueObject( qt ) );
         }

         Collections.sort( results, new Comparator<QuantitationTypeValueObject>() {
             @Override
             public int compare( QuantitationTypeValueObject o1,QuantitationTypeValueObject o2 ) {
                 return -o1.getName().compareTo( o2.getName() );
             }
         } );
         return results;
     }
     
    /**
     * 
     */
    public String getGeneralType()
    {
        return this.generalType;
    }

    /**
     * 
     */
    public java.lang.Boolean getIsBackground()
    {
        return this.isBackground;
    }

    /**
     * 
     */
    public java.lang.Boolean getIsBackgroundSubtracted()
    {
        return this.isBackgroundSubtracted;
    }

    /**
     * 
     */
    public java.lang.Boolean getIsBatchCorrected()
    {
        return this.isBatchCorrected;
    }

    /**
     * <p>
     * If the data represented is a missing-value masked version of the
     * preferred data.
     * </p>
     */
    public java.lang.Boolean getIsMaskedPreferred()
    {
        return this.isMaskedPreferred;
    }

    /**
     * 
     */
    public java.lang.Boolean getIsNormalized()
    {
        return this.isNormalized;
    }

    /**
     * 
     */
    public java.lang.Boolean getIsPreferred()
    {
        return this.isPreferred;
    }

    /**
     * <p>
     * Indicates whether the quantitation type is expressed as a ratio.
     * This has a natural impact on the interpretation. If false, the
     * value is "absolute".
     * </p>
     */
    public java.lang.Boolean getIsRatio()
    {
        return this.isRatio;
    }

    public String getRepresentation()
    {
        return this.representation;
    }

    public String getScale()
    {
        return this.scale;
    }

    /**
     * 
     */
    public String getType()
    {
        return this.type;
    }

    public void setGeneralType(String generalType)
    {
        this.generalType = generalType;
    }

    public void setIsBackground(java.lang.Boolean isBackground)
    {
        this.isBackground = isBackground;
    }

    public void setIsBackgroundSubtracted(java.lang.Boolean isBackgroundSubtracted)
    {
        this.isBackgroundSubtracted = isBackgroundSubtracted;
    }

    public void setIsBatchCorrected(java.lang.Boolean isBatchCorrected)
    {
        this.isBatchCorrected = isBatchCorrected;
    }

    public void setIsMaskedPreferred(java.lang.Boolean isMaskedPreferred)
    {
        this.isMaskedPreferred = isMaskedPreferred;
    }

    public void setIsNormalized(java.lang.Boolean isNormalized)
    {
        this.isNormalized = isNormalized;
    }

    public void setIsPreferred(java.lang.Boolean isPreferred)
    {
        this.isPreferred = isPreferred;
    }

    public void setIsRatio(java.lang.Boolean isRatio)
    {
        this.isRatio = isRatio;
    }

    public void setRepresentation(String representation)
    {
        this.representation = representation;
    }

    public void setScale(String scale)
    {
        this.scale = scale;
    }

    public void setType(String type)
    {
        this.type = type;
    }


    public void setDescription( String description ) {
        this.description = description;
    }


    public String getDescription() {
        return description;
    }


    public void setName( String name ) {
        this.name = name;
    }


    public String getName() {
        return name;
    }
  
}