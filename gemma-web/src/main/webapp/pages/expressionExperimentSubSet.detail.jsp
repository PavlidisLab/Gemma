<%@ include file="/common/taglibs.jsp" %>
<jsp:useBean id="subSet" scope="request" type="ubic.gemma.model.expression.experiment.ExpressionExperimentSubSet" />

<head>
<title>${subSet.name}</title>
<meta name="description" content="${fn:escapeXml(subSet.description)}" />
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
            <td class="label">Description:</td>
            <td>
                <c:choose>
                    <c:when test="${not empty subSet.description}">
                        ${fn:escapeXml(subSet.description)}
                    </c:when>
                    <c:when test="${not empty subSet.sourceExperiment.description}">
                        ${fn:escapeXml(subSet.sourceExperiment.description)} <b>(inherited)</b>
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
                        <Gemma:databaseEntry databaseEntry="${subSet.sourceExperiment.accession}" /> <b>(inherited)</b>
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
                    <Gemma:citation citation="${subSet.sourceExperiment.primaryPublication}" /> <b>(inherited)</b>
                </c:when>
                <c:otherwise><i>No publication/i></c:otherwise>
                </c:choose>
            </td>
        </tr>
        <tr>
            <td class="label">Dimension</td>
            <td>${fn:escapeXml(dimension.name)}</td>
        </tr>
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
    </table>
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
                <td><Gemma:characteristic characteristic="${characteristic}" /> <b>(inherited)</b></td>
            </tr>
        </c:forEach>
    </table>
</div>
