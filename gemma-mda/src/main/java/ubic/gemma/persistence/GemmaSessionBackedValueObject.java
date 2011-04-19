package ubic.gemma.persistence;

import java.util.Collection;

import ubic.gemma.model.Reference;

public interface GemmaSessionBackedValueObject {
	
	public Reference getReference();
	
    public void setReference(Reference reference);
    
    public Collection<Long> getMemberIds();

}
