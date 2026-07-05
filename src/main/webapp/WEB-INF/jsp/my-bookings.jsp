<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Мои брони</title>
    <script src="https://cdn.tailwindcss.com"></script>
</head>
<body class="bg-gray-50 min-h-screen flex flex-col">
    <header class="bg-white shadow-sm py-3 px-4 flex justify-between items-center sticky top-0 z-10">
        <h1 class="text-xl font-bold text-blue-600">Мои брони</h1>
        <div class="flex items-center gap-3">
            <span class="text-sm text-gray-600">
                ${sessionScope.client.name != null ? sessionScope.client.name : sessionScope.client.phone}
            </span>
            <form action="${pageContext.request.contextPath}/logout" method="post" class="inline">
                <button type="submit" class="text-sm text-gray-500 hover:text-gray-700">Выйти</button>
            </form>
        </div>
    </header>

    <main class="flex-1 container mx-auto px-4 py-6 max-w-3xl w-full">
        <c:choose>
            <c:when test="${not empty bookings}">
                <div class="space-y-3">
                    <c:forEach var="b" items="${bookings}">
                        <div class="bg-white rounded-lg shadow p-4 cursor-pointer hover:shadow-md transition"
                             onclick="location.href = '${pageContext.request.contextPath}/bookings/${b.id}'">
                            <div class="flex justify-between items-start">
                                <div>
                                    <span class="text-xs px-2 py-0.5 rounded
                                        <c:choose>
                                            <c:when test="${b.status == 'active'}">bg-green-100 text-green-800</c:when>
                                            <c:when test="${b.status == 'cancelled'}">bg-gray-200 text-gray-600</c:when>
                                            <c:when test="${b.status == 'late_cancel'}">bg-yellow-100 text-yellow-800</c:when>
                                            <c:when test="${b.status == 'center_cancelled'}">bg-red-100 text-red-800</c:when>
                                        </c:choose>">
                                        <c:choose>
                                            <c:when test="${b.status == 'active'}">Активна</c:when>
                                            <c:when test="${b.status == 'cancelled'}">Отменена</c:when>
                                            <c:when test="${b.status == 'late_cancel'}">Поздняя отмена</c:when>
                                            <c:when test="${b.status == 'center_cancelled'}">Отменён центром</c:when>
                                        </c:choose>
                                    </span>
                                    <p class="font-medium">${b.slot.startAtFormatted}</p>
                                    <p class="text-sm text-gray-600">${b.slot.trackConfig.name} · ${b.slot.marshal.name}</p>
                                    <p class="text-sm">${b.seatsCount} места
                                        <c:if test="${b.rentalGearCount > 0}">, прокат: ${b.rentalGearCount}</c:if>
                                    </p>
                                </div>
                                <div class="text-right">
                                    <p class="font-bold">${b.priceTotal} ₽</p>
                                </div>
                            </div>
                        </div>
                    </c:forEach>
                </div>
            </c:when>
            <c:otherwise>
                <div class="text-center py-10 text-gray-500">
                    <p>У вас пока нет записей</p>
                    <a href="${pageContext.request.contextPath}/slots" class="text-blue-600 hover:underline">Выбрать заезд</a>
                </div>
            </c:otherwise>
        </c:choose>
    </main>

    <!-- Нижняя навигация -->
    <div class="bg-white border-t border-gray-200 py-2 flex justify-around text-sm sticky bottom-0">
        <a href="${pageContext.request.contextPath}/slots" class="text-gray-500 hover:text-blue-600">Заезды</a>
        <a href="${pageContext.request.contextPath}/bookings" class="text-blue-600 font-medium">Мои брони</a>
    </div>
</body>
</html>