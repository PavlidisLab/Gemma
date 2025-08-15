<%@ include file="/common/taglibs.jsp" %>

<%--@elvariable id="appConfig" type="java.util.Map"--%>

<head>
<title>${subSet.name} of ${subSet.sourceExperiment.shortName} - ${subSet.sourceExperiment.name}</title>
<c:choose>
    <c:when test="${not empty subSet.description}">
        <meta name="description" content="${fn:escapeXml(fn:trim(subSet.description))}" />
    </c:when>
    <c:when test="${not empty subSet.sourceExperiment.description}">
        <meta name="description" content="${fn:escapeXml(fn:trim(subSet.sourceExperiment.description))}" />
    </c:when>
</c:choose>
<meta name="keywords" content="${fn:escapeXml(keywords)}" />
</head>

<div class="padded">
    <h2>
        ${subSet.name}
        (subset of <Gemma:entityLink
            entity="${subSet.sourceExperiment}">${subSet.sourceExperiment.shortName}</Gemma:entityLink> - ${subSet.sourceExperiment.name},
        <a href="${pageContext.request.contextPath}/expressionExperiment/showAllExpressionExperimentSubSets.html?id=${subSet.sourceExperiment.id}&dimension=${dimension.id}">see all subsets</a>)
    </h2>
    <table>
        <tr>
            <td class="label">Annotations:</td>
            <td>
                <c:forEach items="${annotations}" var="annotation" varStatus="i">
                    <a href="${appConfig['gemma.gemBrow.url']}/#/q/${Gemma:urlEncode(annotation.termUri, "UTF-8")}">
                            ${annotation.termName}</a><c:if test="${!i.last}">, </c:if>
                </c:forEach>
            </td>
        </tr>
        <tr>
            <td class="label">Description:</td>
            <td style="max-width: 800px;">
                <c:choose>
                    <c:when test="${not empty subSet.description}">
                        <div style="white-space: pre-wrap;">${fn:escapeXml(fn:trim(subSet.description))}</div>
                    </c:when>
                    <c:when test="${not empty subSet.sourceExperiment.description}">
                        <div style="white-space: pre-wrap;">${fn:escapeXml(fn:trim(subSet.sourceExperiment.description))}&nbsp;<b>(inherited)</b>
                        </div>
                    </c:when>
                    <c:otherwise>
                        <i>No description available</i>
                    </c:otherwise>
                </c:choose>
            </td>
        </tr>
        <tr>
            <td class="label">Accession:</td>
            <td>
                <c:choose>
                    <c:when test="${subSet.sourceExperiment.accession != null}">
                        <Gemma:databaseEntry databaseEntry="${subSet.sourceExperiment.accession}" />&nbsp;<b>(inherited)</b>
                    </c:when>
                    <c:otherwise><i>No accession available</i></c:otherwise>
                </c:choose>
            </td>
        </tr>
        <tr>
            <td class="label">Publication:</td>
            <td>
                <c:choose>
                <c:when test="${subSet.primaryPublication != null}">
                    <Gemma:citation citation="${subSet.primaryPublication}" />
                </c:when>
                <c:when test="${subSet.sourceExperiment.primaryPublication != null}">
                    <Gemma:citation citation="${subSet.sourceExperiment.primaryPublication}" />&nbsp;<b>(inherited)</b>
                </c:when>
                <c:otherwise><i>No publication/i></c:otherwise>
                </c:choose>
            </td>
        </tr>
        <c:if test="${dimension != null}">
            <tr>
                <td class="label">Dimension:</td>
                <td>
                    <c:forEach items="${possibleDimensions}" var="pd">
                        <c:choose>
                            <c:when test="${pd == dimension}">
                                ${fn:escapeXml(pd.id)} (${bioAssays.size()}/${pd.bioAssays.size()} samples)
                            </c:when>
                            <c:otherwise>
                                <Gemma:entityLink entity="${subSet}"
                                        dimension="${pd}">${fn:escapeXml(pd.id)}</Gemma:entityLink>
                                (${bioAssays.size()}/${pd.bioAssays.size()} samples)
                            </c:otherwise>
                        </c:choose>
                    </c:forEach>
                </td>
            </tr>
        </c:if>
        <c:if test="${not empty otherSubSets}">
            <tr>
                <td class="label">Other subsets:</td>
                <td>
                    <ul>
                        <c:forEach items="${otherSubSets}" var="oss">
                            <li>
                                <a href="${pageContext.request.contextPath}/expressionExperiment/showExpressionExperimentSubSet.html?id=${oss.id}&dimension=${dimension.id}">${fn:escapeXml(oss.name)}</a>
                            </li>
                        </c:forEach>
                    </ul>
                </td>
            </tr>
        </c:if>
        <c:if test="${heatmap != null}">
        </c:if>
        <c:if test="${not empty bioAssays}">
            <td class="label">Assays:</td>
            <td>
                <c:choose>
                    <c:when test="${heatmap != null}">
                        <Gemma:expressionDataHeatmap heatmap="${heatmap}"
                                alt="Heatmap of the expression data of ${subSet.name}. The rows correspond to assays and columns to genes."
                                maxWidth="800" />
                    </c:when>
                    <c:otherwise>
                        <table class="mb-3">
                            <thead>
                            <tr>
                                <th>Name</th>
                                <th>Sample</th>
                                <th>Description</th>
                            </tr>
                            </thead>
                            <tbody>
                            <c:forEach items="${bioAssays}" var="ba">
                                <tr>
                                    <td>
                                        <Gemma:entityLink entity="${ba}"
                                                dimension="${dimension}">${fn:escapeXml(ba.name)}</Gemma:entityLink>
                                    </td>
                                    <td>
                                        <Gemma:entityLink entity="${ba.sampleUsed}"
                                                dimension="${dimension}">${fn:escapeXml(ba.sampleUsed.name)}</Gemma:entityLink>
                                    </td>
                                    <td>
                                        <c:choose>
                                            <c:when test="${not empty ba.description}">
                                                ${fn:escapeXml(ba.description)}
                                            </c:when>
                                            <c:otherwise><i>No description available</i></c:otherwise>
                                        </c:choose>
                                    </td>
                                </tr>
                            </c:forEach>
                            </tbody>
                        </table>
                    </c:otherwise>
                </c:choose>
            </td>
            </tr>
        </c:if>
        <c:if test="${(not empty subSet.characteristics) or (not empty subSet.sourceExperiment.characteristics)}">
            <tr>
                <td class="label">Annotations:</td>
                <td>
                    <table>
                        <thead>
                        <tr>
                            <th>Category</th>
                            <th>Value</th>
                        </tr>
                        </thead>
                        <c:forEach items="${subSet.characteristics}" var="characteristic">
                            <tr>
                                <td><Gemma:characteristic characteristic="${characteristic}" category="true" /></td>
                                <td><Gemma:characteristic characteristic="${characteristic}" /></td>
                            </tr>
                        </c:forEach>
                        <c:forEach items="${subSet.sourceExperiment.characteristics}" var="characteristic">
                            <tr>
                                <td><Gemma:characteristic characteristic="${characteristic}" category="true" /></td>
                                <td><Gemma:characteristic characteristic="${characteristic}" />&nbsp;<b>(inherited)</b>
                                </td>
                            </tr>
                        </c:forEach>
                    </table>
                </td>
            </tr>
        </c:if>
    </table>
</div>