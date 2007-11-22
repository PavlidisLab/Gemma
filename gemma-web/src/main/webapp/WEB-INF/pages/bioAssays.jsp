<%@ include file="/common/taglibs.jsp"%>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">

<title><fmt:message key="bioAssays.title" /></title>

<script type="text/javascript" src="<c:url value="/scripts/aa.js"/>"></script>
<h2>
	<fmt:message key="bioAssays.title" />
	for
	<a href='<c:url value="showExpressionExperiment.html" />?id=${expressionExperiment.id }'>${expressionExperiment.shortName}</a>
</h2>
<p>View the <a href='<c:url value="/experimentalDesign/showExperimentalDesign.html?id=${expressionExperiment.id }" />'>Experimental design</a></p>

<aazone tableId="bioAssayList" zone="bioAssayTable" />
<aa:zone name="bioAssayTable">
	<display:table name="bioAssays" class="list"
		requestURI="/Gemma/expressionExperiment/showBioAssaysFromExpressionExperiment.html" id="bioAssayList" sort="list"
		pagesize="50" decorator="ubic.gemma.web.taglib.displaytag.expression.bioAssay.BioAssayWrapper">

		<display:column property="nameLink" sortable="true" titleKey="bioAssay.name" maxWords="20" />
		<display:column property="description" sortable="true" titleKey="bioAssay.description" maxWords="100" />
		<display:column property="delete" sortable="false" title="QC" />

		<display:setProperty name="basic.empty.showtable" value="true" />
	</display:table>
</aa:zone>
<script type="text/javascript" src="<c:url value="/scripts/aa-init.js"/>"></script>