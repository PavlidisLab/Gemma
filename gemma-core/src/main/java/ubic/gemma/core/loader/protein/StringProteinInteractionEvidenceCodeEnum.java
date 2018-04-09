/*
 * The Gemma project
 *
 * Copyright (c) 2010 University of British Columbia
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
package ubic.gemma.core.loader.protein;

/**
 * Enum that represents the 7 different types of possible evidence that string uses to state if two proteins have an interaction.
 * The enum stores the position of the evidence in the byte array which stores this data in the db. The enuum also holds text that
 * should be displayed when that evidence is refered to in GEMMA.
 *
 * @author ldonnison
 */
public enum StringProteinInteractionEvidenceCodeEnum {
    NEIGHBORHOOD( 0, "Neighbourhood" ), GENEFUSION( 1, "GeneFusion" ), COOCCURENCE( 2, "Coocurrence" ), COEXPRESSION( 3,
            "Coexpression" ), EXPERIMENTAL( 4, "Experimental" ), DATABASE( 5, "Database" ), TEXTMINING( 6,
            "TextMining" );

    /**
     * Position of the particular evidences flag in the array which holds the information in the db
     */
    private int positionInByteArray = 0;

    /**
     * Display string representing the name of the evidence
     */
    private String displayText = "";

    StringProteinInteractionEvidenceCodeEnum( int positionInArray, String displayText ) {
        this.setPositionInArray( positionInArray );
        this.setDisplayText( displayText );
    }

    /**
     * @return the displayText
     */
    public String getDisplayText() {
        return displayText;
    }

    /**
     * @param displayText the displayText to set
     */
    @SuppressWarnings({ "unused", "WeakerAccess" }) // Possible external use
    public void setDisplayText( String displayText ) {
        this.displayText = displayText;
    }

    public int getPositionInArray() {
        return positionInByteArray;
    }

    @SuppressWarnings({ "unused", "WeakerAccess" }) // Possible external use
    public void setPositionInArray( int positionInArray ) {
        this.positionInByteArray = positionInArray;
    }

}
