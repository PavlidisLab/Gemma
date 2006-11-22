<%-- $Id$ --%>
<%@ include file="/common/taglibs.jsp"%>
<html>
	<head>
		<content tag="heading">
		Array designs
		</content>
		<title>Array designs</title>
	</head>
	<body>
		<h2>
			Search results
		</h2>
		<h3>
			<c:out value="${numArrayDesigns }" />
			Array Designs found.
		</h3>
		<table>
			<tr>
				<display:table name="arrayDesigns" sort="list" class="list" requestURI="" id="arrayDesignList"
				pagesize="30" decorator="ubic.gemma.web.taglib.displaytag.expression.arrayDesign.ArrayDesignWrapper">
					<display:column property="name" sortable="true" href="showArrayDesign.html" paramId="id" paramProperty="id"
						titleKey="arrayDesign.name" />
					<display:column property="shortName" sortable="true" titleKey="arrayDesign.shortName" />
					<display:column property="expressionExperimentCountLink" sortable="true" title="Expts" />
					<authz:authorize ifAnyGranted="admin">
						<display:column sortable="false" href="deleteArrayDesign.html" paramId="id" paramProperty="id"
							titleKey="arrayDesign.delete">Delete</display:column>
					</authz:authorize>
					<display:setProperty name="basic.empty.showtable" value="true" />
				</display:table>
			</tr>
		</table>
	</body>
</html>

