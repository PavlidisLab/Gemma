<%@ include file="/WEB-INF/common/taglibs.jsp"%>

<script type="text/javascript">

	var dirty = false;

	//Perform serach by creating a bookmark and forwarding to search page. 
	doSearch = function( ){
				var query = $('searchfield').value;
				if (valid(query)) {
						location.href='${pageContext.request.contextPath}/searcher.html?query=' + query + '&scope=SEGAP';
				}							
			};
	
	//Did the user hit the enter key?
	isEnterHit = function (e) {
		var keycode;

		if (window.event) keycode = window.event.keyCode;
		else if (e) keycode = e.which;
		else return true;
		
		if (keycode == 13)
		{
			doSearch();
			return false;
		}
		else
		   return true;
	};
	
	//Place to validate the query the user typed in. 
	valid = function(query){
		if (!dirty) {return false;}
		
		if (query == null)
			return false;
		
		if (query.length < 3)
			return false;
			
		return true;
	};

	makeDirty = function() {
		dirty = true;
	}
</script>

<input id="searchfield" type="text" name="query" value="Search Gemma" size="20" onClick="clear();makeDirty()"
	onkeypress="isEnterHit(event)" />
<input id="searchbutton" type="button" value="go" onClick="doSearch()" />