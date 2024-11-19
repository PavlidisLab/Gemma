<%@ include file="/common/taglibs.jsp" %>

<jsp:useBean id="appConfig" scope="application" type="java.util.Map" />

<head>
<title>${subSet.name}</title>
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
            entity="${subSet.sourceExperiment}">${subSet.sourceExperiment.shortName}</Gemma:entityLink>,
        <a href="${pageContext.request.contextPath}/expressionExperiment/showAllExpressionExperimentSubSets.html?id=${subSet.sourceExperiment.id}">see
            all subsets</a>)
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
            <td>
                <c:choose>
                    <c:when test="${not empty subSet.description}">
                        ${fn:escapeXml(fn:trim(subSet.description))}
                    </c:when>
                    <c:when test="${not empty subSet.sourceExperiment.description}">
                        ${fn:escapeXml(fn:trim(subSet.sourceExperiment.description))}&nbsp;<b>(inherited)</b>
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
                    <c:when test="${subSet.accession != null}">
                        <Gemma:databaseEntry databaseEntry="${subSet.accession}" />
                    </c:when>
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
                                ${fn:escapeXml(pd.name)}
                            </c:when>
                            <c:otherwise>
                                <a href="${pageContext.request.contextPath}/expressionExperiment/ee/showAllExpressionExperimentSubSets.html?id=${subSet.id}&dimension=${pd.id}">${fn:escapeXml(pd.name)}</a>
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
    </table>
    <c:if test="${not empty bioAssays}">
        <hr class="normal">
        <h3>Assays</h3>
        <table class="mb-3">
            <thead>
            <tr>
                <th>Name</th>
                <th>Description</th>
            </tr>
            </thead>
            <tbody>
            <c:forEach items="${bioAssays}" var="ba">
                <tr>
                    <td>
                        <a href="${pageContext.request.contextPath}/bioAssay/showBioAssay.html?id=${ba.id}&dimension=${dimension.id}">${ba.name}</a>
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
    </c:if>
    <c:if test="${(not empty subSet.characteristics) or (not empty subSet.sourceExperiment.characteristics)}">
        <hr class="normal">
        <h3>Annotations</h3>
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
                    <td><Gemma:characteristic characteristic="${characteristic}" />&nbsp;<b>(inherited)</b></td>
                </tr>
            </c:forEach>
        </table>
    </c:if>
    <div id="app"></div>
</div>

<script>
new Gemma.EESubSet().mount( '#app', ${subSet.id} );
</script>