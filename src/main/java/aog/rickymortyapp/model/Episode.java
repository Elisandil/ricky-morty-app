package aog.rickymortyapp.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Episode(
    int id,
    String name,
    String air_date,
    String episode,
    String[] characters
) {}
