<%@ include file="/common/taglibs.jsp"%>

<title><fmt:message key="bioAssays.title" />
</title>

<h2>
	<fmt:message key="bioAssays.title" />
	for
	<a href='<c:url value="showExpressionExperiment.html" />?id=${expressionExperiment.id }'>${expressionExperiment.shortName}</a>
</h2>
<p>
	View the
	<a href='<c:url value="/experimentalDesign/showExperimentalDesign.html?eeid=${expressionExperiment.id }" />'>Experimental
		design</a>
</p>

<Gemma:expressionQC ee="${expressionExperiment.id}" />

<display:table name="bioAssays" class="list"
	requestURI="/Gemma/expressionExperiment/showBioAssaysFromExpressionExperiment.html" id="bioAssayList" sort="list"
	pagesize="50" decorator="ubic.gemma.web.taglib.displaytag.expression.bioAssay.BioAssayWrapper">

	<display:column property="nameLink" sortable="true" titleKey="bioAssay.name" maxWords="20" />
	<display:column property="description" sortable="true" titleKey="bioAssay.description" maxWords="100" />

	<security:authorize ifAnyGranted="GROUP_ADMIN">
		<display:column property="delete" sortable="false" title="QC" />
	</security:authorize>

	<display:setProperty name="basic.empty.showtable" value="true" />
</display:table>
