/*
 * The Gemma project
 *
 * Copyright (c) 2007 Columbia University
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
package ubic.gemma.core.analysis.sequence;

import lombok.Getter;
import lombok.Setter;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.genome.gene.GeneProductValueObject;
import ubic.gemma.model.genome.gene.GeneValueObject;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * @author jsantos
 */
@Getter
@Setter
@SuppressWarnings({ "WeakerAccess", "unused" }) // Frontend use
public class CompositeSequenceMapValueObject implements Comparable<CompositeSequenceMapValueObject>, Serializable {

    private Long arrayDesignId = null;
    private String arrayDesignName = null;
    private String arrayDesignShortName = null;
    private String bioSequenceId = null;
    private String bioSequenceName = null;
    private String bioSequenceNcbiId = null;
    private String compositeSequenceDescription = null;
    private String compositeSequenceId = null;
    private String compositeSequenceName = null;
    private Map<Long, GeneProductValueObject> geneProducts = new HashMap<>();
    private Map<Long, GeneValueObject> genes = new HashMap<>();
    private Integer numBlatHits = null;

    public CompositeSequenceMapValueObject() {
        super();
    }

    public static CompositeSequenceMapValueObject fromEntity( CompositeSequence cs ) {

        CompositeSequenceMapValueObject vo = new CompositeSequenceMapValueObject();
        vo.setArrayDesignId( cs.getArrayDesign().getId() );
        vo.setArrayDesignName( cs.getArrayDesign().getName() );
        vo.setBioSequenceId( cs.getBiologicalCharacteristic().getId().toString() );
        vo.setBioSequenceName( cs.getBiologicalCharacteristic().getName() );
        vo.setCompositeSequenceDescription( cs.getDescription() );
        vo.setCompositeSequenceId( cs.getId().toString() );
        vo.setArrayDesignShortName( cs.getArrayDesign().getShortName() );
        vo.setCompositeSequenceName( cs.getName() );
        return vo;
    }

    @Override
    public int compareTo( CompositeSequenceMapValueObject o ) {
        return this.compositeSequenceName.compareTo( o.getCompositeSequenceName() );
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( ( compositeSequenceName == null ) ? 0 : compositeSequenceName.hashCode() );
        return result;
    }

    @Override
    public boolean equals( Object obj ) {
        if ( this == obj )
            return true;
        if ( obj == null )
            return false;
        if ( this.getClass() != obj.getClass() )
            return false;
        CompositeSequenceMapValueObject other = ( CompositeSequenceMapValueObject ) obj;
        if ( compositeSequenceName == null ) {
            return other.compositeSequenceName == null;
        }
        return compositeSequenceName.equals( other.compositeSequenceName );
    }

}
