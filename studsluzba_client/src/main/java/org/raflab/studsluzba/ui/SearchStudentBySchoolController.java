package org.raflab.studsluzba.ui;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.raflab.studsluzba.controllers.response.SrednjaSkolaResponse;
import org.raflab.studsluzba.controllers.response.StudentIndeksResponse;
import org.raflab.studsluzba.model.dtos.StudentDTO;
import org.raflab.studsluzba.model.dtos.StudentProfileDTO;
import org.raflab.studsluzba.navigation.NavigationService;
import org.raflab.studsluzba.navigation.Route;
import org.raflab.studsluzba.navigation.StudentTab;
import org.raflab.studsluzba.service.StudentApiService;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
public class SearchStudentBySchoolController {

    private final StudentApiService api;
    private final NavigationService nav;

    public SearchStudentBySchoolController(StudentApiService api, NavigationService nav) {
        this.api = api;
        this.nav = nav;
    }

    @FXML private ComboBox<SrednjaSkolaResponse> cmbSkole;
    @FXML private ProgressIndicator progress;
    @FXML private Label lblMsg;

    @FXML private TableView<StudentDTO> tbl;
    @FXML private TableColumn<StudentDTO, String> colIme;
    @FXML private TableColumn<StudentDTO, String> colPrezime;
    @FXML private TableColumn<StudentDTO, String> colIndeks;
    @FXML private TableColumn<StudentDTO, String> colAktivan;

    @FXML
    public void initialize() {
        colIme.setCellValueFactory(c -> new SimpleStringProperty(safe(c.getValue().getIme())));
        colPrezime.setCellValueFactory(c -> new SimpleStringProperty(safe(c.getValue().getPrezime())));
        colIndeks.setCellValueFactory(c -> new SimpleStringProperty(formatIndeks(c.getValue())));
        colAktivan.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().isAktivanIndeks() ? "DA" : "NE"));

        tbl.setRowFactory(tv -> {
            TableRow<StudentDTO> row = new TableRow<>();
            row.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !row.isEmpty()) openSelected(row.getItem());
            });
            return row;
        });

        cmbSkole.setCellFactory(list -> new ListCell<>() {
            @Override protected void updateItem(SrednjaSkolaResponse item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : formatSkola(item));
            }
        });
        cmbSkole.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(SrednjaSkolaResponse item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : formatSkola(item));
            }
        });

        loadSkole();
    }

    private void loadSkole() {
        progress.setVisible(true);
        api.getSrednjeSkole()
                .subscribe(
                        list -> Platform.runLater(() -> {
                            cmbSkole.getItems().setAll(list == null ? List.of() : list);
                            progress.setVisible(false);
                        }),
                        err -> Platform.runLater(() -> {
                            progress.setVisible(false);
                            showError("Ne mogu da učitam srednje škole: " + err.getMessage());
                        })
                );
    }

    @FXML
    public void onSearch() {
        lblMsg.setText("");
        SrednjaSkolaResponse skola = cmbSkole.getValue();
        if (skola == null || skola.getId() == null) {
            lblMsg.setText("Izaberi srednju školu.");
            return;
        }

        progress.setVisible(true);
        tbl.setDisable(true);

        api.getStudentiPoSrednjojSkoli(skola.getId())
                .subscribe(
                        list -> Platform.runLater(() -> {
                            tbl.getItems().setAll(list == null ? List.of() : list);
                            progress.setVisible(false);
                            tbl.setDisable(false);
                            if (list == null || list.isEmpty()) lblMsg.setText("Nema rezultata.");
                        }),
                        err -> Platform.runLater(() -> {
                            progress.setVisible(false);
                            tbl.setDisable(false);
                            showError("Greška pri pretrazi: " + err.getMessage());
                        })
                );
    }

    private void openSelected(StudentDTO dto) {
        if (dto == null || dto.getIdIndeks() == null) {
            showInfo("Student nema aktivan indeks – ne može se otvoriti profil.");
            return;
        }

        Long indeksId = dto.getIdIndeks();

        progress.setVisible(true);
        tbl.setDisable(true);

        Mono<StudentIndeksResponse> mIndeks = api.getStudentIndeks(indeksId);
        Mono<StudentProfileDTO> mProfile = api.getProfile(indeksId);

        Mono.zip(mIndeks, mProfile)
                .subscribe(
                        tup -> Platform.runLater(() -> {
                            progress.setVisible(false);
                            tbl.setDisable(false);
                            nav.navigate(Route.studentProfile(tup.getT1(), tup.getT2(), StudentTab.LICNI));
                        }),
                        err -> Platform.runLater(() -> {
                            progress.setVisible(false);
                            tbl.setDisable(false);
                            showError("Ne mogu da otvorim profil: " + err.getMessage());
                        })
                );
    }

    private static String formatSkola(SrednjaSkolaResponse s) {
        String naziv = safe(s.getNaziv());
        String mesto = safe(s.getMesto());
        String vrsta = safe(s.getVrsta());
        String tail = (mesto + (vrsta.isBlank() ? "" : (", " + vrsta))).trim();
        return tail.isBlank() ? naziv : (naziv + " (" + tail + ")");
    }

    private static String formatIndeks(StudentDTO s) {
        if (s == null) return "";
        String prog = safe(s.getStudProgramOznaka());
        int godina = s.getGodinaUpisa();
        int yy = godina > 0 ? (godina % 100) : 0;
        int broj = s.getBroj();
        return (prog + " " + String.format("%02d", yy) + "/" + broj).trim();
    }

    private static String safe(String s) { return s == null ? "" : s; }

    private void showError(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle("Greška");
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }

    private void showInfo(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("Info");
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }
}
