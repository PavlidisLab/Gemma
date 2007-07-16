<%@ include file="/common/taglibs.jsp"%>

<p>
Use the different index buttons to index the database. This will
delete the current index and reindex the database based on the mappings and devices
defined in the compass configuration context.
</p>

<br/>

<c:if test="${time != null}">
	It took <c:out value="${time}"/> to index the <c:out value="${description}"/>
	<br/> <br/>	
</c:if>

<form method="post" action="<c:url value="/indexer.html"/>">

    <input type="submit" name="eeIndex" value="eeIndex"/>
    <input type="submit" name="geneIndex" value="geneIndex"/>
    <input type="submit" name="arrayIndex" value="arrayIndex"/>
    <input type="submit" name="ontologyIndex" value="ontologyIndex"/>
    <input type="submit" name="bibliographicIndex" value="bibliographicIndex"/>
    
</form>


<p>
<br>