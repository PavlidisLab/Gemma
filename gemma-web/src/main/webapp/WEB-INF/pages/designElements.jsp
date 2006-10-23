<%@ include file="/common/taglibs.jsp"%>

<display:table name="designElements" class="list" requestURI="" id="designElementList" >
    <display:column property="name" sortable="true" titleKey="arrayDesign.name" />
    <display:column property="description" sortable="true" titleKey="arrayDesign.description" />
    <display:setProperty name="basic.empty.showtable" value="true" />
</display:table>


