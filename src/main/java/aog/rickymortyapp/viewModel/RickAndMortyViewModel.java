package aog.rickymortyapp.viewModel;

import aog.rickymortyapp.model.Charac;
import aog.rickymortyapp.model.Episode;
import aog.rickymortyapp.service.ApiService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class RickAndMortyViewModel {
    private final ApiService apiService;
    private final ConcurrentHashMap<String, List<Episode>> seasonEpisodes;
    private final ConcurrentHashMap<Integer, List<Charac>> episodeCharacters;
    private final Semaphore loadingSemaphore;
    
    private Consumer<String> onStatusUpdate;
    private Consumer<Map<String, List<Episode>>> onEpisodesLoaded;
    private Consumer<Boolean> onLoadingComplete;

    public RickAndMortyViewModel() {
        this.apiService = new ApiService();
        this.seasonEpisodes = new ConcurrentHashMap<>();
        this.episodeCharacters = new ConcurrentHashMap<>();
        this.loadingSemaphore = new Semaphore(2);
    }

    public void setOnStatusUpdate(Consumer<String> onStatusUpdate) {
        this.onStatusUpdate = onStatusUpdate;
    }

    public void setOnEpisodesLoaded(Consumer<Map<String, List<Episode>>> 
            onEpisodesLoaded) {
        this.onEpisodesLoaded = onEpisodesLoaded;
    }
    
    public void setOnLoadingComplete(Consumer<Boolean> onLoadingComplete) {
        this.onLoadingComplete = onLoadingComplete;
    }

    public void loadData() {
        updateStatus("Cargando episodios...");
        
        apiService.getAllEpisodes()
            .thenAccept(episodes -> {
                updateStatus("Episodios obtenidos: " + episodes.size() + 
                        ". Organizando por temporadas...");

                Map<String, List<Episode>> seasons = new HashMap<>();
                
                for (Episode episode : episodes) {
                    String season = extractSeasonFromEpisode(episode.episode());
                    seasonEpisodes.computeIfAbsent(season, k -> new ArrayList<>())
                            .add(episode);
                }
                if (onEpisodesLoaded != null) {
                    onEpisodesLoaded.accept(seasonEpisodes);
                }
                updateStatus("Episodios cargados. Iniciando carga de personajes...");
                loadCharactersWithThreads(episodes);
            })
            .exceptionally(ex -> {
                updateStatus("Error cargando episodios: " + ex.getMessage());
                if (onLoadingComplete != null) {
                    onLoadingComplete.accept(false);
                }
                return null;
            });
    }

    private void loadCharactersWithThreads(List<Episode> episodes) {
        List<Thread> threads = new ArrayList<>();
        AtomicInteger processedCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);
        int totalEpisodes = episodes.size();
        
        for (Episode episode : episodes) {
            Thread t = new Thread(() -> {
                try {
                    loadingSemaphore.acquire();
                    
                    int current = processedCount.incrementAndGet();
                    updateStatus("Cargando personajes para episodio: " + 
                            episode.name() + " (" + current + "/" + totalEpisodes + 
                            ")");

                    if (episode.characters() == null || episode.characters()
                            .length == 0) {
                        updateStatus("Episodio " + episode.name() + 
                                " no tiene personajes");
                        episodeCharacters.put(episode.id(), new ArrayList<>());
                        return;
                    }
                    List<String> validUrls = new ArrayList<>();
                    
                    for (String url : episode.characters()) {
                        
                        if (url != null && !url.trim().isEmpty()) {
                            validUrls.add(url.trim());
                        }
                    }
                    
                    if (validUrls.isEmpty()) {
                        updateStatus("No hay URLs válidas para " + episode.name());
                        episodeCharacters.put(episode.id(), new ArrayList<>());
                        return;
                    }
                    apiService.getMultipleCharacters(validUrls)
                        .thenAccept(characters -> {
                            episodeCharacters.put(episode.id(), characters);
                            updateStatus("Personajes cargados para episodio: " + 
                                    episode.name() + " (" + characters.size() + 
                                    " personajes)");
                        })
                        .exceptionally(ex -> {
                            errorCount.incrementAndGet();
                            updateStatus("Error cargando personajes para " + 
                                    episode.name() + ": " + ex.getMessage());
                            episodeCharacters.put(episode.id(), new ArrayList<>());
                            return null;
                        })
                        .join();
                    
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException delayEx) {
                        Thread.currentThread().interrupt();
                    }
                        
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    errorCount.incrementAndGet();
                    updateStatus("Thread interrumpido para episodio: " + 
                            episode.name());
                    episodeCharacters.put(episode.id(), new ArrayList<>());
                } catch (Exception ex) {
                    errorCount.incrementAndGet();
                    updateStatus("Error inesperado cargando personajes para " + 
                            episode.name() + ": " + ex.getMessage());
                    episodeCharacters.put(episode.id(), new ArrayList<>());
                } finally {
                    loadingSemaphore.release();
                }
            });
            
            threads.add(t);
            t.start();
        }

        Thread completionThread = new Thread(() -> {
            
            for (Thread t : threads) {
                
                try {
                    t.join();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    updateStatus("Interrupción durante espera de threads");
                    break;
                }
            }
            int errors = errorCount.get();
            int successful = episodeCharacters.size();
            
            if (errors > 0) {
                updateStatus("Carga completa con errores. Episodios procesados: " + 
                        successful + ", Errores: " + errors);
            } else {
                updateStatus("Carga completa exitosa. " + successful + 
                        " episodios con personajes cargados.");
            }
            if (onLoadingComplete != null) {
                onLoadingComplete.accept(true);
            }
        });
        completionThread.start();
    }

    private String extractSeasonFromEpisode(String episodeCode) {
        if (episodeCode != null && episodeCode.length() >= 3) {
            return episodeCode.substring(0, 3);
        }
        return "Unknown";
    }
    
    private void updateStatus(String status) {
        System.out.println("Status: " + status);
        if (onStatusUpdate != null) {
            onStatusUpdate.accept(status);
        }
    }
    
    public Map<String, List<Episode>> getSeasonEpisodes() {
        return new HashMap<>(seasonEpisodes);
    }
    
    public List<Charac> getEpisodeCharacters(int episodeId) {
        return episodeCharacters.getOrDefault(episodeId, new ArrayList<>());
    }
    
    public String getAppInfo() {
        return """
            Aplicación Rick and Morty
            -------------------------
            Desarrollada usando:
            - Java HttpClient para peticiones API
            - Jackson para mapeo JSON
            - Hilos manuales para carga concurrente
            - ConcurrentHashMap para almacenamiento thread-safe
            - Semáforos para control de concurrencia
            - Patrón MVVM para separación de responsabilidades
            - Records para mapeo de datos
            - Swing para interfaces gráficas
            
            Esta aplicación consulta la API de Rick and Morty para
            obtener información sobre episodios y personajes de la serie.
            
            Correcciones implementadas:
            - Validación de códigos de respuesta HTTP
            - Timeouts para peticiones HTTP
            - Validación de URLs antes del fetch
            - Manejo robusto de errores
            - Logging de errores mejorado
            - Control de concurrencia optimizado
            """;
    }
}