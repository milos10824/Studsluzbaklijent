package org.raflab.studsluzba.ui;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import org.raflab.studsluzba.controllers.request.IspitniRokRequest;
import org.raflab.studsluzba.controllers.response.IspitniRokResponse;
import org.raflab.studsluzba.navigation.NavigationService;
import org.raflab.studsluzba.navigation.Route;
import org.raflab.studsluzba.service.ExamApiService;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class ExamPeriodsController {

    private final ExamApiService api;
    private final NavigationService nav;

    private Long activeSkolskaGodinaId;

    public ExamPeriodsController(ExamApiService api, NavigationService nav) {
        this.api = api;
        this.nav = nav;
    }

    @FXML private TableView<IspitniRokResponse> table;
    @FXML private TableColumn<IspitniRokResponse, String> colNaziv;
    @FXML private TableColumn<IspitniRokResponse, String> colOd;
    @FXML private TableColumn<IspitniRokResponse, String> colDo;
    @FXML private ProgressIndicator loader;

    @FXML
    public void initialize() {
        colNaziv.setCellValueFactory(c -> new SimpleStringProperty(nullSafe(c.getValue().getNaziv())));
        colOd.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().getDatumPocetka())));
        colDo.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().getDatumZavrsetka())));

        table.setRowFactory(tv -> {
            TableRow<IspitniRokResponse> row = new TableRow<>();
            row.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !row.isEmpty()) {
                    nav.navigate(Route.examsByPeriod(row.getItem()));
                }
            });
            return row;
        });

        refresh();
    }

    @FXML
    public void refresh() {
        setLoading(true);
        api.getIspitniRokovi()
                .subscribe(list -> Platform.runLater(() -> {
                    table.setItems(FXCollections.observableArrayList(list));

                    // uzmi skolskaGodinaId iz roka koji pripada aktivnoj skolskoj godini
                    activeSkolskaGodinaId = list.stream()
                            .filter(IspitniRokResponse::isAktivnaSkolskaGodina)
                            .map(IspitniRokResponse::getSkolskaGodinaId)
                            .findFirst()
                            .orElse(null);

                    setLoading(false);
                }), err -> Platform.runLater(() -> {
                    setLoading(false);
                    alert("Greška", "Neuspešno učitavanje ispitnih rokova.");
                }));
    }

    @FXML
    public void openCreateDialog() {
        Dialog<IspitniRokRequest> dialog = new Dialog<>();
        dialog.setTitle("Novi ispitni rok");

        ButtonType saveBtn = new ButtonType("Sačuvaj", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtn, ButtonType.CANCEL);

        TextField tfNaziv = new TextField();
        DatePicker dpOd = new DatePicker();
        DatePicker dpDo = new DatePicker();

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);

        grid.addRow(0, new Label("Naziv:"), tfNaziv);
        grid.addRow(1, new Label("Datum od:"), dpOd);
        grid.addRow(2, new Label("Datum do:"), dpDo);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(bt -> {
            if (bt != saveBtn) return null;

            String naziv = tfNaziv.getText() != null ? tfNaziv.getText().trim() : "";
            LocalDate od = dpOd.getValue();
            LocalDate d0 = dpDo.getValue();

            if (naziv.isEmpty() || od == null || d0 == null) {
                alert("Validacija", "Sva polja su obavezna.");
                return null;
            }
            if (od.isAfter(d0)) {
                alert("Validacija", "Datum od ne sme biti posle datuma do.");
                return null;
            }

            if (activeSkolskaGodinaId == null) {
                alert("Validacija", "Nije pronađena aktivna školska godina. Nije moguće kreirati ispitni rok.");
                return null;
            }

            IspitniRokRequest req = new IspitniRokRequest();
            req.setNaziv(naziv);
            req.setDatumPocetka(od);
            req.setDatumZavrsetka(d0);
            req.setSkolskaGodinaId(activeSkolskaGodinaId);

            // skolskaGodinaId po potrebi (ako backend zahteva), u suprotnom ostavi null
            return req;
        });

        dialog.showAndWait().ifPresent(req -> {
            if (req == null) return;
            setLoading(true);
            api.createIspitniRok(req).subscribe(ok -> Platform.runLater(() -> {
                setLoading(false);
                refresh();
            }), err -> Platform.runLater(() -> {
                setLoading(false);
                alert("Greška", "Neuspešno kreiranje ispitnog roka.");
            }));
        });
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

    private String nullSafe(String s) { return s == null ? "" : s; }
}
