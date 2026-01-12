package org.raflab.studsluzba.service;


import org.raflab.studsluzba.controllers.response.StudentIndeksResponse;
import org.raflab.studsluzba.controllers.response.StudentPodaciResponse;
import org.raflab.studsluzba.controllers.response.PolozenPredmetResponse;
import org.raflab.studsluzba.controllers.response.UplataResponse;
import org.raflab.studsluzba.controllers.response.UpisGodineResponse;
import org.raflab.studsluzba.controllers.response.ObnovaGodineResponse;
import org.raflab.studsluzba.model.dtos.StudentProfileDTO;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.core.ParameterizedTypeReference;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
public class StudentApiService {

    private final WebClient webClient;

    public StudentApiService(WebClient webClient) {
        this.webClient = webClient;
    }

    public Mono<StudentIndeksResponse> fastSearchByIndex(String indeksShort) {
        String cleaned = indeksShort == null ? "" : indeksShort.trim();

        System.out.println(">>> fastSearch indeksShort=" + cleaned);

        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/student/fastsearch")
                        .queryParam("indeksShort", cleaned)
                        .build())
                .retrieve()
                .bodyToMono(StudentIndeksResponse.class);
    }

    public Mono<StudentProfileDTO> getProfile(Long indeksId) {
        System.out.println(">>> getProfile indeksId=" + indeksId);

        // SERVER: GET /api/student/profile/{studentIndeksId}
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/student/profile/{studentIndeksId}")
                        .build(indeksId))
                .retrieve()
                .bodyToMono(StudentProfileDTO.class);
    }

    public Mono<StudentPodaciResponse> getStudentPodaci(Long studentPodaciId) {
        return webClient.get()
                .uri("/api/student/podaci/{id}", studentPodaciId)
                .retrieve()
                .bodyToMono(StudentPodaciResponse.class);
    }

    /**
     * Server vraća Spring Page JSON. Na klijentu mapiramo u laganu PageResponse strukturu.
     */
    public Mono<PageResponse<PolozenPredmetResponse>> getPolozeni(Long studentIndeksId, int page, int size) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/polozeni/{studentIndeksId}")
                        .queryParam("page", page)
                        .queryParam("size", size)
                        .build(studentIndeksId))
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<PageResponse<PolozenPredmetResponse>>() {});
    }

    public Mono<List<UplataResponse>> getUplate(Long studentIndeksId) {
        return webClient.get()
                .uri("/api/uplata/{indeksId}", studentIndeksId)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<UplataResponse>>() {});
    }

    public Mono<List<UpisGodineResponse>> getUpisi(Long studentIndeksId) {
        return webClient.get()
                .uri("/api/upis-godine/{studentIndeksId}", studentIndeksId)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<UpisGodineResponse>>() {});
    }

    public Mono<List<ObnovaGodineResponse>> getObnove(Long studentIndeksId) {
        return webClient.get()
                .uri("/api/obnova-godine/{studentIndeksId}", studentIndeksId)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<ObnovaGodineResponse>>() {});
    }
}