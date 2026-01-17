package org.raflab.studsluzba.ui;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.raflab.studsluzba.controllers.response.PredmetResponse;
import org.raflab.studsluzba.controllers.response.StudijskiProgramResponse;
import org.raflab.studsluzba.dto.response.ProsecnaOcenaResponse;
import org.raflab.studsluzba.navigation.NavigationService;
import org.raflab.studsluzba.navigation.Route;
import org.raflab.studsluzba.service.ProgramApiService;
import org.springframework.stereotype.Component;

@Component
public class ProgramDetailsController {

    private final ProgramApiService api;
    private final NavigationService nav;

    private StudijskiProgramResponse program;

    public ProgramDetailsController(ProgramApiService api, NavigationService nav) {
        this.api = api;
        this.nav = nav;
    }

    @FXML private Label lblProgram;

    // predmeti na programu
    @FXML private TableView<StudijskiProgramResponse.PredmetLight> tblPredmeti;
    @FXML private TableColumn<StudijskiProgramResponse.PredmetLight, String> colPredmetNaziv;

    // prosek
    @FXML private ComboBox<PredmetResponse> cbPredmetZaProsek;
    @FXML private TextField tfGodOd;
    @FXML private TextField tfGodDo;
    @FXML private TableView<ProsecnaOcenaResponse> tblProsek;
    @FXML private TableColumn<ProsecnaOcenaResponse, String> colGodina;
    @FXML private TableColumn<ProsecnaOcenaResponse, String> colProsek;

    @FXML private ProgressIndicator loader;

    public void setProgram(StudijskiProgramResponse program) {
        this.program = program;
        if (lblProgram != null) {
            lblProgram.setText(program.getOznaka() + " - " + program.getNaziv());
        }
        refreshPredmeti();
        preloadPredmetiZaProsek();
    }

    @FXML
    public void initialize() {
        // tabela predmeta na programu
        colPredmetNaziv.setCellValueFactory(c ->
                new SimpleStringProperty(ns(c.getValue().getNaziv()))
        );

        // tabela proseka
        colGodina.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().getGodina())));
        colProsek.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().getProsecnaOcena())));

        // combo za prosek - prikazuj naziv
        cbPredmetZaProsek.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(PredmetResponse item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.getNaziv());
            }
        });
        cbPredmetZaProsek.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(PredmetResponse item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.getNaziv());
            }
        });
    }

    @FXML
    public void back() {
        nav.navigate(Route.studyPrograms());
    }

    @FXML
    public void refreshPredmeti() {
        if (program == null) return;

        setLoading(true);
        api.getPredmetiNaProgramu(program.getId())
                .subscribe(list -> Platform.runLater(() -> {
                    System.out.println("REFRESH PREDMETI size=" + (list == null ? 0 : list.size()));

                    tblPredmeti.getItems().setAll(list);
                    tblPredmeti.refresh();

                    setLoading(false);
                }), err -> Platform.runLater(() -> {
                    setLoading(false);
                    err.printStackTrace();
                    alert("Greška", "Neuspešno učitavanje predmeta na programu.");
                }));
    }

    private void preloadPredmetiZaProsek() {
        setLoading(true);
        api.getSviPredmeti()
                .subscribe(list -> Platform.runLater(() -> {
                    cbPredmetZaProsek.setItems(FXCollections.observableArrayList(list));
                    setLoading(false);
                }), err -> Platform.runLater(() -> {
                    setLoading(false);
                    err.printStackTrace();
                    alert("Greška", "Neuspešno učitavanje liste predmeta.");
                }));
    }

    @FXML
    public void dodajPredmetNaProgram() {
        if (program == null) return;

        setLoading(true);

        api.getSviPredmeti()
                .doFinally(sig -> Platform.runLater(() -> setLoading(false)))
                .subscribe(all -> Platform.runLater(() -> {

                    if (all == null || all.isEmpty()) {
                        alert("Info", "Nema dostupnih predmeta.");
                        return;
                    }

                    Dialog<PredmetResponse> dialog = new Dialog<>();
                    dialog.setTitle("Dodaj predmet");
                    dialog.setHeaderText("Izaberi predmet koji želiš da dodaš na program");

                    ButtonType okBtn = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
                    dialog.getDialogPane().getButtonTypes().addAll(okBtn, ButtonType.CANCEL);

                    ComboBox<PredmetResponse> cb = new ComboBox<>(FXCollections.observableArrayList(all));
                    cb.setMaxWidth(Double.MAX_VALUE);

                    cb.setCellFactory(lv -> new ListCell<>() {
                        @Override protected void updateItem(PredmetResponse item, boolean empty) {
                            super.updateItem(item, empty);
                            setText(empty || item == null ? "" : item.getNaziv());
                        }
                    });

                    cb.setButtonCell(new ListCell<>() {
                        @Override protected void updateItem(PredmetResponse item, boolean empty) {
                            super.updateItem(item, empty);
                            setText(empty || item == null ? "" : item.getNaziv());
                        }
                    });

                    cb.getSelectionModel().selectFirst();
                    dialog.getDialogPane().setContent(cb);

                    dialog.setResultConverter(btn -> btn == okBtn ? cb.getValue() : null);

                    dialog.showAndWait().ifPresent(predmet -> {
                        if (predmet == null) return;

                        setLoading(true);

                        api.dodajPredmetNaProgram(program.getId(), predmet.getId())
                                .doFinally(sig -> Platform.runLater(() -> setLoading(false)))
                                .subscribe(v -> Platform.runLater(() -> {
                                    refreshPredmeti();
                                    alert("Info", "Predmet dodat na program.");
                                }), err -> Platform.runLater(() -> {
                                    err.printStackTrace();
                                    alert("Greška", "Neuspešno dodavanje predmeta na program.");
                                }));
                    });

                }), err -> Platform.runLater(() -> {
                    err.printStackTrace();
                    alert("Greška", "Neuspešno učitavanje predmeta.");
                }));
    }

    @FXML
    public void ucitajProsek() {
        PredmetResponse predmet = cbPredmetZaProsek.getValue();
        Integer od = parseInt(tfGodOd.getText());
        Integer d0 = parseInt(tfGodDo.getText());

        if (predmet == null || od == null || d0 == null) {
            alert("Validacija", "Izaberi predmet i unesi opseg godina (od/do).");
            return;
        }
        if (od > d0) {
            alert("Validacija", "Godina OD ne sme biti veća od godine DO.");
            return;
        }

        setLoading(true);
        api.getProsecnaOcenaPredmetaZaRaspon(predmet.getId(), od, d0)
                .subscribe(list -> Platform.runLater(() -> {
                    tblProsek.setItems(FXCollections.observableArrayList(list));
                    setLoading(false);
                }), err -> Platform.runLater(() -> {
                    setLoading(false);

                    if (err instanceof org.springframework.web.reactive.function.client.WebClientResponseException) {
                        org.springframework.web.reactive.function.client.WebClientResponseException e =
                                (org.springframework.web.reactive.function.client.WebClientResponseException) err;

                        System.out.println("PROSEK STATUS: " + e.getStatusCode());
                        System.out.println("PROSEK BODY: " + e.getResponseBodyAsString());

                        alert("Greška", e.getStatusCode() + " " + e.getResponseBodyAsString());
                    } else {
                        err.printStackTrace();
                        alert("Greška", "Neuspešno učitavanje proseka.");
                    }
                }));
    }

    @FXML
    public void stampajProsek() {
        alert("Info", "nesto.");
    }

    private Integer parseInt(String s) {
        try { return Integer.parseInt(s.trim()); }
        catch (Exception e) { return null; }
    }

    private void setLoading(boolean v) {
        loader.setVisible(v);
        loader.setManaged(v);
    }

    private void alert(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }

    private String ns(String s) { return s == null ? "" : s; }
}
