<%@ include file="/common/taglibs.jsp"%>

<display:table name="designElements" class="list" requestURI="" id="designElementList" export="true">
    <display:column property="name" sort="true" titleKey="arrayDesign.name" />
    <display:column property="description" sort="true" titleKey="arrayDesign.description" />
    <display:setProperty name="basic.empty.showtable" value="true" />
</display:table>


