import java.net.URI; // Импортируем класс для работы с адресами URI
import java.net.http.HttpClient; // Импортируем встроенный HTTP-клиент (доступен с Java 11)
import java.net.http.HttpRequest; // Импортируем класс для создания структуры HTTP-запроса
import java.net.http.HttpResponse; // Импортируем класс для обработки ответов от сервера
import java.util.Locale; // Импортируем Locale для принудительного использования точек в числах с плавающей точкой

public class WeatherService {

    public static void main(String[] args) {
        // Объявляем константу с базовым URL-адресом
        final String API_URL = "https://api.weather.yandex.ru/v2/forecast";

        // Объявляем константу с API_KEY
        final String API_KEY = "902c3cf4-e0b4-4cc6-b19f-b5920b0be2d9";

        // Задаем координаты точки
        double lat = 53.20;
        double lon = 50.15;

        // Лимит прогноза погоды в сутках для передачи в API
        int limitDays = 5;

        try {
            // Принудительно используем Locale.US, чтобы координаты разделялись точкой (55.75)
            String fullUrl = String.format(Locale.US, "%s?lat=%.2f&lon=%.2f&limit=%d", API_URL, lat, lon, limitDays);

            // Создаем HTTP-клиент с автоматическим переходом по редиректам
            HttpClient client = HttpClient.newBuilder()
                    .followRedirects(HttpClient.Redirect.ALWAYS)
                    .build();

            // Строим HTTP-запрос к серверу
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(fullUrl)) // Передаем сформированный URI-адрес
                    .GET() // Указываем метод запроса GET
                    .header("X-Yandex-Weather-Key", API_KEY) // Добавляем заголовок авторизации
                    .build(); // Финализируем сборку объекта запроса

            // Отправляем запрос и получаем ответ в виде обычной строки (String)
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            // Получаем текстовое тело ответа
            String jsonResponse = response.body();

            // Вывод полного JSON-ответа
            System.out.println("=== JSON-ОТВЕТ ===");
            System.out.println(jsonResponse); // Печатаем весь ответ в консоль
            System.out.println("=========================\n");

            // Проверяем статус-код (200 OK) и наличие JSON-структуры (ответ должен начинаться не с HTML тегов вроде <body>)
            if (response.statusCode() == 200 && !jsonResponse.trim().startsWith("<")) {

                //  Извлечение текущей температуры
                int factIndex = jsonResponse.indexOf("\"fact\"");
                int tempIndex = (factIndex != -1) ? jsonResponse.indexOf("\"temp\"", factIndex) : -1;

                if (tempIndex != -1) {
                    int colonIndex = jsonResponse.indexOf(":", tempIndex);
                    int commaIndex = jsonResponse.indexOf(",", colonIndex);

                    String tempValueStr = jsonResponse.substring(colonIndex + 1, commaIndex).trim();
                    int currentTemp = Integer.parseInt(tempValueStr);
                    System.out.println("Текущая температура в г. Самара: " + currentTemp + "°C");
                } else {
                    System.out.println("Ошибка: Поле 'fact.temp' не найдено в ответе сервера.");
                }

                //  Расчет средней температуры
                double tempSum = 0;
                int count = 0;

                String[] parts = jsonResponse.split("\"temp_avg\"");

                for (int i = 1; i < parts.length && count < limitDays; i++) {
                    String part = parts[i];
                    int currentColon = part.indexOf(":");
                    int currentComma = part.indexOf(",", currentColon);

                    if (currentColon != -1 && currentComma != -1) {
                        String avgStr = part.substring(currentColon + 1, currentComma).trim();
                        double tempAvg = Double.parseDouble(avgStr);
                        tempSum += tempAvg;
                        count++;
                    }
                }

                if (count > 0) {
                    double averageForecastTemp = tempSum / count;
                    System.out.printf("Средняя температура по прогнозу (на %d сут.): %.2f°C\n", count, averageForecastTemp);
                } else {
                    System.out.println("Не удалось найти данные по прогнозам temp_avg.");
                }

            } else {
                // Если Яндекс заблокировал ключ
                System.out.println("Запрос не вернул JSON погоды (Код: " + response.statusCode() + ").");
                System.out.println("API_KEY заблокирован или перегружен запросами.");

            }

        } catch (Exception e) {
            System.out.println("Произошла системная ошибка: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
