package org.raflab.studsluzba.ui;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import org.raflab.studsluzba.controllers.response.StudentIndeksResponse;
import org.raflab.studsluzba.model.dtos.StudentProfileDTO;
import org.springframework.stereotype.Component;

@Component
public class StudentProfileController {

    @FXML private Label lblHeader;
    @FXML private Label lblImePrezime;
    @FXML private Label lblIndeks;
    @FXML private Label lblProgram;
    @FXML private Label lblEspb;
    @FXML private Label lblProsek;

    public void setData(StudentIndeksResponse indeksRes, StudentProfileDTO profile) {

        // ============ IME / PREZIME ============
        String ime = null;
        String prezime = null;

        // 1) iz fastsearch response-a (flat)
        if (indeksRes != null) {
            try {
                ime = indeksRes.getImeStudenta();
                prezime = indeksRes.getPrezimeStudenta();
            } catch (Exception ignore) {
                // ako je klijent slučajno na starom jar-u
            }
        }

        // 2) fallback iz profila (ako postoji)
        if ((ime == null || prezime == null) && profile != null
                && profile.getIndeks() != null
                && profile.getIndeks().getStudent() != null) {
            if (ime == null) ime = profile.getIndeks().getStudent().getIme();
            if (prezime == null) prezime = profile.getIndeks().getStudent().getPrezime();
        }

        String imePrezime = ((ime == null ? "" : ime) + " " + (prezime == null ? "" : prezime)).trim();
        if (imePrezime.isBlank()) imePrezime = "(nepoznato)";

        lblHeader.setText("Profil: " + imePrezime);
        lblImePrezime.setText(imePrezime);

        // ============ PROGRAM ============
        String programOznaka = (indeksRes != null && indeksRes.getStudProgramOznaka() != null)
                ? indeksRes.getStudProgramOznaka()
                : "";

        String programNaziv = null;
        if (indeksRes != null) {
            try {
                programNaziv = indeksRes.getStudijskiProgramNaziv();
            } catch (Exception ignore) {}
        }

        if (programNaziv != null && !programNaziv.isBlank()) {
            lblProgram.setText(programNaziv);
        } else {
            lblProgram.setText(programOznaka);
        }

        // ============ INDEKS (RN 24/1) ============
        int godina = (indeksRes != null) ? indeksRes.getGodina()
                : (profile != null && profile.getIndeks() != null ? profile.getIndeks().getGodina() : 0);
        int yy = godina > 0 ? (godina % 100) : 0;

        Integer broj = (indeksRes != null) ? indeksRes.getBroj()
                : (profile != null && profile.getIndeks() != null ? profile.getIndeks().getBroj() : null);

        String indeksStr = programOznaka + " " + String.format("%02d", yy) + "/" + (broj == null ? "" : broj);
        lblIndeks.setText(indeksStr.trim());

        // ============ ESPB / PROSEK ============
        Integer espb = (indeksRes != null) ? indeksRes.getOstvarenoEspb() : null;
        lblEspb.setText(String.valueOf(espb == null ? 0 : espb));

        lblProsek.setText("—");
    }

}
