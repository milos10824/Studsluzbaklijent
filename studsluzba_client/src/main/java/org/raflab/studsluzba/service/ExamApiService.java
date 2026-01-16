package org.raflab.studsluzba.service;

import org.raflab.studsluzba.controllers.request.IspitRequest;
import org.raflab.studsluzba.controllers.request.IspitniRokRequest;
import org.raflab.studsluzba.controllers.response.*;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
public class ExamApiService {

    private final WebClient web;

    public ExamApiService(WebClient web) {
        this.web = web;
    }

    // --- Ispitni rokovi ---
    public Mono<List<IspitniRokResponse>> getIspitniRokovi() {
        return web.get()
                .uri("/ispitni-rokovi")
                .retrieve()
                .bodyToFlux(IspitniRokResponse.class)
                .collectList();
    }

    public Mono<IspitniRokResponse> createIspitniRok(IspitniRokRequest req) {
        return web.post()
                .uri("/ispitni-rokovi")
                .bodyValue(req)
                .retrieve()
                .bodyToMono(IspitniRokResponse.class);
    }

    // --- Ispiti ---
    public Mono<List<IspitResponse>> getIspitiByRok(Long rokId) {
        return web.get()
                .uri("/api/ispiti/rok/{rokId}", rokId)
                .retrieve()
                .bodyToFlux(IspitResponse.class)
                .collectList();
    }

    public Mono<IspitResponse> createIspit(IspitRequest req) {
        return web.post()
                .uri("/api/ispiti")
                .bodyValue(req)
                .retrieve()
                .bodyToMono(IspitResponse.class);
    }

    // --- Pomocno za formu (ComboBox) ---
    public Mono<List<PredmetResponse>> getPredmeti() {
        return web.get()
                .uri("/predmeti")
                .retrieve()
                .bodyToFlux(PredmetResponse.class)
                .collectList();
    }

    public Mono<List<NastavnikResponse>> getNastavnici() {
        return web.get()
                .uri("/api/nastavnik/all")
                .retrieve()
                .bodyToFlux(NastavnikResponse.class)
                .collectList()
                .doOnError(err -> {
                    if (err instanceof WebClientResponseException) {
                        WebClientResponseException e =
                                (WebClientResponseException) err;

                        System.out.println("STATUS: " + e.getStatusCode());
                        System.out.println("BODY: " + e.getResponseBodyAsString());
                    } else {
                        err.printStackTrace();
                    }
                });

    }

    public Mono<List<SkolskaGodinaResponse>> getSkolskeGodine() {
        return web.get()
                .uri("/api/skolske-godine")
                .retrieve()
                .bodyToFlux(SkolskaGodinaResponse.class)
                .collectList();
    }




}
