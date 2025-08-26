<%@ include file="/common/taglibs.jsp" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<head>
<title>All Bibliographic References for Experiments</title>
</head>

<div class="padded">
    <h3>List of all <fmt:formatNumber value="${totalCitations}" /> published papers with data in Gemma:</h3>
    <p>
        To search for a paper or experiment and see more details, visit the
        <a href="${pageContext.request.contextPath}/bibRef/searchBibRefs.html">annotated paper search page</a>.
    </p>
    <p class="flex justify-space-around">
        <c:if test="${firstPageOffset != null}">
            <a href="${pageContext.request.contextPath}/bibRef/showAllEeBibRefs.html?offset=${firstPageOffset}">First</a>
        </c:if>
        <c:if test="${previousPageOffset != null}">
            <a href="${pageContext.request.contextPath}/bibRef/showAllEeBibRefs.html?offset=${previousPageOffset}">Previous</a>
        </c:if>
        <c:if test="${nextPageOffset != null}">
            <a href="${pageContext.request.contextPath}/bibRef/showAllEeBibRefs.html?offset=${nextPageOffset}">Next</a>
        </c:if>
        <c:if test="${lastPageOffset != null}">
            <a href="${pageContext.request.contextPath}/bibRef/showAllEeBibRefs.html?offset=${lastPageOffset}">Last</a>
        </c:if>
    </p>
    <table>
        <c:forEach items="${citationToEEs}" var="citationToEE">
            <tr>
                <td>
                    <c:out value="${citationToEE.key.citation}"/>
                </td>
                <td style="padding-right: 10px">
                    <a target="_blank" href="${citationToEE.key.pubmedURL}" rel="noopener noreferrer">
                        <Gemma:img src="/images/pubmed.gif" alt="PubMed link" />
                    </a>
                </td>
                <td>
                    <c:forEach items="${citationToEE.value}" var="ee">
                        <a href="${pageContext.request.contextPath}/expressionExperiment/showExpressionExperiment.html?id=${ee.id}">
                            <c:out value="${ee.shortName}"/>
                        </a>&nbsp;&nbsp;
                    </c:forEach>
                </td>
            </tr>
        </c:forEach>
    </table>
    <p class="flex justify-space-around">
        <c:if test="${firstPageOffset != null}">
            <a href="${pageContext.request.contextPath}/bibRef/showAllEeBibRefs.html?offset=${firstPageOffset}">First</a>
        </c:if>
        <c:if test="${previousPageOffset != null}">
            <a href="${pageContext.request.contextPath}/bibRef/showAllEeBibRefs.html?offset=${previousPageOffset}">Previous</a>
        </c:if>
        <c:if test="${nextPageOffset != null}">
            <a href="${pageContext.request.contextPath}/bibRef/showAllEeBibRefs.html?offset=${nextPageOffset}">Next</a>
        </c:if>
        <c:if test="${lastPageOffset != null}">
            <a href="${pageContext.request.contextPath}/bibRef/showAllEeBibRefs.html?offset=${lastPageOffset}">Last</a>
        </c:if>
    </p>
</div>
