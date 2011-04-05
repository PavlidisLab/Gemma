package ubic.gemma.persistence;

import ubic.gemma.model.Reference;

public interface GemmaSessionBackedValueObject {
	
	public Long getSessionId();
	
	public Reference getReference();
	
    public void setReference(Reference reference);
	
	public void setSessionId(Long l);
	
	public boolean isSession();
	
	public void setSession(boolean b);
	
	public Long getId();

    public void setId( Long newId );

}
