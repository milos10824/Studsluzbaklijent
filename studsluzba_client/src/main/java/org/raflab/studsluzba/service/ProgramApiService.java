package org.raflab.studsluzba.service;

import org.raflab.studsluzba.controllers.response.PredmetResponse;
import org.raflab.studsluzba.controllers.response.StudijskiProgramResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import org.raflab.studsluzba.dto.request.DodajPredmetNaProgramRequest;
import org.raflab.studsluzba.dto.response.ProsecnaOcenaResponse;


import java.util.List;

@Service
public class ProgramApiService {

    private final WebClient web;

    public ProgramApiService(WebClient web) {
        this.web = web;
    }

    // 1) svi programi
    public Mono<List<StudijskiProgramResponse>> getStudijskiProgrami() {
        return web.get()
                .uri("/studijski-programi")
                .retrieve()
                .bodyToFlux(StudijskiProgramResponse.class)
                .collectList();
    }

    // 2) predmeti na programu
    public Mono<List<StudijskiProgramResponse.PredmetLight>> getPredmetiNaProgramu(Long programId) {
        return web.get()
                .uri("/studijski-programi/{id}/predmeti", programId)
                .retrieve()
                .bodyToFlux(StudijskiProgramResponse.PredmetLight.class)
                .collectList();
    }



    // 3) svi predmeti (za ComboBox u dodavanju)
    public Mono<List<PredmetResponse>> getSviPredmeti() {
        return web.get()
                .uri("/predmeti")
                .retrieve()
                .bodyToFlux(PredmetResponse.class)
                .collectList();
    }

    // 4) dodaj predmet na program
    public Mono<Void> dodajPredmetNaProgram(Long programId, Long predmetId) {
        return web.post()
                .uri("/studijski-programi/{id}/predmeti/{predmetId}", programId, predmetId)
                .retrieve()
                .bodyToMono(Void.class);
    }


    // 5) prosek ocena po godinama (tabela)
    public Mono<List<ProsecnaOcenaResponse>> getProsecnaOcenaPredmetaZaRaspon(Long predmetId, int odGod, int doGod) {
        return web.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/predmeti/{predmetId}/prosek") // <- prilagodi
                        .queryParam("od", odGod)
                        .queryParam("do", doGod)
                        .build(predmetId))
                .retrieve()
                .bodyToFlux(ProsecnaOcenaResponse.class)
                .collectList();
    }


}
