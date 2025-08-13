package aog.rickymortyapp.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PaginatedResponse<T>(
    Info info,
    T[] results
) {}
