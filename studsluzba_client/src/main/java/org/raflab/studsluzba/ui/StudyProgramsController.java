package org.raflab.studsluzba.ui;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.raflab.studsluzba.controllers.response.StudijskiProgramResponse;
import org.raflab.studsluzba.navigation.NavigationService;
import org.raflab.studsluzba.navigation.Route;
import org.raflab.studsluzba.service.ProgramApiService;
import org.springframework.stereotype.Component;

@Component
public class StudyProgramsController {

    private final ProgramApiService api;
    private final NavigationService nav;

    public StudyProgramsController(ProgramApiService api, NavigationService nav) {
        this.api = api;
        this.nav = nav;
    }

    @FXML private TableView<StudijskiProgramResponse> table;
    @FXML private TableColumn<StudijskiProgramResponse, String> colNaziv;
    @FXML private TableColumn<StudijskiProgramResponse, String> colOznaka;
    @FXML private ProgressIndicator loader;

    @FXML
    public void initialize() {
        colNaziv.setCellValueFactory(c -> new SimpleStringProperty(ns(c.getValue().getNaziv())));
        colOznaka.setCellValueFactory(c -> new SimpleStringProperty(ns(c.getValue().getOznaka())));

        table.setRowFactory(tv -> {
            TableRow<StudijskiProgramResponse> row = new TableRow<>();
            row.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !row.isEmpty()) {
                    nav.navigate(Route.programDetails(row.getItem()));
                }
            });
            return row;
        });

        refresh();
    }

    @FXML
    public void refresh() {
        setLoading(true);
        api.getStudijskiProgrami().subscribe(list -> Platform.runLater(() -> {
            table.setItems(FXCollections.observableArrayList(list));
            setLoading(false);
        }), err -> Platform.runLater(() -> {
            setLoading(false);
            alert("Greška", "Neuspešno učitavanje studijskih programa.");
        }));
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
