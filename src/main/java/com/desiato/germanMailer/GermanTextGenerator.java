package com.desiato.germanMailer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Component
public class GermanTextGenerator {

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.model:gemini-2.0-flash}")
    private String model;

    private final RestClient restClient = RestClient.create();
    private final ObjectMapper mapper = new ObjectMapper();

    private static final List<String> TOPICS = List.of(
            "das Wetter", "Essen und Trinken", "eine Reise", "Hobbys",
            "die Familie", "der Alltag", "Einkaufen im Supermarkt",
            "die Jahreszeiten", "ein Restaurantbesuch", "der Sport"
    );

    public String generate() {
        String topic = TOPICS.get((int) (Math.random() * TOPICS.size()));

        String prompt = """
                Schreibe einen kurzen Text auf Deutsch fuer Deutschlerner auf Niveau A1/A2 zum Thema "%s".

                Regeln:
                - Nur einfache Woerter und kurze Saetze.
                - 5 bis 8 Saetze lang.
                - Danach eine englische Uebersetzung.
                - Danach 5 wichtige Vokabeln mit englischer Uebersetzung.

                Formatiere mit den Ueberschriften: Text, Translation, Vocabulary.
                """.formatted(topic);

        Map<String, Object> requestBody = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(Map.of("text", prompt)))
                )
        );

        String url = "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s"
                .formatted(model, apiKey);

        String response = restClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .retrieve()
                .body(String.class);

        return extractText(response);
    }

    private String extractText(String rawJson) {
        try {
            JsonNode root = mapper.readTree(rawJson);
            return root.path("candidates").get(0)
                    .path("content").path("parts").get(0)
                    .path("text").asText();
        } catch (Exception e) {
            throw new RuntimeException("Could not parse Gemini response: " + rawJson, e);
        }
    }
}
