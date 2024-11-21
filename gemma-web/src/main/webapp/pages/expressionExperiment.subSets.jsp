<%@ include file="/common/taglibs.jsp" %>

<head>
<title>Subsets of ${expressionExperiment.shortName} - ${expressionExperiment.name}</title>
<meta name="description" content="${fn:escapeXml(fn:trim(expressionExperiment.description))}" />
<meta name="keywords" content="${fn:escapeXml(keywords)}" />
</head>

<div class="padded">
    <h2>
        Subsets of <Gemma:entityLink
            entity="${expressionExperiment}">${expressionExperiment.shortName}</Gemma:entityLink>
        - ${expressionExperiment.name}
    </h2>
    <table>
        <tr>
            <td class="label">Description:</td>
            <td>
                <div style="white-space: pre-wrap;">${fn:escapeXml(fn:trim(expressionExperiment.description))}</div>
            </td>
        </tr>
        <tr>
            <td class="label">Accession:</td>
            <td><Gemma:databaseEntry databaseEntry="${expressionExperiment.accession}" /></td>
        </tr>
        <tr>
            <td class="label">Publication:</td>
            <td><Gemma:citation citation="${expressionExperiment.primaryPublication}" /></td>
        </tr>
        <c:forEach items="${subSetsByDimension.entrySet()}" var="e" varStatus="eI">
            <c:if test="${not eI.first}">
                <tr>
                    <td colspan="2">
                        <hr class="normal">
                    </td>
                </tr>
            </c:if>
            <tr>
                <td class="label">Dimension:</td>
                <td>${fn:escapeXml(e.key.name)} (${e.key.bioAssays.size()} samples)</td>
            </tr>
            <c:if test="${quantitationTypesByDimension[e.key].size() > 0}">
                <tr>
                    <td class="label">Quantitation types:</td>
                    <td>
                        <ul>
                            <c:forEach items="${quantitationTypesByDimension[e.key]}" var="qt">
                                <li>
                                    <c:choose>
                                        <c:when test="${vectorTypes[qt] == null}">
                                            ${fn:escapeXml(qt.name)} <i>(this quantitation does not have any
                                            associated
                                            vectors)</i>
                                        </c:when>
                                        <c:when test="${vectorTypes[qt].name == 'ubic.gemma.model.expression.bioAssayData.RawExpressionDataVector'}">
                                            <a href="${pageContext.request.contextPath}/rest/v2/datasets/${expressionExperiment.id}/data/raw?quantitationType=${qt.id}">${fn:escapeXml(qt.name)}</a>
                                        </c:when>
                                        <c:when test="${vectorTypes[qt].name == 'ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVector'}">
                                            <a href="${pageContext.request.contextPath}/rest/v2/datasets/${expressionExperiment.id}/data/processed?quantitationType=${qt.id}">${fn:escapeXml(qt.name)}</a>
                                        </c:when>
                                        <c:otherwise>
                                            ${fn:escapeXml(qt.name)}
                                        </c:otherwise>
                                    </c:choose>
                                </li>
                            </c:forEach>
                        </ul>
                    </td>
                </tr>
            </c:if>
            <tr>
                <td class="label">Subsets:</td>
                <td>
                    <ul>
                        <c:forEach items="${e.value}" var="subSet" varStatus="i">
                            <li>
                                <Gemma:entityLink entity="${subSet}" dimension="${e.key}">${fn:escapeXml(subSet.name)}</Gemma:entityLink>
                            </li>
                        </c:forEach>
                    </ul>
                </td>
            </tr>
        </c:forEach>
    </table>
</div>