<%@ include file="/common/taglibs.jsp"%>

<jsp:useBean id="coexpressionSearchCommand" scope="request"
	class="ubic.gemma.web.controller.coexpressionSearch.CoexpressionSearchCommand" />

<spring:bind path="coexpressionSearchCommand.*">
	<c:if test="${not empty status.errorMessages}">
		<div class="error">
			<c:forEach var="error" items="${status.errorMessages}">
				<img src="<c:url value="/images/iconWarning.gif"/>"
					alt="<fmt:message key="icon.warning"/>" class="icon" />
				<c:out value="${error}" escapeXml="false" />
				<br />
			</c:forEach>
		</div>
	</c:if>
</spring:bind>

<c:if test="${numCoexpressedGenes == null}">
	<title>Coexpression Search</title> 
	<h2>
		Search for coexpressed genes:
	</h2>
</c:if>

<c:if test="${coexpressedGenes != null}">
<title>Coexpression search for <c:out value="${sourceGene.officialSymbol}" /> (<c:out value="${sourceGene.officialName}" />)</title>
<h2>
Results for 
	<a href="/Gemma/gene/showGene.html?id=<c:out value="${sourceGene.id}" />">
		<c:out value="${sourceGene.officialSymbol}" /> 
	</a>

	(<c:out value="${sourceGene.officialName}" />)
</h2>
	<h4>

			<a href="/Gemma/searchCoexpression.html?
			searchString=${coexpressionSearchCommand.searchString}&
			stringency=${ coexpressionSearchCommand.stringency}&
			taxon=${coexpressionSearchCommand.taxon.scientificName}&
			eeSearchString=${coexpressionSearchCommand.eeSearchString}&
			exactSearch=on">
				(Bookmarkable link)
			</a>

	</h4>
<table class="datasummary">
	<tr>
		<td colspan=2 align=center>
			<b>
				Search Summary
			</b>
		</td>
	</tr>
	<tr>
		<td>
			Datasets searched
		</td>
		<td>
			<c:out value="${numSearchedExpressionExperiments}" />
		</td>
	</tr>
	<tr>
		<td colspan=2>
			Links
		</td>
	</tr>
	<tr>
		<td>
			&nbsp;&nbsp;Found
		</td>
		<td>
			<c:out value="${numMatchedLinks}" />
		</td>
	</tr>
	<tr>
		<td>
			&nbsp;&nbsp;Met stringency (+)
		</td>
		<td>
			<c:out value="${numPositiveCoexpressedGenes}" />
		</td>
	</tr>
	<tr>
		<td>
			&nbsp;&nbsp;Met stringency (-)
		</td>
		<td>
			<c:out value="${numNegativeCoexpressedGenes}" />
		</td>
	</tr>
	
</table>
</c:if>

<form method="post" name="coexpressionSearch"
	action="<c:url value="/searchCoexpression.html"/>">

	<table class='searchTable'>
		<tr>
			<td class='label' valign="top">
				<b> Gene Name </b>
			</td>
			<td>
			<!-- If there are genes defined, just show those genes in a pulldown menu. 
				Otherwise, show a text field
			 -->
			 	<c:if test="${genes != null}">
					<spring:bind path="coexpressionSearchCommand.searchString">
						<select id="searchStringPullDown" name="${status.expression}" 
						onchange="var popup = document.getElementById('searchStringPullDown');
						if (popup.value == 'Refine Search') {
							popup.style.display='none';
							popup.disabled='true';
							var text = document.getElementById('searchStringTextInput');
							text.style.display='inline';
							text.disabled='';
							var checkbox = document.getElementById('exactSearchCheckbox');
							checkbox.checked='';
							checkbox.style.display='inline';
							document.getElementById('exactSearchLabel').style.display='inline';
						}
						">
							<option value="">[Select a gene or refine the search]</option>
							<option value="Refine Search">[Refine Search]</option>
							<c:forEach items="${genes}" var="gene">
								<option value="${gene.officialSymbol}">
									${gene.officialSymbol} : ${gene.officialName }
								</option>
							</c:forEach>
						</select>		
						<input id="searchStringTextInput" disabled style="display:none" type="text" size=15
							name="<c:out value="${status.expression}"/>"
							value="${status.value}" />
					</spring:bind>		
					<spring:bind path="coexpressionSearchCommand.exactSearch">
						<input style="display:none" id="exactSearchCheckbox" type="checkbox" name="${status.expression}" 
						<c:if test="${status.value == 'on'}">checked="checked"</c:if> 
						/>
						<input type="hidden" name="_<c:out value="${status.expression}"/>">
					</spring:bind>
					<span id="exactSearchLabel" style="display:none">
					Exact search			
					</span>
				</c:if>
				
				
				
			 	<c:if test="${genes == null}">
					<spring:bind path="coexpressionSearchCommand.searchString">
						<input type="text" size=15
							name="<c:out value="${status.expression}"/>"
							value="<c:out value="${status.value}"/>" />
					</spring:bind>
					<spring:bind path="coexpressionSearchCommand.exactSearch">
						<input id="exactSearchCheckbox" type="checkbox" name="${status.expression}" 
						<c:if test="${status.value == 'on'}">checked="checked"</c:if> 
						/>
						<input type="hidden" name="_<c:out value="${status.expression}"/>">
					</spring:bind>
					Exact search
				</c:if>
			</td>
			<td>
				<a class="helpLink" href="?" onclick="showHelpTip(event, 
				'Term(s) to identify a gene; check \'exact search\' if you know the official symbol for the gene.'); return false">
				<img src="/Gemma/images/help.png" />
				</a>
			</td>
		</tr>
		
		<tr>
			<td  class='label'  valign="top">
				<b> Experiment keywords </b>
			</td>
			<td>
				<spring:bind path="coexpressionSearchCommand.eeSearchString">
					<input type="text" size=30
						name="<c:out value="${status.expression}"/>"
						value="<c:out value="${status.value}"/>" />
				</spring:bind>
			</td>
			<td>
				<a class="helpLink" href="?" onclick="showHelpTip(event, 
				'keywords of experiments to use in the coexpression analysis'); return false">
				<img src="/Gemma/images/help.png" />
				</a>
			</td>
		</tr>
		
		<tr>
			<td  class='label'  valign="top">
				<b> <fmt:message key="label.species" /> </b>
			</td>
			
			
			<td>
				<spring:bind path="coexpressionSearchCommand.taxon">
					<select name="${status.expression}">
						<c:forEach items="${taxa}" var="taxon">
							<spring:transform value="${taxon}" var="scientificName" />
							<option value="${scientificName}"
								<c:if test="${status.value == scientificName}">selected </c:if>>
								${scientificName}
							</option>
						</c:forEach>
					</select>
					
				</spring:bind>
			</td>
			<td>
				<a class="helpLink" href="?" onclick="showHelpTip(event, 
				'Species to use in the coexpression search'); return false">
				<img src="/Gemma/images/help.png" />
				</a>
			</td>
		</tr>

		<tr>
			<td  class='label'  valign="top">
				<b> <fmt:message key="label.stringency" /> </b>
			</td>
			<td>
				<spring:bind path="coexpressionSearchCommand.stringency">
					<input type="text" size=1
						name="<c:out value="${status.expression}"/>"
						value="<c:out value="${status.value}"/>" />
				</spring:bind>
			</td>
			<td>
				<a class="helpLink" href="?" onclick="showHelpTip(event, 
				'The number of datasets (experiments) that coexpress the gene before it is considered a positive result'); return false">
				<img src="/Gemma/images/help.png" />
				</a>
			</td>
		</tr>
		<tr>
			<td>&nbsp;</td>
			<td>
				<input type="submit" class="button" name="submit"
				value="<fmt:message key="button.search"/>" />
			</td>
			<td>&nbsp;</td>
		</tr>
		
	</table>

<c:if test="${coexpressedGenes != null}">
<c:if test="${numLinkedExpressionExperiments != 0}">
<script type='text/javascript' src='/Gemma/scripts/expandableObjects.js'></script>

<!-- Toggles for the expand/hide datasetList table -->
<span name="datasetList" onclick="return toggleVisibility('datasetList')">
	<img src="/Gemma/images/chart_organisation_add.png" />
</span>
<span name="datasetList" style="display:none" onclick="return toggleVisibility('datasetList')">
	<img src="/Gemma/images/chart_organisation_delete.png" />
</span>
</c:if>
<b> 
<c:out value="${numLinkedExpressionExperiments}" /> datasets appear in search results 
<c:if test="${numLinkedExpressionExperiments != 0}">
<a href="#" onclick="return toggleVisibility('datasetList')" >(details)</a> 
</c:if>
</b>


<br />
<div name="datasetList" style="display:none">
	
	<display:table pagesize="100" name="expressionExperiments" sort="list" class="list" requestURI="" id="expressionExperimentList"
		decorator="ubic.gemma.web.taglib.displaytag.expression.experiment.ExpressionExperimentWrapper">

			<display:column property="nameLink" sortable="true" sortProperty="name" titleKey="expressionExperiment.name" />

			<display:column property="shortName" sortable="true" titleKey="expressionExperiment.shortName" />

			<authz:authorize ifAnyGranted="admin">
	 			<display:column property="arrayDesignLink" sortable="true" title="Arrays" />
			</authz:authorize>
		

 			<display:column property="assaysLink" sortable="true" titleKey="bioAssays.title" />

			<display:setProperty name="basic.empty.showtable" value="false" />
		</display:table>
</div>
</c:if>


<br />
	
	

<display:table name="coexpressedGenes"
	class="list" sort="list" requestURI="" id="foundGenes" 
	decorator="ubic.gemma.web.taglib.displaytag.coexpressionSearch.CoexpressionWrapper" 
	pagesize="200">
	<display:column property="nameLink" sortable="true" sortProperty="geneName" titleKey="gene.name" />
	<display:column property="geneOfficialName" maxLength="50" sortable="true" titleKey="gene.officialName" />
	<display:column property="dataSetCount" sortable="true" title="#DS" />	
	<display:setProperty name="basic.empty.showtable" value="false" />
</display:table>

</form>
