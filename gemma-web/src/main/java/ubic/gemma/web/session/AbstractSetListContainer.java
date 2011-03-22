package ubic.gemma.web.session;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;


import org.apache.commons.collections.BidiMap;
import org.apache.commons.collections.bidimap.DualHashBidiMap;

public abstract class AbstractSetListContainer implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -7207696842986893748L;

	static final int MAX_SIZE = 100;
	
	Long largestSessionId=0l;
	
	ArrayList<GemmaSessionBackedValueObject> setList;
	
	//Map of DB ids to Session Ids (Bidirectional map for one-to-one mappings)
	BidiMap dbIdToSessionIdMap;
	
	public AbstractSetListContainer(){
		setList = new ArrayList<GemmaSessionBackedValueObject>();
		dbIdToSessionIdMap = new DualHashBidiMap();	
	}
	
	//dbResult should be a set of db backed geneSetValueObjects(they should have non null db ids)
	public void setUniqueSetStoreIds(Collection<GemmaSessionBackedValueObject> dbResult){
		
		//give db genesets a unique sessionId so that the javascript widget plays nice
        for (GemmaSessionBackedValueObject vo: dbResult){
        	
        	if (dbIdToSessionIdMap.containsKey(vo.getId())){
        		vo.setSessionId((Long)dbIdToSessionIdMap.get(vo.getId()));        		
        	}
        	else{        		
        		Long newSessionId = incrementAndGetLargestSessionId();        		
        		vo.setSessionId(newSessionId);
        		dbIdToSessionIdMap.put(vo.getId(), newSessionId);
        	}
        }
		
	}
	
	public boolean isDbBackedSessionId(Long id){
		
		return dbIdToSessionIdMap.containsValue(id);
		
	}
	
	public Long getDbIdFromSessionId(Long id){
		
		return (Long)dbIdToSessionIdMap.getKey(id);
	}
	
	public Long incrementAndGetLargestSessionId(){
		largestSessionId = largestSessionId +1;
		
		//unique sessionId for each entry in the user's session(doubt that a user will have over 100000 set session entries however
		// large gaps can occur between sessionIds because of 'fake' sessionIds assigned to db backed sets for the front end store-
		// eg. page reloads within a user's session lifecycle will create these gaps
		//still I believe 100000 provides a large enough range to avoid conflicts
		if (largestSessionId>100000){
			largestSessionId = 0l;
		}
		
		return largestSessionId;
	}
	
	
	
	public GemmaSessionBackedValueObject addSet(GemmaSessionBackedValueObject vo){		
		
		
		boolean setExists=false;
		
		
		//This case shouldn't happen very often (or at all)
		if (vo.getSessionId()!=null){
			for (int i =0 ; i<setList.size(); i++){
			
				Long sid = setList.get(i).getSessionId();
			
				if (sid!=null && sid.equals(vo.getSessionId())){
					setList.remove(i);
					setList.add(i, vo);
					setExists=true;
					break;				
				}
			
			}
		}
		
		if (!setExists){			
			
			vo.setSessionId(incrementAndGetLargestSessionId());
			vo.setSession(true);//this line may be redundant
			
			setList.add(vo);
			if (setList.size()>MAX_SIZE){
				setList.remove(0);
			}
		}
		
		return vo;
		
	}
	
	public void removeSet(GemmaSessionBackedValueObject vo){		
		
		if (vo.getSessionId()!=null){
		
			for (int i =0 ; i<setList.size(); i++){
				
				if (setList.get(i).getSessionId().equals(vo.getSessionId())){
					setList.remove(i);
					break;
				}
				
			}
			
		}
		
	}
	
	public void updateSet(GemmaSessionBackedValueObject vo){		
		
		if (vo.getSessionId()!=null){
		
			for (int i =0 ; i<setList.size(); i++){
				
				if (setList.get(i).getSessionId().equals(vo.getSessionId())){
					setList.remove(i);
					setList.add(i, vo);
					break;
				}
				
			}
			
		}
		
	}
	
	public Collection<GemmaSessionBackedValueObject> getRecentSets(){
		
		return setList;
		
	}
	
	

}
