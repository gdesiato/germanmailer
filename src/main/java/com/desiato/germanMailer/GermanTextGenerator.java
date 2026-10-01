package com.desiato.germanMailer;

import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
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

    @Value("${gemini.models:gemini-3.8-flash,gemini-3.6-flash,gemini-3.5-flash-lite}")
    private List<String> models;

    private final RestClient restClient = RestClient.create();
    private final ObjectMapper mapper = new ObjectMapper();

    private static final int MAX_ATTEMPTS_PER_MODEL = 3;
    private static final long RETRY_DELAY_MS = 15_000;

    private static final List<String> TOPICS = List.of(
            "was du heute gemacht hast",
            "deine Pläne für das Wochenende",
            "etwas Lustiges, das heute passiert ist",
            "was du gern isst",
            "ein Ort, den du gern besuchst",
            "deine Arbeit und dein Alltag",
            "deine Hobbys",
            "was du gestern Abend gemacht hast",
            "eine kleine Reise, die du machen möchtest",
            "Sport und Freizeit",
            "das Wetter und deine Pläne für heute",
            "was du am Wochenende gemacht hast",
            "ein Restaurant oder Café, das du magst",
            "etwas Neues, das du gelernt hast",
            "deine Pläne für den nächsten Urlaub"
    );

    public String generate() {
        String topic = TOPICS.get((int) (Math.random() * TOPICS.size()));

        String prompt = """
                Du bist mein deutscher Brieffreund.

                Schreibe mir eine kurze, freundliche E-Mail auf Deutsch auf Niveau A1/A2.
                Schreibe so, als wärst du eine echte Person, die mir regelmäßig schreibt.

                Heute möchtest du über das Thema "%s" sprechen.

                Regeln:
                - Formatiere die E-Mail als Klartext mit echten Zeilenumbrüchen.
                - Beginne nach ungefähr 60 bis 70 Zeichen eine neue Zeile,
                  ohne Wörter zu trennen.
                - Teile den Text in kurze Absätze mit jeweils 2 bis 3 Sätzen.
                - Trenne die Absätze durch eine Leerzeile.
                - Schreibe die Anrede und den abschließenden Gruß jeweils
                  in eine eigene Zeile.
                - Verwende dieselbe Formatierung für die englische Übersetzung.
                - Schreibe jeden Vokabeleintrag in eine eigene Zeile.
            
                - Beginne mit "Hallo Giuseppe,".
                - Erzähle mir etwas über deinen Tag, dein Leben, deine Pläne,
                  deine Meinung oder eine kleine Erfahrung.
                - Schreibe natürlich und persönlich, nicht wie ein Lehrbuch.
                - Verwende einfache Wörter und kurze Sätze auf Niveau A1/A2.
                - Schreibe ungefähr 8 bis 12 Sätze.
                - Stelle mir am Ende 2 oder 3 einfache Fragen zum Thema,
                  damit ich dir antworten kann.
                - Verwende gelegentlich typische deutsche Ausdrücke,
                  aber keine schwierige Grammatik.
                - Beende die E-Mail mit einem freundlichen Gruß.
                - Gib danach eine englische Übersetzung.
                - Gib danach 5 wichtige deutsche Wörter oder Ausdrücke
                  aus der E-Mail mit englischer Übersetzung.

                Verwende diese Struktur:

                Brief

                [E-Mail auf Deutsch]

                Translation

                [Englische Übersetzung]

                Vocabulary

                [5 Wörter oder Ausdrücke mit englischer Übersetzung]
                """.formatted(topic);

        Map<String, Object> requestBody = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(Map.of("text", prompt)))
                )
        );

        RuntimeException lastError = null;

        for (String model : models) {
            for (int attempt = 1; attempt <= MAX_ATTEMPTS_PER_MODEL; attempt++) {
                try {
                    return callGemini(model, requestBody);

                } catch (HttpClientErrorException.NotFound e) {
                    // wrong or unavailable model name: retrying won't help, try the next model
                    lastError = e;
                    System.out.printf("Model %s not found. Skipping.%n", model);
                    break;

                } catch (HttpServerErrorException | HttpClientErrorException.TooManyRequests e) {
                    // 5xx (e.g. 503 overloaded) or 429 rate limit: retry, then fall back
                    lastError = e;
                    System.out.printf(
                            "Gemini model %s unavailable (%s). Attempt %d/%d.%n",
                            model, e, attempt, MAX_ATTEMPTS_PER_MODEL
                    );

                    if (attempt < MAX_ATTEMPTS_PER_MODEL) {
                        sleep(RETRY_DELAY_MS * attempt);   // 15s, 30s
                    }
                }
            }
            System.out.printf("Giving up on model %s, trying next model if any.%n", model);
        }

        throw new IllegalStateException("All Gemini models failed: " + models, lastError);
    }

    private String callGemini(String model, Map<String, Object> requestBody) {
        String url = "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s"
                .formatted(model, apiKey);

        String response = restClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .retrieve()
                .body(String.class);

        System.out.printf("Generated lesson with model %s%n", model);
        return extractText(response);
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting to retry Gemini", e);
        }
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
