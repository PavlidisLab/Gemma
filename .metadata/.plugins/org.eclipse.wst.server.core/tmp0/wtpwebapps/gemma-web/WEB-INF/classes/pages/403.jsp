<%@ include file="/common/taglibs.jsp"%>
<%-- $Id$ --%>
<page:applyDecorator name="default">

<title>
    <fmt:message key="403.title" />
</title>
<content tag="heading">
<fmt:message key="403.title" />
<input type="hidden" id="reloadOnLogin" value="true"/>
</content>

<p>
    <fmt:message key="403.message">
        <fmt:param>
            <c:url value="/home.html" />
        </fmt:param>
    </fmt:message>
</p>
 </page:applyDecorator>