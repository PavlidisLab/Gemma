<%@ include file="/common/taglibs.jsp"%>

<head>
	<jwr:script src='/scripts/api/ext/data/DwrProxy.js' />
	<jwr:script src='/scripts/app/loadExpressionExperiment.js' />
</head>

<title>
	<fmt:message key="expressionExperimentLoad.title" />
</title>

<content tag="heading"> 
	<div class="padded">
		<fmt:message key="expressionExperimentLoad.title" /> 
	</div>
</content>

<div class="padded">
	<fmt:message key="expressionExperimentLoad.message" />
</div>

<div id="messages" class="padded" style="margin: 10px; width: 400px;"></div>

<div class="padded">
	<table class="detail row-separated info-boxes" style="width: 600px;">
		<tr>
			<td>
				<fmt:message key="expressionExperimentLoad.accession" />
			</td>
			<td></td>
			<td>
				<input type="text" name="accession" id="accession" size="40"
					value="<c:out value="${status.value}"/>" />
				<span class="fieldError"> <c:out
						value="${status.errorMessage}" />
				</span>
			</td>
		</tr>
		<tr>
			<td>
				<fmt:message key="expressionExperimentLoad.arrayExpress" />
			</td>
			<td align="center">
				<a class="helpLink" href="?"
					onclick="showHelpTip(event, 'Check if data is to come from ArrayExpress.'); return false">
					<img src="/Gemma/images/help.png" />
				</a>
			</td>
			<td align="left">
				<input type="hidden" name="_<c:out value="${status.expression}"/>">
				<input id="arrayExpress" type="checkbox"
					name="<c:out value="${status.expression}"/>" value="true"
					<c:if test="${status.value}">checked</c:if> />
				<span class="fieldError"> <c:out
						value="${status.errorMessage}" />
				</span>
			</td>
		</tr>
		<tr>
			<td>
				<fmt:message key="expressionExperimentLoad.platformOnly" />
			</td>
			<td align="center">
				<a class="helpLink" href="?"
					onclick="showHelpTip(event, 'Load an array design only, not  expression data.'); return false">
					<img src="/Gemma/images/help.png" />
				</a>
			</td>
			<td align="left">
				<input type="hidden" name="_<c:out value="${status.expression}"/>">
				<input type="checkbox" name="<c:out value="${status.expression}"/>"
					value="true" id="loadPlatformOnly"
					<c:if test="${status.value}">checked</c:if> />
				<span class="fieldError"> <c:out
						value="${status.errorMessage}" />
				</span>
			</td>
		</tr>
		<tr>
			<td>
				<fmt:message key="expressionExperimentLoad.suppressMatching" />
			</td>
			<td align="center">
				<a class="helpLink" href="?"
					onclick="showHelpTip(event, 'Check this box if you know tdat samples were run on only one platform each. Otderwise an attempt will be made to identify biological replicates on different platforms.'); return false">
					<img src="/Gemma/images/help.png" />
				</a>
			</td>
			<td align="left">
				<input type="hidden" name="_<c:out value="${status.expression}"/>">
				<input id="suppressMatching" type="checkbox"
					name="<c:out value="${status.expression}"/>" value="true"
					<c:if test="${status.value}">checked</c:if> />
				<span class="fieldError"> <c:out
						value="${status.errorMessage}" />
				</span>
			</td>
		</tr>
		<tr>
			<td>
				<fmt:message key="expressionExperimentLoad.splitByPlatform" />
			</td>
			<td align="center">
				<a class="helpLink" href="?"
					onclick="showHelpTip(event, 'For multi-platform studies, check this box if you want tde sample run on each platform to be considered separate experiments. If checked implies suppress matching'); return false">
					<img src="/Gemma/images/help.png" />
				</a>
			</td>
			<td align="left">
				<input type="hidden" name="_<c:out value="${status.expression}"/>">
				<input id="splitByPlatform" type="checkbox"
					name="<c:out value="${status.expression}"/>" value="true"
					<c:if test="${status.value}">checked</c:if> />
				<span class="fieldError"> <c:out
						value="${status.errorMessage}" />
				</span>
			</td>
		</tr>
		<tr>
			<td>
				<fmt:message key="expressionExperimentLoad.allowSuperSeries" />
			</td>
			<td align="center">
				<a class="helpLink" href="?"
					onclick="showHelpTip(event, 'If series is a superseries in GEO, allow it to load; leave this unchecked to prevent accidental superseries loading.'); return false">
					<img src="/Gemma/images/help.png" />
				</a>
			</td>
			<td align="left">
				<input type="hidden" name="_<c:out value="${status.expression}"/>">
				<input checked id="allowSuperSeriesLoad" type="checkbox"
					name="<c:out value="${status.expression}"/>" value="true"
					<c:if test="${status.value}">checked</c:if> />
				<span class="fieldError"> <c:out
						value="${status.errorMessage}" />
				</span>
			</td>
		</tr>
		<tr>
			<td>
				<fmt:message key="expressionExperimentLoad.allowArrayExpressDesign" />
			</td>
			<td align="center">
				<a class="helpLink" href="?"
					onclick="showHelpTip(event, 'When loading from ArrayExpress, allow tde array design to be imported. It must not be an array design already in Gemma!'); return false">
					<img src="/Gemma/images/help.png" />
				</a>
			</td>
			<td align="left">
				<input type="hidden" name="_<c:out value="${status.expression}"/>">
				<input checked id="allowArrayExpressDesign" type="checkbox"
					name="<c:out value="${status.expression}"/>" value="false"
					<c:if test="${status.value}">checked</c:if> />
				<span class="fieldError"> <c:out
						value="${status.errorMessage}" />
				</span>
			</td>
		</tr>
	
		<tr class="last-row">
			<td>
				<fmt:message key="expressionExperimentLoad.arrayDesign" />
			</td>
			<td></td>
			<td>
				<div id="arrayDesignCombo"></div>
			</td>
		</tr>
		<tr class="last-row">
			<td></td>
			<td></td>
			<td class="buttonBar">
				<div id="upload-button"></div>
			</td>
		</tr>
	</table>
</div>

<div id="progress-area" style="padding: 5px;"></div>

<validate:javascript formName="expressionExperimentLoad" staticJavascript="false" />
