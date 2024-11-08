<%@ include file="/common/taglibs.jsp" %>

<head>
<title>
    <fmt:message key="expressionExperimentLoad.title" />
</title>
</head>

<div class="padded">
    <fmt:message key="expressionExperimentLoad.title" />
</div>

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
                <i class="qtp fa fa-question-circle fa-fw"
                        title='Check if data is to come from ArrayExpress.'>
                </i>
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
                <i class="qtp fa fa-question-circle fa-fw"
                        title='Load an array design only, not expression data.'>
                </i>
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
                <i class="qtp fa fa-question-circle fa-fw"
                        title='Check this box if you know tdat samples were run on only one platform each. Otderwise an attempt will be made to identify biological replicates on different platforms.'>
                </i>
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
                <i class="qtp fa fa-question-circle fa-fw"
                        title='For multi-platform studies, check this box if you want tde sample run on each platform to be considered separate experiments. If checked implies suppress matching'>
                </i>
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
                <i class="qtp fa fa-question-circle fa-fw"
                        title='If series is a superseries in GEO, allow it to load; leave this unchecked to prevent accidental superseries loading.'>
                </i>
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
                <i class="qtp fa fa-question-circle fa-fw"
                        title='When loading from ArrayExpress, allow tde array design to be imported. It must not be an array design already in Gemma!'>
                </i>
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

<jwr:script src='/scripts/app/loadExpressionExperiment.js' />
<script>
$( document ).ready( function() {
   $( 'i[title]' ).qtip();
} );
</script>

<validate:javascript formName="expressionExperimentLoad" staticJavascript="false" />
