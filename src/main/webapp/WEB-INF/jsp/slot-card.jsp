<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Карточка заезда</title>
    <script src="https://cdn.tailwindcss.com"></script>
</head>
<body class="bg-gray-50 min-h-screen flex flex-col">
    <header class="bg-white shadow-sm py-3 px-4 flex items-center">
        <a href="${pageContext.request.contextPath}/slots" class="text-blue-600 hover:underline mr-4">← Назад</a>
        <h1 class="text-xl font-bold text-blue-600">Карточка заезда</h1>
    </header>

    <main class="flex-1 container mx-auto px-4 py-6 max-w-3xl w-full">
        <c:choose>
            <c:when test="${not empty slot}">
                <div class="bg-white rounded-lg shadow p-6 space-y-4">
                    <h2 class="text-2xl font-bold">${slot.startAtFormatted}</h2>
                    <p class="text-lg">${slot.trackConfig.name}</p>
                    <p>Маршал: ${slot.marshal.name} ★ ${slot.marshal.ratingAvg}</p>
                    <c:if test="${not empty slot.requirementsText}">
                        <div class="bg-yellow-50 border-l-4 border-yellow-400 p-3 text-sm">
                            ⚠️ ${slot.requirementsText}
                        </div>
                    </c:if>
                    <p>Свободно мест: ${slot.freeKarts} из ${slot.totalKarts}</p>
                    <p>Прокат экипировки: ${slot.freeRentalGear} шт.</p>
                    <p class="text-xl font-bold">${slot.priceKart} ₽ за место</p>
                    <c:if test="${not empty slot.meetingPoint}">
                        <div class="text-sm text-gray-600">📍 ${slot.meetingPoint}</div>
                    </c:if>
                    <c:if test="${not empty slot.address}">
                        <div class="text-sm text-gray-600">Адрес: ${slot.address}</div>
                    </c:if>

                    <c:choose>
                        <c:when test="${slot.status == 'scheduled' && slot.freeKarts > 0}">
                            <a href="${pageContext.request.contextPath}/booking/${slot.id}"
                               class="block w-full bg-blue-600 text-white text-center py-2 rounded-lg hover:bg-blue-700">
                                Записаться
                            </a>
                        </c:when>
                        <c:otherwise>
                            <span class="block w-full bg-gray-300 text-gray-500 text-center py-2 rounded-lg cursor-not-allowed">
                                <c:choose>
                                    <c:when test="${slot.status == 'cancelled'}">Заезд отменён</c:when>
                                    <c:otherwise>Мест нет</c:otherwise>
                                </c:choose>
                            </span>
                        </c:otherwise>
                    </c:choose>
                </div>
            </c:when>
            <c:otherwise>
                <div class="text-center py-10 text-gray-500">
                    <p>Заезд не найден</p>
                    <a href="${pageContext.request.contextPath}/slots" class="text-blue-600 hover:underline">Вернуться к списку</a>
                </div>
            </c:otherwise>
        </c:choose>
    </main>
</body>
</html>