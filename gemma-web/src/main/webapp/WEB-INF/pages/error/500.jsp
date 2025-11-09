<%@ include file="/WEB-INF/common/taglibs.jsp" %>

<title><fmt:message key="errorPage.title"/></title>

<style>
    ul {
        padding-left: 20px;
    }

    ul li {
        list-style-type: circle;
    }
</style>

<div class="padded">
    <h2><fmt:message key="errorPage.title"/></h2>
    <p><fmt:message key="errorPage.heading"/></p>

    <hr class="normal">

    <p>Possible next steps:</p>

    <ul>
        <li>Go back and try what you were doing again</li>
        <li>Do you need to log in?</li>
        <c:choose>
            <c:when test="${not empty requestScope['exception']}">
                <li>
                    <a href="mailto:pavlab-support@msl.ubc.ca?subject=${fn:escapeXml(requestScope['exception'].message)}">Email
                        us</a>about the problem.
                </li>
            </c:when>
            <c:when test="${not empty requestScope['javax.servlet.error.exception']}">
                <li>
                    <a href="mailto:pavlab-support@msl.ubc.ca?subject=${fn:escapeXml(requestScope['javax.servlet.error.exception'].message)}">Email
                        us</a> about the problem.
                </li>
            </c:when>
            <c:when test="${not empty requestScope['javax.servlet.error.message']}">
                <li>
                    <a href="mailto:pavlab-support@msl.ubc.ca?subject=${fn:escapeXml(requestScope['javax.servlet.error.message'])}">Email
                        us</a> about the problem.
                </li>
            </c:when>
            <c:otherwise>
                <li><a href="mailto:pavlab-support@msl.ubc.ca?subject=Unknown exception">Email us</a> about the
                    problem.
                </li>
            </c:otherwise>
        </c:choose>
    </ul>

    <%@ include file="/WEB-INF/common/exception.jsp" %>
</div>
