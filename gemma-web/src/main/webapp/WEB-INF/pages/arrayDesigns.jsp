<%-- $Id$ --%>
<%@ include file="/common/taglibs.jsp"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD html 4.01 Transitional//EN">
<html>
	<head>
	</head>
	<body>
		<div align="left">
			<P>
			<table width="100%">
				<tr>
					<td>
						<div align="left">
							<b>Search Results</b>
						</div>
					</td>
				</tr>
				<tr>
					<td>
						<hr>
					</td>
				</tr>
				<tr>
					<display:table name="arrayDesigns" class="list" requestURI=""
						id="arrayDesignList" export="true">
						<display:column property="name" sortable="true"
							href="showArrayDesign.html" paramId="name" paramProperty="name"
							titleKey="arrayDesign.name" />
						<display:column property="description" sortable="true"
							titleKey="arrayDesign.description" />
						<display:column title="Design Elements" sortable="true"
							href="../designElement/showAllDesignElements.html" paramId="name"
							paramProperty="name"
							property="advertisedNumberOfDesignElements"
							titleKey="arrayDesign.advertisedNumberOfDesignElements">
							
						</display:column>
						<display:setProperty name="basic.empty.showtable" value="true" />
					</display:table>
				</tr>
				<tr>
					<td>
						<hr />
					</td>
				</tr>
			</table>
		</div>
	</body>
</html>

