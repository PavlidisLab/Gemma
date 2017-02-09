<%@ include file="/common/taglibs.jsp"%>

<head>
	<jwr:script src='/scripts/api/ext/data/DwrProxy.js' />
	<jwr:script src='/scripts/app/loadExpressionExperiment.js' />
</head>

<title>
	<fmt:message key="expressionExperimentLoad.title" />
</title>

<content tag="heading">
	<fmt:message key="expressionExperimentLoad.title" />
</content>

<fmt:message key="expressionExperimentLoad.message" />

<div id="messages" style="margin: 10px; width: 400px"></div>

<table class="detail">
	<tr>
		<th>
			<fmt:message key="expressionExperimentLoad.accession" />
		</th>
		<td>
			<input type="text" name="accession" id="accession" size="40" value="<c:out value="${status.value}"/>" />
			<span class="fieldError"> <c:out value="${status.errorMessage}" /> 
			</span>
		</td>
	</tr>
	<tr>
		<th>
			<fmt:message key="expressionExperimentLoad.arrayExpress" />
			<a class="helpLink" href="?"
				onclick="showHelpTip(event, 'Check if data is to come from ArrayExpress.'); return false">
				<img src="/Gemma/images/help.png" /> 
			</a>
		</th>
		<td align="left">
			<input type="hidden" name="_<c:out value="${status.expression}"/>">
			<input id="arrayExpress" type="checkbox" name="<c:out value="${status.expression}"/>" value="true"
				<c:if test="${status.value}">checked</c:if> />
			<span class="fieldError"> <c:out value="${status.errorMessage}" /> 
			</span>
		</td>
	</tr>
	<tr>
		<th>
			<fmt:message key="expressionExperimentLoad.platformOnly" />
			<a class="helpLink" href="?"
				onclick="showHelpTip(event, 'Load an array design only, not  expression data.'); return false">
				<img src="/Gemma/images/help.png" />
			</a>
		</th>
		<td align="left">
			<input type="hidden" name="_<c:out value="${status.expression}"/>">
			<input type="checkbox" name="<c:out value="${status.expression}"/>" value="true" id="loadPlatformOnly"
				<c:if test="${status.value}">checked</c:if> />
			<span class="fieldError"> <c:out value="${status.errorMessage}" /> 
			</span>
		</td>
	</tr>
	<tr>
		<th>
			<fmt:message key="expressionExperimentLoad.suppressMatching" />
			<a class="helpLink" href="?"
				onclick="showHelpTip(event, 'Check this box if you know that samples were run on only one platform each. Otherwise an attempt will be made to identify biological replicates on different platforms.'); return false"><img
					src="/Gemma/images/help.png" /> </a>
		</th>
		<td align="left">
			<input type="hidden" name="_<c:out value="${status.expression}"/>">
			<input id="suppressMatching" type="checkbox" name="<c:out value="${status.expression}"/>" value="true"
				<c:if test="${status.value}">checked</c:if> />
			<span class="fieldError"> <c:out value="${status.errorMessage}" /> 
			</span>
		</td>
	</tr>
	<tr>
		<th>
			<fmt:message key="expressionExperimentLoad.splitByPlatform" />
			<a class="helpLink" href="?"
				onclick="showHelpTip(event, 'For multi-platform studies, check this box if you want the sample run on each platform to be considered separate experiments. If checked implies suppress matching'); return false">
				<img src="/Gemma/images/help.png" /> 
			</a>
		</th>
		<td align="left">
			<input type="hidden" name="_<c:out value="${status.expression}"/>">
			<input id="splitByPlatform" type="checkbox" name="<c:out value="${status.expression}"/>" value="true"
				<c:if test="${status.value}">checked</c:if> />
			<span class="fieldError"> <c:out value="${status.errorMessage}" /> 
			</span>
		</td>
	</tr>
	<tr>
		<th>
			<fmt:message key="expressionExperimentLoad.allowSuperSeries" />
			<a class="helpLink" href="?"
				onclick="showHelpTip(event, 'If series is a superseries in GEO, allow it to load; leave this unchecked to prevent accidental superseries loading.'); return false">
				<img src="/Gemma/images/help.png" /> 
			</a>
		</th>
		<td align="left">
			<input type="hidden" name="_<c:out value="${status.expression}"/>">
			<input checked id="allowSuperSeriesLoad" type="checkbox" name="<c:out value="${status.expression}"/>"
				value="true" <c:if test="${status.value}">checked</c:if> />
			<span class="fieldError"> <c:out value="${status.errorMessage}" /> 
			</span>
		</td>
	</tr>
	
	
	<tr>
		<th>
			<fmt:message key="expressionExperimentLoad.allowArrayExpressDesign" />
			<a class="helpLink" href="?"
				onclick="showHelpTip(event, 'When loading from ArrayExpress, allow the array design to be imported. It must not be an array design already in Gemma!'); return false">
				<img src="/Gemma/images/help.png" /> 
			</a>
		</th>
		<td align="left">
			<input type="hidden" name="_<c:out value="${status.expression}"/>">
			<input checked id="allowArrayExpressDesign" type="checkbox" name="<c:out value="${status.expression}"/>"
				value="false" <c:if test="${status.value}">checked</c:if> />
			<span class="fieldError"> <c:out value="${status.errorMessage}" /> 
			</span>
		</td>
	</tr>

	<tr>
		<th>
			<fmt:message key="expressionExperimentLoad.arrayDesign" />
		</th>
		<td>
			<div id="arrayDesignCombo"></div>
		</td>
	</tr>
	<tr>
		<td colspan="2" style="padding: 10px" align="center" class="buttonBar">
			<div id="upload-button"></div>
		</td>
	</tr>
</table>

<div id="progress-area" style="padding: 5px;"></div>

<validate:javascript formName="expressionExperimentLoad" staticJavascript="false" />
