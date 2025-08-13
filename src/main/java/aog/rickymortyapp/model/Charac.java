package aog.rickymortyapp.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Charac(
    int id,
    String name,
    String status,
    String species,
    String gender,
    String image,
    String[] episode
) {}
