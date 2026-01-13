package org.raflab.studsluzba.ui;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import org.raflab.studsluzba.controllers.response.StudentIndeksResponse;
import org.raflab.studsluzba.model.dtos.StudentProfileDTO;
import org.raflab.studsluzba.navigation.NavigationService;
import org.raflab.studsluzba.navigation.Route;
import org.raflab.studsluzba.navigation.StudentTab;
import org.raflab.studsluzba.service.StudentApiService;
import org.springframework.stereotype.Component;

@Component
public class SearchStudentController {

    private final StudentApiService api;
    private final NavigationService nav;

    public SearchStudentController(StudentApiService api, NavigationService nav) {
        this.api = api;
        this.nav = nav;
    }

    @FXML private TextField txtIndeks;
    @FXML private Label lblMsg;
    @FXML private ProgressIndicator progress;

    public void setInitialIndeksText(String text) {
        if (text != null && !text.trim().isEmpty()) {
            txtIndeks.setText(text);
        }
    }

    private String normalizeIndeksShort(String input) {
        if (input == null) return "";
        String s = input.trim().toUpperCase();
        s = s.replaceAll("\\s+", "");

        if (s.matches("^[A-Z]+\\d{2}/\\d+$")) {
            String program = s.replaceAll("^([A-Z]+)\\d{2}/\\d+$", "$1");
            String yy = s.replaceAll("^[A-Z]+(\\d{2})/\\d+$", "$1");
            String broj = s.replaceAll("^[A-Z]+\\d{2}/(\\d+)$", "$1");
            return program + yy + broj;
        }

        if (s.matches("^[A-Z]+\\d+/\\d{2}$")) {
            String program = s.replaceAll("^([A-Z]+)\\d+/\\d{2}$", "$1");
            String broj = s.replaceAll("^[A-Z]+(\\d+)/\\d{2}$", "$1");
            String yy = s.replaceAll("^[A-Z]+\\d+/(\\d{2})$", "$1");
            return program + yy + broj;
        }

        if (s.matches("^[A-Z]+\\d+/\\d{4}$")) {
            String program = s.replaceAll("^([A-Z]+)\\d+/\\d{4}$", "$1");
            String broj = s.replaceAll("^[A-Z]+(\\d+)/\\d{4}$", "$1");
            String godina4 = s.replaceAll("^[A-Z]+\\d+/(\\d{4})$", "$1");
            String yy = godina4.substring(2);
            return program + yy + broj;
        }

        if (s.matches("^[A-Z]+\\d{4}/\\d+$")) {
            String program = s.replaceAll("^([A-Z]+)\\d{4}/\\d+$", "$1");
            String godina4 = s.replaceAll("^[A-Z]+(\\d{4})/\\d+$", "$1");
            String broj = s.replaceAll("^[A-Z]+\\d{4}/(\\d+)$", "$1");
            String yy = godina4.substring(2);
            return program + yy + broj;
        }

        return s.replaceAll("[^A-Z0-9]", "");
    }

    @FXML
    public void onSearch() {
        final String indeksRaw = txtIndeks.getText() == null ? "" : txtIndeks.getText().trim();
        final String indeks = normalizeIndeksShort(indeksRaw);

        if (indeks.isEmpty()) {
            lblMsg.setText("Unesite broj indeksa.");
            return;
        }

        // zapamti input u trenutnoj ruti (da se vrati kad ideš Back)
        Route cur = nav.getCurrent();
        if (cur != null) {
            nav.updateCurrent(cur.withSearchText(indeksRaw));
        }

        lblMsg.setText("");
        progress.setVisible(true);

        api.fastSearchByIndex(indeks)
                .flatMap(indeksRes ->
                        api.getProfile(indeksRes.getId())
                                .map(profile -> new Result(indeksRes, profile))
                )
                .subscribe(
                        res -> Platform.runLater(() -> {
                            progress.setVisible(false);
                            nav.navigate(Route.studentProfile(res.indeksRes, res.profile, StudentTab.LICNI));
                        }),
                        err -> Platform.runLater(() -> {
                            progress.setVisible(false);
                            err.printStackTrace();

                            String msg = "Greška: " + err.getClass().getSimpleName();
                            if (err instanceof org.springframework.web.reactive.function.client.WebClientResponseException) {
                                org.springframework.web.reactive.function.client.WebClientResponseException wex =
                                        (org.springframework.web.reactive.function.client.WebClientResponseException) err;
                                msg += " HTTP " + wex.getRawStatusCode() + " " + wex.getStatusText()
                                        + " | body: " + wex.getResponseBodyAsString();
                            } else if (err.getMessage() != null) {
                                msg += " | " + err.getMessage();
                            }

                            lblMsg.setText(msg);
                        })
                );
    }

    private static class Result {
        final StudentIndeksResponse indeksRes;
        final StudentProfileDTO profile;

        Result(StudentIndeksResponse indeksRes, StudentProfileDTO profile) {
            this.indeksRes = indeksRes;
            this.profile = profile;
        }
    }
}
