package org.raflab.studsluzba.ui;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import org.raflab.studsluzba.service.StudentApiService;
import org.raflab.studsluzba.controllers.response.StudentIndeksResponse;
import org.raflab.studsluzba.model.dtos.StudentProfileDTO;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Component
public class SearchStudentController {

    private final StudentApiService api;
    private final MainController mainController;
    private final ApplicationContext ctx;

    private String normalizeIndeksShort(String input) {
        if (input == null) return "";

        // Korisnici unose razne formate:
        //  - RN1923 (program + YY + broj)
        //  - RN 19/23 (YY/broj)
        //  - RN 23/19 (broj/YY)
        //  - RN 23/2019 (broj/YYYY) ili RN 2019/23 (YYYY/broj)
        // Nama treba canonical: RN + YY + BROJ (npr. RN1923)

        String s = input.trim().toUpperCase();
        s = s.replaceAll("\\s+", "");

        // YY/BROJ -> RN1923
        if (s.matches("^[A-Z]+\\d{2}/\\d+$")) {
            String program = s.replaceAll("^([A-Z]+)\\d{2}/\\d+$", "$1");
            String yy = s.replaceAll("^[A-Z]+(\\d{2})/\\d+$", "$1");
            String broj = s.replaceAll("^[A-Z]+\\d{2}/(\\d+)$", "$1");
            return program + yy + broj;
        }

        // BROJ/YY -> RN1923
        if (s.matches("^[A-Z]+\\d+/\\d{2}$")) {
            String program = s.replaceAll("^([A-Z]+)\\d+/\\d{2}$", "$1");
            String broj = s.replaceAll("^[A-Z]+(\\d+)/\\d{2}$", "$1");
            String yy = s.replaceAll("^[A-Z]+\\d+/(\\d{2})$", "$1");
            return program + yy + broj;
        }

        // BROJ/YYYY -> RNYYBROJ
        if (s.matches("^[A-Z]+\\d+/\\d{4}$")) {
            String program = s.replaceAll("^([A-Z]+)\\d+/\\d{4}$", "$1");
            String broj = s.replaceAll("^[A-Z]+(\\d+)/\\d{4}$", "$1");
            String godina4 = s.replaceAll("^[A-Z]+\\d+/(\\d{4})$", "$1");
            String yy = godina4.substring(2);
            return program + yy + broj;
        }

        // YYYY/BROJ -> RNYYBROJ
        if (s.matches("^[A-Z]+\\d{4}/\\d+$")) {
            String program = s.replaceAll("^([A-Z]+)\\d{4}/\\d+$", "$1");
            String godina4 = s.replaceAll("^[A-Z]+(\\d{4})/\\d+$", "$1");
            String broj = s.replaceAll("^[A-Z]+\\d{4}/(\\d+)$", "$1");
            String yy = godina4.substring(2);
            return program + yy + broj;
        }

        // default: ukloni sve što nije slovo/broj (npr. "RN-19/23")
        s = s.replaceAll("[^A-Z0-9]", "");
        return s;
    }

    public SearchStudentController(StudentApiService api, MainController mainController, ApplicationContext ctx) {
        this.api = api;
        this.mainController = mainController;
        this.ctx = ctx;
    }

    @FXML private TextField txtIndeks;
    @FXML private Label lblMsg;
    @FXML private ProgressIndicator progress;

    @FXML
    public void onSearch() {
        String indeksRaw = txtIndeks.getText() == null ? "" : txtIndeks.getText().trim();
        String indeks = normalizeIndeksShort(indeksRaw);

        if (indeks.isEmpty()) {
            lblMsg.setText("Unesite broj indeksa.");
            return;
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
                            openProfile(res.indeksRes, res.profile);
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

    private void openProfile(StudentIndeksResponse indeksRes, StudentProfileDTO profile) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/StudentProfileTabs.fxml"));
            loader.setControllerFactory(ctx::getBean);
            Parent view = loader.load();

            StudentProfileTabsController ctrl = loader.getController();
            ctrl.setData(indeksRes, profile);

            mainController.showView(view);
        } catch (Exception e) {
            e.printStackTrace();
            lblMsg.setText("Greška pri otvaranju profila.");
        }
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
