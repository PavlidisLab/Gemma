<%@ include file="/common/taglibs.jsp"%>
<p>
<h2><fmt:message key="menu.compassIndexer"/></h2>
<p>
Use the Index button to index the database using Compass::Gps. This will
delete the current index and reindex the database based on the mappings and devices
defined in the Compass::Gps configuration context.
<p>
<form method="post" action="<c:url value="/indexer.html"/>">
	<spring:bind path="command.doIndex">
		<INPUT type="hidden" name="doIndex" value="true" />
	</spring:bind>
    <INPUT type="submit" value="Index"/>
</FORM>
<c:if test="${! empty indexResults}">
	<p>Indexing took: <c:out value="${indexResults.indexTime}" />ms.
</c:if>
<p>
<br>
