<%@ include file="/common/taglibs.jsp"%>
<form id="search-form" name="quickSearch" action="<c:url value="/searcher.html"/>" method="get">
	<input id="searchfield" type="text" name="query" value="Search Gemma" size="20"  onClick="clear()" />
	<input id="searchbutton" type="submit" name="submit" value="go" />
</form>
