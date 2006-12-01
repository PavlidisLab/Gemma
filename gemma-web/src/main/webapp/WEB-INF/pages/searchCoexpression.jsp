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

<c:if test="${numCoexpressedGenes != null}">
<table class="datasummary">
	<tr>
		<td colspan=2>
			<b>Coexpression Summary</b>
		</td>
	</tr>
	<tr>
		<td>
			Datasets searched
		</td>
		<td>
			<c:out value="${numExpressionExperiments}" />
		</td>
	</tr>
	<tr>
		<td>
			Unique links
		</td>
		<td>
			<c:out value="${numMatchedLinks}" />
		</td>
	</tr>
	<tr>
		<td>
			Links that met stringency
		</td>
		<td>
			<c:out value="${numCoexpressedGenes}" />
		</td>
	</tr>
</table>
</c:if>


<form method="post" name="coexpressionSearch"
	action="<c:url value="/searchCoexpression.html"/>">

	<table>
		<tr>
			<td valign="top">
				<b> Gene Name </b>
			</td>
			<td>
			<!-- If there are genes defined, just show those genes in a pulldown menu. 
				Otherwise, show a text field
			 -->
			 	<c:if test="${genes != null}">
					<spring:bind path="coexpressionSearchCommand.searchString">
						<select name="${status.expression}">
							<c:forEach items="${genes}" var="gene">
								<option value="${gene.officialSymbol}">
									${gene.officialSymbol} : ${gene.officialName }
								</option>
							</c:forEach>
						</select>
					
					</spring:bind>					
				</c:if>
				<c:if test="${genes == null}">
					<spring:bind path="coexpressionSearchCommand.searchString">
						<input type="text" size=15
							name="<c:out value="${status.expression}"/>"
							value="<c:out value="${status.value}"/>" />
					</spring:bind>
				</c:if>
				

				<spring:bind path="coexpressionSearchCommand.exactSearch">
					<input type="checkbox" name="${status.expression}" 
					<c:if test="${status.value}">checked="checked"</c:if> 
					/>
					<input type="hidden" name="_<c:out value="${status.expression}"/>">
				</spring:bind>
				Exact search
			</td>
			<td>
				<a class="helpLink" href="?" onclick="showHelpTip(event, 
				'Official symbol of a gene'); return false">
				<img src="/Gemma/images/help.png" />
				</a>
			</td>
		</tr>
		
		<tr>
			<td valign="top">
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
			<td valign="top">
				<b> <fmt:message key="label.species" /> </b>
			</td>
			
			
			<td>
				<spring:bind path="coexpressionSearchCommand.taxon">
					<select name="${status.expression}">
						<c:forEach items="${taxa}" var="taxon">
							<spring:transform value="${taxon}" var="scientificName" />
							<option value="${scientificName}"
								<c:if test="${status.value == taxon}">selected </c:if>>
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
			<td valign="top">
				<b> <fmt:message key="label.stringency" /> </b>
			</td>
			<td>
				<spring:bind path="coexpressionSearchCommand.stringency">
					<input "type="text" size=1
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

	</table>
	<br />
	
	<table>
		<tr>
			<td>
				<input type="submit" class="button" name="submit"
					value="<fmt:message key="button.submit"/>" />
			</td>
		</tr>
	</table>

<display:table name="coexpressedGenes"
	class="list" sort="list" requestURI="" id="foundGenes" 
	decorator="ubic.gemma.web.taglib.displaytag.coexpressionSearch.CoexpressionWrapper" 
	pagesize="200">
	<display:column property="nameLink" sortable="true" sortProperty="geneName" titleKey="gene.name" />
	<display:column property="geneOfficialName" maxLength="50" sortable="true" titleKey="gene.officialName" />
	<display:column property="dataSetCount" sortable="true" title="#DS" />	
	<display:column property="dataSets" title="Data Sets" />	
	<display:setProperty name="basic.empty.showtable" value="false" />
</display:table>

</form>
