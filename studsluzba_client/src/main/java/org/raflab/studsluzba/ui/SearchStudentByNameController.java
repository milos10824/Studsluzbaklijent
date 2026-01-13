package org.raflab.studsluzba.ui;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.raflab.studsluzba.controllers.response.StudentIndeksResponse;
import org.raflab.studsluzba.model.dtos.StudentDTO;
import org.raflab.studsluzba.model.dtos.StudentProfileDTO;
import org.raflab.studsluzba.navigation.NavigationService;
import org.raflab.studsluzba.navigation.Route;
import org.raflab.studsluzba.navigation.StudentTab;
import org.raflab.studsluzba.service.PageResponse;
import org.raflab.studsluzba.service.StudentApiService;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
public class SearchStudentByNameController {

    private final StudentApiService api;
    private final NavigationService nav;

    public SearchStudentByNameController(StudentApiService api, NavigationService nav) {
        this.api = api;
        this.nav = nav;
    }

    @FXML private TextField txtIme;
    @FXML private TextField txtPrezime;
    @FXML private TextField txtProgram;
    @FXML private TextField txtGodina;
    @FXML private TextField txtBroj;

    @FXML private ProgressIndicator progress;
    @FXML private Label lblMsg;

    @FXML private TableView<StudentDTO> tbl;
    @FXML private TableColumn<StudentDTO, String> colIme;
    @FXML private TableColumn<StudentDTO, String> colPrezime;
    @FXML private TableColumn<StudentDTO, String> colIndeks;
    @FXML private TableColumn<StudentDTO, String> colAktivan;

    @FXML private Button btnPrev;
    @FXML private Button btnNext;
    @FXML private Label lblPage;

    private int page = 0;
    private int size = 10;
    private int totalPages = 1;

    @FXML
    public void initialize() {
        colIme.setCellValueFactory(c -> new SimpleStringProperty(safe(c.getValue().getIme())));
        colPrezime.setCellValueFactory(c -> new SimpleStringProperty(safe(c.getValue().getPrezime())));
        colIndeks.setCellValueFactory(c -> new SimpleStringProperty(formatIndeks(c.getValue())));
        colAktivan.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().isAktivanIndeks() ? "DA" : "NE"));

        tbl.setRowFactory(tv -> {
            TableRow<StudentDTO> row = new TableRow<>();
            row.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !row.isEmpty()) {
                    openSelected(row.getItem());
                }
            });
            return row;
        });

        // Enter = pretraga
        txtIme.setOnAction(e -> onSearch());
        txtPrezime.setOnAction(e -> onSearch());
        txtProgram.setOnAction(e -> onSearch());
        txtGodina.setOnAction(e -> onSearch());
        txtBroj.setOnAction(e -> onSearch());

        refreshPager();
    }

    @FXML
    public void onSearch() {
        lblMsg.setText("");
        loadPage(0);
    }

    @FXML
    public void onPrev() {
        if (page > 0) loadPage(page - 1);
    }

    @FXML
    public void onNext() {
        if (page + 1 < totalPages) loadPage(page + 1);
    }

    private void loadPage(int newPage) {
        progress.setVisible(true);
        tbl.setDisable(true);
        btnPrev.setDisable(true);
        btnNext.setDisable(true);

        Integer godina = parseIntOrNull(txtGodina.getText());
        Integer broj = parseIntOrNull(txtBroj.getText());

        api.searchStudents(
                        txtIme.getText(),
                        txtPrezime.getText(),
                        txtProgram.getText(),
                        godina,
                        broj,
                        newPage,
                        size
                )
                .subscribe(
                        resp -> Platform.runLater(() -> {
                            applyPage(resp);
                            progress.setVisible(false);
                            tbl.setDisable(false);
                        }),
                        err -> Platform.runLater(() -> {
                            progress.setVisible(false);
                            tbl.setDisable(false);
                            showError("Greška pri pretrazi: " + err.getMessage());
                        })
                );
    }

    private void applyPage(PageResponse<StudentDTO> resp) {
        List<StudentDTO> content = (resp.getContent() == null) ? List.of() : resp.getContent();
        tbl.getItems().setAll(content);

        this.page = resp.getNumber();
        this.totalPages = Math.max(1, resp.getTotalPages());

        refreshPager();

        if (content.isEmpty()) {
            lblMsg.setText("Nema rezultata za date kriterijume.");
        }
    }

    private void refreshPager() {
        lblPage.setText((page + 1) + " / " + totalPages);
        btnPrev.setDisable(page <= 0);
        btnNext.setDisable(page + 1 >= totalPages);
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

    private static Integer parseIntOrNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        if (t.isBlank()) return null;
        try { return Integer.parseInt(t); } catch (Exception e) { return null; }
    }

    private static String formatIndeks(StudentDTO s) {
        if (s == null) return "";
        String prog = safe(s.getStudProgramOznaka());
        int godina = s.getGodinaUpisa();
        int yy = godina > 0 ? (godina % 100) : 0;
        int broj = s.getBroj();
        return (prog + " " + String.format("%02d", yy) + "/" + broj).trim();
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

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
