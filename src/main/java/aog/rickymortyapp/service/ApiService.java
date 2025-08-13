package aog.rickymortyapp.service;

import aog.rickymortyapp.model.Charac;
import aog.rickymortyapp.model.Episode;
import aog.rickymortyapp.model.PaginatedResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class ApiService {
    private static final String BASE_URL = "https://rickandmortyapi.com/api";
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final Executor executor;

    public ApiService() {
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
        this.objectMapper = new ObjectMapper();
        this.executor = Executors.newFixedThreadPool(10);
    }

    public CompletableFuture<PaginatedResponse<Episode>> getEpisodes(int page) {
        HttpRequest request = HttpRequest.newBuilder()
            .GET()
            .uri(URI.create(BASE_URL + "/episode?page=" + page))
            .timeout(Duration.ofSeconds(30))
            .header("Accept", "application/json")
            .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
            .thenApplyAsync(response -> {
                
                if (response.statusCode() != 200) {
                    throw new RuntimeException("HTTP Error: " + 
                            response.statusCode() + 
                        " - Response: " + response.body());
                }
                
                try {
                    return objectMapper.readValue(
                            response.body(), 
                        new TypeReference<PaginatedResponse<Episode>>() {});
                } catch (JsonProcessingException ex) {
                    throw new RuntimeException(
                            "Error parsing episodes from page " + page, 
                            ex);
                }
            }, executor);
    }

    public CompletableFuture<List<Episode>> getAllEpisodes() {
        return getEpisodes(1)
            .thenComposeAsync(firstPage -> {
                List<Episode> allEpisodes = new ArrayList<>(
                        List.of(firstPage.results()));
                List<CompletableFuture<PaginatedResponse<Episode>>> futures = 
                        new ArrayList<>();
                
                for (int i = 2; i <= firstPage.info().pages(); i++) {
                    futures.add(getEpisodes(i));
                }
                return CompletableFuture.allOf(
                        futures.toArray(new CompletableFuture[0]))
                    .thenApply(v -> {
                        for (CompletableFuture<PaginatedResponse<Episode>> 
                                future : futures) {
                            
                            try {
                                Episode[] episodes = future.join().results();
                                allEpisodes.addAll(List.of(episodes));
                            } catch (Exception ex) {
                                System.err.println("Error processing episode page: "
                                        + ex.getMessage());
                            }
                        }
                        return allEpisodes;
                    });
            }, executor);
    }

    public CompletableFuture<Charac> getCharacter(String url) {
        return getCharacterWithRetry(url, 3, 1000);
    }

    private CompletableFuture<Charac> getCharacterWithRetry(String url, 
            int maxRetries, long delayMs) {
        if (url == null || url.trim().isEmpty()) {
            return CompletableFuture.failedFuture(
                new IllegalArgumentException("URL cannot be null or empty"));
        }

        HttpRequest request = HttpRequest.newBuilder()
            .GET()
            .uri(URI.create(url.trim()))
            .timeout(Duration.ofSeconds(30))
            .header("Accept", "application/json")
            .build();

        return httpClient.sendAsync(request, 
                HttpResponse.BodyHandlers.ofString())
            .thenApplyAsync(response -> {
                
                if (response.statusCode() == 429) {
                    throw new RuntimeException(
                            "Rate limit exceeded (429) for URL: " + url);
                }
                if (response.statusCode() != 200) {
                    System.err.println("HTTP Error " + response.statusCode() + 
                        " for character URL: " + url);
                    return new Charac(0, "Error loading character (HTTP " + 
                            response.statusCode() + ")", 
                        "unknown", "unknown", "unknown", "", new String[0]);
                }
                
                try {
                    return objectMapper.readValue(response.body(), Charac.class);
                } catch (JsonProcessingException ex) {
                    System.err.println("Error parsing character from URL " + url + 
                        ": " + ex.getMessage());
                    return new Charac(0, "Error parsing character", 
                        "unknown", "unknown", "unknown", "", new String[0]);
                }
            }, executor)
            .exceptionally(throwable -> {
                
                if (throwable.getMessage().contains("Rate limit") && 
                        maxRetries > 0) {
                    try {
                        Thread.sleep(delayMs);
                        System.out.println("Reintentando petición para: " + url + 
                            " (intentos restantes: " + maxRetries + ")");
                        return getCharacterWithRetry(url, maxRetries - 1, 
                                delayMs * 2).join();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return new Charac(0, "Request interrupted", 
                            "unknown", "unknown", "unknown", "", new String[0]);
                    }
                } 
                System.err.println("Error final para " + url + ": " + 
                        throwable.getMessage());
                return new Charac(0, "Error loading character", 
                    "unknown", "unknown", "unknown", "", new String[0]);
            });
    }

    public CompletableFuture<List<Charac>> getMultipleCharacters(
            List<String> urls) {
        
        if (urls == null || urls.isEmpty()) {
            return CompletableFuture.completedFuture(new ArrayList<>());
        }
        List<String> validUrls = new ArrayList<>();
        
        for (String url : urls) {
            
            if (url != null && !url.trim().isEmpty()) {
                validUrls.add(url.trim());
            }
        }

        if (validUrls.isEmpty()) {
            return CompletableFuture.completedFuture(new ArrayList<>());
        }
        return processCharactersInBatches(validUrls, 3); 
    }

    private CompletableFuture<List<Charac>> 
        processCharactersInBatches(List<String> urls, int batchSize) {
        List<Charac> allCharacters = new ArrayList<>();
        
        return CompletableFuture.supplyAsync(() -> {
            
            for (int i = 0; i < urls.size(); i += batchSize) {
                int endIndex = Math.min(i + batchSize, urls.size());
                List<String> batch = urls.subList(i, endIndex);
                List<CompletableFuture<Charac>> batchFutures = new ArrayList<>();
               
                for (String url : batch) {
                    batchFutures.add(getCharacter(url));
                }

                try {
                    CompletableFuture.allOf(batchFutures.toArray
        (new CompletableFuture[0])).join();
                    
                    for (CompletableFuture<Charac> future : batchFutures) {
                        try {
                            Charac character = future.join();
                            if (character != null && character.id() > 0) {
                                allCharacters.add(character);
                            }
                        } catch (Exception ex) {
                            System.err.println("Error getting character in batch: " 
                                    + ex.getMessage());
                        }
                    }
                    if (endIndex < urls.size()) {
                        Thread.sleep(500); 
                    }
                    
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    System.err.println("Error processing batch: " + e.getMessage());
                }
            }
            return allCharacters;
        }, executor);
    }
}