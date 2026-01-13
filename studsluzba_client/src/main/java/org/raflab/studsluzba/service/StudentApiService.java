package org.raflab.studsluzba.service;


import org.raflab.studsluzba.controllers.request.ObnovaGodineRequest;
import org.raflab.studsluzba.controllers.request.UpisGodineRequest;
import org.raflab.studsluzba.controllers.request.UplataRequest;
import org.raflab.studsluzba.controllers.response.*;
import org.raflab.studsluzba.model.dtos.StudentDTO;
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


    public Mono<StudentIndeksResponse> getStudentIndeks(Long indeksId) {
        return webClient.get()
                .uri("/api/student/indeks/{id}", indeksId)
                .retrieve()
                .bodyToMono(StudentIndeksResponse.class);
    }

    public Mono<PageResponse<StudentDTO>> searchStudents(
            String ime,
            String prezime,
            String studProgram,
            Integer godina,
            Integer broj,
            int page,
            int size
    ) {
        return webClient.get()
                .uri(uriBuilder -> {
                    var b = uriBuilder.path("/api/student/search")
                            .queryParam("page", page)
                            .queryParam("size", size);

                    if (ime != null && !ime.trim().isBlank()) b = b.queryParam("ime", ime.trim());
                    if (prezime != null && !prezime.trim().isBlank()) b = b.queryParam("prezime", prezime.trim());
                    if (studProgram != null && !studProgram.trim().isBlank()) b = b.queryParam("studProgram", studProgram.trim());
                    if (godina != null) b = b.queryParam("godina", godina);
                    if (broj != null) b = b.queryParam("broj", broj);

                    return b.build();
                })
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<PageResponse<StudentDTO>>() {});
    }
    public Mono<UplataResponse> addUplata(Long indeksId, double iznosRsd) {
        UplataRequest req = new UplataRequest();
        req.setIndeksId(indeksId);
        req.setIznosRsd(iznosRsd);

        return webClient.post()
                .uri("/api/uplata/{indeksId}", indeksId)
                .bodyValue(req)
                .retrieve()
                .bodyToMono(UplataResponse.class);
    }

    public Mono<List<SrednjaSkolaResponse>> getSrednjeSkole() {
        return webClient.get()
                .uri("/srednje-skole")
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<SrednjaSkolaResponse>>() {});
    }

    public Mono<List<StudentDTO>> getStudentiPoSrednjojSkoli(Long srednjaSkolaId) {
        return webClient.get()
                .uri("/api/student/srednja-skola/{id}", srednjaSkolaId)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<StudentDTO>>() {});
    }
    public Mono<List<SkolskaGodinaResponse>> getSkolskeGodine() {
        return webClient.get()
                .uri("/api/skolske-godine")
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<SkolskaGodinaResponse>>() {});
    }

    public Mono<List<DrziPredmetResponse>> getDrziPredmet(Long skolskaGodinaId, String studProgramOznaka) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/drzi-predmet")
                        .queryParam("skolskaGodinaId", skolskaGodinaId)
                        .queryParam("studProgramOznaka", studProgramOznaka)
                        .build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<DrziPredmetResponse>>() {});
    }

    public Mono<UpisGodineResponse> addUpisGodine(UpisGodineRequest req) {
        return webClient.post()
                .uri("/api/upis-godine")
                .bodyValue(req)
                .retrieve()
                .bodyToMono(UpisGodineResponse.class);
    }

    public Mono<ObnovaGodineResponse> addObnovaGodine(ObnovaGodineRequest req) {
        return webClient.post()
                .uri("/api/obnova-godine")
                .bodyValue(req)
                .retrieve()
                .bodyToMono(ObnovaGodineResponse.class);
    }


}