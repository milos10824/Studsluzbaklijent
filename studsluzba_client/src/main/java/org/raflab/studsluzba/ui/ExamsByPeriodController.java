package org.raflab.studsluzba.ui;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import org.raflab.studsluzba.controllers.request.IspitRequest;
import org.raflab.studsluzba.controllers.response.IspitResponse;
import org.raflab.studsluzba.controllers.response.IspitniRokResponse;
import org.raflab.studsluzba.controllers.response.NastavnikResponse;
import org.raflab.studsluzba.controllers.response.PredmetResponse;
import org.raflab.studsluzba.navigation.NavigationService;
import org.raflab.studsluzba.navigation.Route;
import org.raflab.studsluzba.service.ExamApiService;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Component
public class ExamsByPeriodController {

    private final ExamApiService api;
    private final NavigationService nav;

    private IspitniRokResponse rok;

    public ExamsByPeriodController(ExamApiService api, NavigationService nav) {
        this.api = api;
        this.nav = nav;
    }

    @FXML private Label lblRok;
    @FXML private TableView<IspitResponse> table;
    @FXML private TableColumn<IspitResponse, String> colPredmet;
    @FXML private TableColumn<IspitResponse, String> colDatum;
    @FXML private TableColumn<IspitResponse, String> colVreme;
    @FXML private TableColumn<IspitResponse, String> colNastavnik;
    @FXML private ProgressIndicator loader;

    public void setRok(IspitniRokResponse rok) {
        this.rok = rok;
        if (lblRok != null && rok != null) {
            lblRok.setText(rok.getNaziv() + " (" + rok.getDatumPocetka() + " - " + rok.getDatumZavrsetka() + ")");
        }
        refresh();
    }

    @FXML
    public void initialize() {
        colPredmet.setCellValueFactory(c -> new SimpleStringProperty(nullSafe(c.getValue().getPredmetNaziv())));
        colDatum.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().getDatum())));
        colVreme.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().getVremePocetka())));
        colNastavnik.setCellValueFactory(c -> new SimpleStringProperty(nullSafe(c.getValue().getNastavnikImePrezime())));
    }

    @FXML
    public void backToPeriods() {
        nav.navigate(Route.examPeriods());
    }

    @FXML
    public void refresh() {
        if (rok == null || rok.getId() == null) return;
        setLoading(true);
        api.getIspitiByRok(rok.getId())
                .subscribe(list -> Platform.runLater(() -> {
                    table.setItems(FXCollections.observableArrayList(list));
                    setLoading(false);
                }), err -> Platform.runLater(() -> {
                    setLoading(false);
                    alert("Greška", "Neuspešno učitavanje ispita za rok.");
                }));
    }

    @FXML
    public void openCreateDialog() {
        if (rok == null) return;

        setLoading(true);

        api.getPredmeti().zipWith(api.getNastavnici())
                .subscribe(tuple -> Platform.runLater(() -> {
                    setLoading(false);
                    showCreateExamDialog(tuple.getT1(), tuple.getT2());
                }), err -> Platform.runLater(() -> {
                    setLoading(false);
                    err.printStackTrace();
                    alert("Greška", err.getMessage());
                }));
    }

    private void showCreateExamDialog(List<PredmetResponse> predmeti, List<NastavnikResponse> nastavnici) {
        Dialog<IspitRequest> dialog = new Dialog<>();
        dialog.setTitle("Novi ispit");

        ButtonType saveBtn = new ButtonType("Sačuvaj", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtn, ButtonType.CANCEL);

        ComboBox<PredmetResponse> cbPredmet = new ComboBox<>(FXCollections.observableArrayList(predmeti));
        cbPredmet.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(PredmetResponse item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.getNaziv());
            }
        });
        cbPredmet.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(PredmetResponse item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.getNaziv());
            }
        });

        ComboBox<NastavnikResponse> cbNastavnik = new ComboBox<>(FXCollections.observableArrayList(nastavnici));
        cbNastavnik.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(NastavnikResponse item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : (item.getIme() + " " + nullSafe(item.getPrezime())));
            }
        });
        cbNastavnik.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(NastavnikResponse item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : (item.getIme() + " " + nullSafe(item.getPrezime())));
            }
        });

        DatePicker dpDatum = new DatePicker();
        TextField tfTime = new TextField(); // format HH:mm

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);

        grid.addRow(0, new Label("Rok:"), new Label(rok.getNaziv()));
        grid.addRow(1, new Label("Predmet:"), cbPredmet);
        grid.addRow(2, new Label("Nastavnik:"), cbNastavnik);
        grid.addRow(3, new Label("Datum:"), dpDatum);
        grid.addRow(4, new Label("Vreme (HH:mm):"), tfTime);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(bt -> {
            if (bt != saveBtn) return null;

            PredmetResponse p = cbPredmet.getValue();
            NastavnikResponse n = cbNastavnik.getValue();
            LocalDate datum = dpDatum.getValue();
            String timeStr = tfTime.getText() != null ? tfTime.getText().trim() : "";

            if (p == null || n == null || datum == null || timeStr.isEmpty()) {
                alert("Validacija", "Sva polja su obavezna.");
                return null;
            }

            // validacija datuma unutar roka
            if (rok.getDatumPocetka() != null && datum.isBefore(rok.getDatumPocetka())) {
                alert("Validacija", "Datum ispita je pre početka roka.");
                return null;
            }
            if (rok.getDatumZavrsetka() != null && datum.isAfter(rok.getDatumZavrsetka())) {
                alert("Validacija", "Datum ispita je posle završetka roka.");
                return null;
            }

            LocalTime vreme;
            try {
                vreme = LocalTime.parse(timeStr);
            } catch (Exception ex) {
                alert("Validacija", "Vreme mora biti u formatu HH:mm (npr. 09:30).");
                return null;
            }

            IspitRequest req = new IspitRequest();
            req.setDatum(datum);
            req.setVremePocetka(vreme);
            req.setPredmetId(p.getId());
            req.setNastavnikId(n.getId());
            req.setIspitniRokId(rok.getId());
            req.setZakljucen(false);
            return req;
        });

        dialog.showAndWait().ifPresent(req -> {
            if (req == null) return;
            setLoading(true);
            api.createIspit(req).subscribe(ok -> Platform.runLater(() -> {
                setLoading(false);
                refresh();
            }), err -> Platform.runLater(() -> {
                setLoading(false);
                err.printStackTrace();
                alert("Greška", err.getMessage());
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
