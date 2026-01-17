package org.raflab.studsluzba.dto.request;

public class DodajPredmetNaProgramRequest {
    private Long programId;
    private Long predmetId;

    public Long getProgramId() {
        return programId;
    }

    public void setProgramId(Long programId) {
        this.programId = programId;
    }

    public Long getPredmetId() {
        return predmetId;
    }

    public void setPredmetId(Long predmetId) {
        this.predmetId = predmetId;
    }
}
