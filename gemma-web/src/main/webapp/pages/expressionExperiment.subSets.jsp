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
    <security:authorize access="hasAuthority('GROUP_ADMIN')">
        <c:if test="${dimension != null}">
            <p>
                Only displaying subsets from dimension #${fn:escapeXml(dimension.id)}.
                <a href="${pageContext.request.contextPath}/expressionExperiment/showAllExpressionExperimentSubSets.html?id=${expressionExperiment.id}">
                    Show all dimensions
                </a>
            </p>
        </c:if>
    </security:authorize>
    <table>
        <tr>
            <td class="label">Description:</td>
            <td style="max-width: 800px;">
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
                <td>
                    <security:authorize access="hasAuthority('GROUP_ADMIN')">
                        <c:choose>
                            <c:when test="${dimension == null}">
                                <a href="${pageContext.request.contextPath}/expressionExperiment/showAllExpressionExperimentSubSets.html?id=${expressionExperiment.id}&dimension=${e.key.id}">
                                        ${fn:escapeXml(e.key.name)} (${e.key.bioAssays.size()} samples)
                                </a>
                            </c:when>
                            <c:otherwise>
                                ${fn:escapeXml(e.key.name)} (${e.key.bioAssays.size()} samples)
                            </c:otherwise>
                        </c:choose>
                    </security:authorize>
                    <!-- regular users can only see the preferred BAD -->
                    <security:authorize access="not hasAuthority('GROUP_ADMIN')">
                        ${fn:escapeXml(e.key.name)} (${e.key.bioAssays.size()} samples)
                    </security:authorize>
                </td>
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
                    <table>
                        <c:if test="${not empty subSetFactorsByDimension[e.key]}">
                            <tr>
                                <th>Name</th>
                                <c:forEach
                                        items="${subSetFactorsByDimension[e.key].values().iterator().next().keySet()}"
                                        var="factor">
                                    <th style="text-transform: capitalize;">${fn:escapeXml(factor.name)}</th>
                                </c:forEach>
                            </tr>
                        </c:if>
                        <c:forEach items="${e.value}" var="subSet" varStatus="i">
                            <tr>
                                <td>
                                    <Gemma:entityLink entity="${subSet}"
                                            dimension="${e.key}">${fn:escapeXml(subSet.name)}</Gemma:entityLink>
                                </td>
                                <c:if test="${not empty subSetFactorsByDimension[e.key][subSet]}">
                                    <c:forEach items="${subSetFactorsByDimension[e.key][subSet].entrySet()}"
                                            var="factor">
                                        <td><Gemma:factorValue factorValue="${factor.value}" /></td>
                                    </c:forEach>
                                </c:if>
                            </tr>
                        </c:forEach>
                    </table>
                    <c:if test="${empty subSetFactorsByDimension[e.key]}">
                        <strong>Note:</strong> These subsets are not associated to any experimental factor.
                    </c:if>
                </td>
            </tr>
            <c:if test="${heatmapsByDimension[e.key] != null}">
                <tr>
                    <td class="label">Assays:</td>
                    <td><Gemma:expressionDataHeatmap heatmap="${heatmapsByDimension[e.key]}"
                            alt="Heatmap of the expression data of ${expressionExperiment.name}. The rows correspond to genes and columns to assays."
                            maxWidth="800"
                            maxHeight="600" /></td>
                </tr>
            </c:if>
        </c:forEach>
    </table>
</div>