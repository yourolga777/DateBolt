package com.javarush.telegram;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

public class ChatGPTService {
    private final String apiKey;
    private final HttpClient httpClient;
    private List<Message> messageHistory = new ArrayList<>();

    // Внутренний класс для хранения сообщений
    private static class Message {
        String role;
        String content;

        Message(String role, String content) {
            this.role = role;
            this.content = content;
        }
    }

    public ChatGPTService(String token) {
        this.apiKey = token;
        this.httpClient = HttpClient.newHttpClient();
    }

    /**
     * Одиночный запрос к DeepSeek по формату "запрос" -> "ответ".
     * Запрос состоит из двух частей:
     *      prompt - контекст вопроса (системное сообщение)
     *      question - сам запрос пользователя
     */
    public String sendMessage(String prompt, String question) {
        // Очищаем историю и добавляем системный промпт и вопрос пользователя
        messageHistory.clear();
        messageHistory.add(new Message("system", prompt));
        messageHistory.add(new Message("user", question));

        return sendToDeepSeek();
    }

    /**
     * Запросы к DeepSeek с сохранением истории сообщений.
     * Метод setPrompt() задаёт контекст запроса (системный промпт)
     */
    public void setPrompt(String prompt) {
        messageHistory.clear();
        messageHistory.add(new Message("system", prompt));
    }

    /**
     * Устанавливает системный промпт (альтернативный метод)
     */
    public void setSystemPrompt(String systemPrompt) {
        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            messageHistory.clear();
            messageHistory.add(new Message("system", systemPrompt));
        }
    }

    /**
     * Запросы к DeepSeek с сохранением истории сообщений.
     * Метод addMessage() добавляет новый вопрос (сообщение) в чат.
     */
    public String addMessage(String question) {
        messageHistory.add(new Message("user", question));
        return sendToDeepSeek();
    }

    /**
     * Отправляем DeepSeek серию сообщений
     */
    private String sendToDeepSeek() {
        try {
            String jsonBody = buildJsonRequest();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.deepseek.com/v1/chat/completions"))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            String answer = parseResponse(response.body());

            // Сохраняем ответ в историю
            if (answer != null && !answer.isEmpty()) {
                messageHistory.add(new Message("assistant", answer));
            }

            return answer != null ? answer : "Ошибка: не удалось получить ответ";

        } catch (Exception e) {
            e.printStackTrace();
            return "Ошибка: " + e.getMessage();
        }
    }

    /**
     * Формирует JSON-запрос для DeepSeek API
     */
    private String buildJsonRequest() {
        StringBuilder messagesJson = new StringBuilder();
        messagesJson.append("[");
        for (int i = 0; i < messageHistory.size(); i++) {
            Message msg = messageHistory.get(i);
            messagesJson.append("{\"role\":\"").append(escapeJson(msg.role))
                    .append("\",\"content\":\"").append(escapeJson(msg.content))
                    .append("\"}");
            if (i < messageHistory.size() - 1) {
                messagesJson.append(",");
            }
        }
        messagesJson.append("]");

        return String.format("""
                {
                    "model": "deepseek-chat",
                    "messages": %s,
                    "stream": false,
                    "max_tokens": 3000,
                    "temperature": 0.9
                }
                """, messagesJson.toString());
    }

    /**
     * Парсит ответ DeepSeek API
     */
    private String parseResponse(String response) {
        // Ищем поле "content"
        int start = response.indexOf("\"content\":\"");
        if (start == -1) {
            // Пробуем другой формат
            start = response.indexOf("\"content\": \"");
            if (start == -1) {
                return null;
            }
            start += 12;
        } else {
            start += 11;
        }

        int end = response.indexOf("\"", start);
        if (end == -1) {
            return null;
        }

        return response.substring(start, end)
                .replace("\\n", "\n")
                .replace("\\\"", "\"")
                .replace("\\\\", "\\");
    }

    /**
     * Экранирует специальные символы для JSON
     */
    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}