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
		<table>
			<tr>
				<display:table name="arrayDesigns" class="list" requestURI="" id="arrayDesignList" export="true">
					<display:column property="name" sortable="true" href="showArrayDesign.html" paramId="id" paramProperty="id"
						titleKey="arrayDesign.name" />
					<display:column property="description" sortable="true" titleKey="arrayDesign.description" />
					<display:column title="Design Elements" sortable="true" property="advertisedNumberOfDesignElements"
						titleKey="arrayDesign.advertisedNumberOfDesignElements">
					</display:column>
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

