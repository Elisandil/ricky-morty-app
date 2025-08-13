package aog.rickymortyapp.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Info(
    int count,
    int pages,
    String next,
    String prev
) {}
