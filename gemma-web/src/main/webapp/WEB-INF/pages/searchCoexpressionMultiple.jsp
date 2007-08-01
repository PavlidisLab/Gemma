<%@ include file="/common/taglibs.jsp"%>
<%@ page import="java.util.Map"%>
<%@ page import="java.util.Collection"%>
<%@ page import="java.util.Iterator"%>
<%@ page import="ubic.gemma.model.genome.Gene"%>
<jsp:useBean id="multipleCoexpressionSearchCommand" scope="request"
	class="ubic.gemma.web.controller.coexpressionSearch.MultipleCoexpressionSearchCommand" />

<spring:bind path="multipleCoexpressionSearchCommand.*">
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

<c:if test="${coexpressedGenes == null}">
	<title>Multiple coexpression Search</title>
	<h2>
		Search for coexpressed genes:
	</h2>
</c:if>

<c:if test="${coexpressedGenes != null}">
	<title>Multiple coexpression search for <c:out
			value="${sourceGenesDescription}" /></title>
	<h2>
		Results for
		<%
	            Map m = ( Map ) request.getAttribute( "model" );
	            if ( m != null ) {
	                Collection<Gene> sourceGenes = ( Collection<Gene> ) m.get( "sourceGenes" );
	                for ( Iterator iter = sourceGenes.iterator(); iter.hasNext(); ) {
	                    Gene gene = ( Gene ) iter.next();
	                    String separator = iter.hasNext() ? "" : ", ";
	    %>
	    <a
	    	href="/Gemma/gene/showGene.html?id=<%= gene.getId() %>"><%= gene.getOfficialSymbol() %> </a>
	    	( <%= gene.getOfficialName() %> )<%= separator %>
	    <%
	    			}
	    		}
	    %>
	</h2>
	<hr>
	<h4>

		<a
			href="/Gemma/searchCoexpressionMultiple.html?
			geneListString=${coexpressionSearchCommand.geneListString}&
			stringency=${coexpressionSearchCommand.stringency}&
			taxon=${coexpressionSearchCommand.taxon.id}&
			eeSearchString=${coexpressionSearchCommand.eeSearchString}">
			(Bookmarkable link) </a>

	</h4>

	<table class="datasummary" style="float:right">
		<tr>
			<td colspan=2 align=center>
				<b> Search Summary </b>
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
				Genes
			</td>
		</tr>
		<tr>
			<td>
				&nbsp;&nbsp;Found
			</td>
			<td>
				<c:out value="${numGenes}" />
			</td>
		</tr>
		<authz:authorize ifAnyGranted="admin">
			<tr>
				<td colspan=2>
					Predicted Genes
				</td>
			</tr>
			<tr>
				<td>
					&nbsp;&nbsp;Found
				</td>
				<td>
					<c:out value="${numPredictedGenes}" />
				</td>
			</tr>
			<tr>
				<td colspan=2>
					Probe Aligned Regions
				</td>
			</tr>
			<tr>
				<td>
					&nbsp;&nbsp;Found
				</td>
				<td>
					<c:out value="${numProbeAlignedRegions}" />
				</td>
			</tr>
		</authz:authorize>
	</table>
</c:if>

<form method="post" name="multipleCoexpressionSearch"
	action="<c:url value="/searchCoexpressionMultiple.html"/>">

	<table class='searchTable'>
		<tr>
			<td class='label' valign="top">
				<b> Gene Names </b>
			</td>
			<td>
				<spring:bind path="multipleCoexpressionSearchCommand.geneListString">
					<textarea cols="30" rows="8"
						name="<c:out value="${status.expression}"/>"
						value="<c:out value="${sourceGenesDescription}"/>">
					</textarea>
				</spring:bind>
			</td>
			<td>
				<a class="helpLink" href="?"
					onclick="showHelpTip(event, 
				'A list of genes; one gene per line.'); return false">
					<img src="/Gemma/images/help.png" /> </a>
			</td>
		</tr>

		<tr>
			<td class='label' valign="top">
				<b> Experiment keywords </b>
			</td>
			<td>
				<spring:bind path="multipleCoexpressionSearchCommand.eeSearchString">
					<input type="text" size=30
						name="<c:out value="${status.expression}"/>"
						value="<c:out value="${status.value}"/>" />
				</spring:bind>
			</td>
			<td>
				<a class="helpLink" href="?"
					onclick="showHelpTip(event, 
				'keywords of experiments to use in the coexpression analysis'); return false">
					<img src="/Gemma/images/help.png" /> </a>
			</td>
		</tr>

		<tr>
			<td class='label' valign="top">
				<b> <fmt:message key="label.species" /> </b>
			</td>


			<td>
				<spring:bind path="multipleCoexpressionSearchCommand.taxon">
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
				<a class="helpLink" href="?"
					onclick="showHelpTip(event, 
				'Species to use in the coexpression search'); return false">
					<img src="/Gemma/images/help.png" /> </a>
			</td>
		</tr>

		<tr>
			<td class='label' valign="top">
				<b> <fmt:message key="label.stringency" /> </b>
			</td>
			<td>
				<spring:bind path="multipleCoexpressionSearchCommand.stringency">
					<input type="text" size=1
						name="<c:out value="${status.expression}"/>"
						value="<c:out value="${status.value}"/>" />
				</spring:bind>
			</td>
			<td>
				<a class="helpLink" href="?"
					onclick="showHelpTip(event, 
				'The number of datasets (experiments) that exhibit pairwise coexpression with a query gene before it is considered a positive result'); return false">
					<img src="/Gemma/images/help.png" /> </a>
			</td>
		</tr>
		<tr>
			<td>
				&nbsp;
			</td>
			<td>
				<input type="submit" class="button" name="submit"
					value="<fmt:message key="button.search"/>" />
			</td>
			<td>
				&nbsp;
			</td>
		</tr>

	</table>
</form>

<c:if test="${coexpressedGenes != null}">
	<c:if test="${numLinkedExpressionExperiments != 0}">

		<script type='text/javascript' src='/Gemma/scripts/prototype.js'></script>
		<script type='text/javascript'
			src='/Gemma/scripts/expandableObjects.js'></script>

		<!-- Toggles for the expand/hide datasetList table -->
		<span class="datasetList"
			onclick="return toggleVisibility('datasetList')"> <img
				src="/Gemma/images/chart_organisation_add.png" /> </span>
		<span class="datasetList" style="display:none"
			onclick="return toggleVisibility('datasetList')"> <img
				src="/Gemma/images/chart_organisation_delete.png" /> </span>
	</c:if>
	<b> <c:out value="${numLinkedExpressionExperiments}" /> datasets had relevant coexpression data
		<a href="#" onclick="return toggleVisibility('datasetList')">(details)</a>
		<br /> &emsp;&nbsp;
		<c:out value="${numStringencyGenes}" /> of <c:out value="${numGenes}" /> genes exhibited coexpression with at least
		<c:out value="${minimumCommonQueryGenes}" /> of <c:out value="${sourceGenesDescription}" />
		</b>
	<%--
	<b> <c:out value="${numUsedExpressionExperiments}" /> datasets had
		relevent coexpression data <br /> &emsp;&nbsp; <c:out
			value="${numQuerySpecificEEs}" /> datasets probes for <c:out
			value="${sourceGene.officialSymbol}" /> without detected
		cross-hybridization potential. <c:if
			test="${numLinkedExpressionExperiments != 0}">
			<a href="#" onclick="return toggleVisibility('datasetList')">(details)</a>
		</c:if> </b>
	--%>
	
	<br />
	<div class="datasetList" style="display:none">

		<display:table pagesize="200" name="expressionExperiments" sort="list"
			defaultsort="2" class="list" requestURI=""
			id="expressionExperimentList"
			decorator="ubic.gemma.web.taglib.displaytag.expression.experiment.ExpressionExperimentWrapper">

			<display:column property="rawCoexpressionLinkCount" sortable="true"
				defaultorder="descending"
				titleKey="expressionExperiment.rawLinkcount" />

			<display:column property="coexpressionLinkCount" sortable="true"
				defaultorder="ascending" titleKey="expressionExperiment.linkcount" />

			<display:column property="nameLink" sortable="true"
				sortProperty="name" titleKey="expressionExperiment.name" />

			<display:column property="shortName" sortable="true"
				titleKey="expressionExperiment.shortName" />

			<display:column property="specific" sortable="true"
				titleKey="expressionExperiment.specific" />


			<authz:authorize ifAnyGranted="admin">
				<display:column property="arrayDesignLink" sortable="true"
					title="Arrays" />
			</authz:authorize>


			<display:column property="assaysLink" sortable="true"
				titleKey="bioAssays.title" />

			<display:setProperty name="basic.empty.showtable" value="false" />
		</display:table>
	</div>
</c:if>


<br />

<script>
			var text = '<Gemma:help helpFile="CoexpressionSearchSupportHelp.html"/>';
			function getCoexpressionHelp(event) {showWideHelpTip(event,text); }
	
			var text2 = '<Gemma:help helpFile="CoexpressionSearchOverlapHelp.html"/>';
			function getOverlapHelp(event) {showWideHelpTip(event,text2); }
			
			var text3 = '<Gemma:help helpFile="ExpressionExperimentBits.html"/>';
			function getExpBitsHelp(event) {showWideHelpTip(event,text3); }
		</script>

<div id="coexpressed-genes" style="width:600px;">
	<display:table name="coexpressedGenes" class="list" sort="list"
		requestURI="" id="foundGenes"
		decorator="ubic.gemma.web.taglib.displaytag.coexpressionSearch.MultipleCoexpressionWrapper"
		pagesize="200">
		<display:column property="nameLink" sortable="true"
			sortProperty="geneName" titleKey="gene.name" />
		<display:column property="geneOfficialName" maxLength="50"
			sortable="true" titleKey="gene.officialName" />
		<display:column property="coexpressedQueryGenes" sortable="true"
			sortProperty="maxLinkCount"
			title="Coexpressed Query Genes <a  class='helpLink' name='?' href='' onclick='getExpBitsHelp(event);return false;'>
					<img src='/Gemma/images/help.png' /> </a>" />
		<display:column property="experimentBitImage" sortable="false"
			title="exps <a  class='helpLink' name='?' href='' onclick='getExpBitsHelp(event);return false;'>
					<img src='/Gemma/images/help.png' /> </a>" />
		<display:setProperty name="basic.empty.showtable" value="false" />
	</display:table>
</div>

<authz:authorize ifAnyGranted="admin">

	<!--  ================ Predicted Gene Results ================== -->

	<c:if test="${coexpressedPredictedGenes != null}">

		<c:if test="${numLinkedPredictedExpressionExperiments != 0}">

			<!-- Toggles for the expand/hide datasetList table -->
			<span class="predictedDatasetList"
				onclick="return toggleVisibility('predictedDatasetList')"> <img
					src="/Gemma/images/chart_organisation_add.png" /> </span>
			<span class="predictedDatasetList" style="display:none"
				onclick="return toggleVisibility('predictedDatasetList')"> <img
					src="/Gemma/images/chart_organisation_delete.png" /> </span>
		</c:if>
		<b> <c:out value="${numLinkedPredictedExpressionExperiments}" /> datasets had relevant coexpression data
		<a href="#" onclick="return toggleVisibility('datasetList')">(details)</a>
		<br /> &emsp;&nbsp;
		<c:out value="${numStringencyPredictedGenes}" /> of <c:out value="${numPredictedGenes}" /> predicted genes exhibited coexpression with at least
		<c:out value="${minimumCommonQueryGenes}" /> of <c:out value="${sourceGenesDescription}" />
		</b>
		<%--
		<b> <c:out value="${numUsedPredictedExpressionExperiments}" />
			datasets that had relevent Predicted Gene Coexpression data <c:if
				test="${numLinkedPredictedExpressionExperiments != 0}">
				<a href="#"
					onclick="return toggleVisibility('predictedDatasetList')">(details)</a>
			</c:if> </b>
		--%>
		
		<br />
		<div class="predictedDatasetList" style="display:none">

			<display:table pagesize="100" name="predictedExpressionExperiments"
				sort="list" defaultsort="2" class="list" requestURI=""
				id="predictedExpressionExperimentList"
				decorator="ubic.gemma.web.taglib.displaytag.expression.experiment.ExpressionExperimentWrapper">

				<display:column property="rawCoexpressionLinkCount" sortable="true"
					defaultorder="descending"
					titleKey="expressionExperiment.rawLinkcount" />

				<display:column property="coexpressionLinkCount" sortable="true"
					defaultorder="descending" titleKey="expressionExperiment.linkcount" />

				<display:column property="nameLink" sortable="true"
					sortProperty="name" titleKey="expressionExperiment.name" />

				<display:column property="shortName" sortable="true"
					titleKey="expressionExperiment.shortName" />

				<authz:authorize ifAnyGranted="admin">
					<display:column property="arrayDesignLink" sortable="true"
						title="Arrays" />
				</authz:authorize>


				<display:column property="assaysLink" sortable="true"
					titleKey="bioAssays.title" />

				<display:setProperty name="basic.empty.showtable" value="false" />
			</display:table>
		</div>
	</c:if>

	<br />
	<display:table name="coexpressedPredictedGenes" class="list"
		sort="list" requestURI="" id="predictedFoundGenes"
		decorator="ubic.gemma.web.taglib.displaytag.coexpressionSearch.MultipleCoexpressionWrapper"
		pagesize="200">
		<display:column property="nameLink" sortable="true"
			sortProperty="geneName" titleKey="gene.name" />
		<display:column property="geneOfficialName" maxLength="50"
			sortable="true" titleKey="gene.officialName" />
		<display:column property="coexpressedQueryGenes" sortable="true"
			sortProperty="maxLinkCount"
			title="Coexpressed Query Genes <a  class='helpLink' name='?' href='' onclick='getExpBitsHelp(event);return false;'>
					<img src='/Gemma/images/help.png' /> </a>" />
		<display:column property="experimentBitImage" sortable="false"
			title="exps <a  class='helpLink' name='?' href='' onclick='getExpBitsHelp(event);return false;'>
					<img src='/Gemma/images/help.png' /> </a>" />
		<display:setProperty name="basic.empty.showtable" value="false" />
	</display:table>

	<!--  ================ Probe Aligned Region Results ================== -->

	<c:if test="${coexpressedAlignedRegions != null}">

		<c:if test="${numLinkedAlignedExpressionExperiments != 0}">

			<!-- Toggles for the expand/hide datasetList table -->
			<span class="alignedDatasetList"
				onclick="return toggleVisibility('alignedDatasetList')"> <img
					src="/Gemma/images/chart_organisation_add.png" /> </span>
			<span class="alignedDatasetList" style="display:none"
				onclick="return toggleVisibility('alignedDatasetList')"> <img
					src="/Gemma/images/chart_organisation_delete.png" /> </span>
		</c:if>
		<b> <c:out value="${numLinkedAlignedExpressionExperiments}" /> datasets had relevant coexpression data
		<a href="#" onclick="return toggleVisibility('datasetList')">(details)</a>
		<br /> &emsp;&nbsp;
		<c:out value="${numStringencyProbeAlignedRegions}" /> of <c:out value="${numProbeAlignedRegions}" /> probe-aligned regions exhibited coexpression with at least
		<c:out value="${minimumCommonQueryGenes}" /> of <c:out value="${sourceGenesDescription}" />
		</b>
		<%--
		<b> <c:out value="${numUsedAlignedExpressionExperiments}" />
			datasets had relevent Probe Aligned Region Coexpression data <c:if
				test="${numLinkedAlignedExpressionExperiments != 0}">
				<a href="#" onclick="return toggleVisibility('alignedDatasetList')">(details)</a>
			</c:if> </b>
		--%>

		<br />
		<div class="alignedDatasetList" style="display:none">

			<display:table pagesize="100" name="alignedExpressionExperiments"
				sort="list" defaultsort="2" class="list" requestURI=""
				id="alignedExpressionExperimentList"
				decorator="ubic.gemma.web.taglib.displaytag.expression.experiment.ExpressionExperimentWrapper">

				<display:column property="rawCoexpressionLinkCount" sortable="true"
					defaultorder="descending"
					titleKey="expressionExperiment.rawLinkcount" />

				<display:column property="coexpressionLinkCount" sortable="true"
					defaultorder="descending" titleKey="expressionExperiment.linkcount" />

				<display:column property="nameLink" sortable="true"
					sortProperty="name" titleKey="expressionExperiment.name" />

				<display:column property="shortName" sortable="true"
					titleKey="expressionExperiment.shortName" />

				<authz:authorize ifAnyGranted="admin">
					<display:column property="arrayDesignLink" sortable="true"
						title="Arrays" />
				</authz:authorize>


				<display:column property="assaysLink" sortable="true"
					titleKey="bioAssays.title" />

				<display:setProperty name="basic.empty.showtable" value="false" />
			</display:table>
		</div>
	</c:if>

	<br />
	<display:table name="coexpressedAlignedRegions" class="list"
		sort="list" requestURI="" id="alignedFoundGenes"
		decorator="ubic.gemma.web.taglib.displaytag.coexpressionSearch.MultipleCoexpressionWrapper"
		pagesize="200">
		<display:column property="nameLink" sortable="true"
			sortProperty="geneName" titleKey="gene.name" />
		<display:column property="geneOfficialName" maxLength="50"
			sortable="true" titleKey="gene.officialName" />
		<display:column property="coexpressedQueryGenes" sortable="true"
			sortProperty="maxLinkCount"
			title="Coexpressed Query Genes <a  class='helpLink' name='?' href='' onclick='getExpBitsHelp(event);return false;'>
					<img src='/Gemma/images/help.png' /> </a>" />
		<display:column property="experimentBitImage" sortable="false"
			title="exps <a  class='helpLink' name='?' href='' onclick='getExpBitsHelp(event);return false;'>
					<img src='/Gemma/images/help.png' /> </a>" />
		<display:setProperty name="basic.empty.showtable" value="false" />
	</display:table>
</authz:authorize>