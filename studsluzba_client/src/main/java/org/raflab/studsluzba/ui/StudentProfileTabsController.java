package org.raflab.studsluzba.ui;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import org.raflab.studsluzba.controllers.response.*;
import org.raflab.studsluzba.model.SlusaPredmet;
import org.raflab.studsluzba.model.dtos.StudentProfileDTO;
import org.raflab.studsluzba.service.PageResponse;
import org.raflab.studsluzba.service.StudentApiService;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Profil studenta organizovan u tabove:
 * - Osnovno (sa sumarnim podacima)
 * - Lični podaci
 * - Nepoloženi (predmeti koje sluša / nepoloženi)
 * - Položeni
 * - Uplate
 * - Tok studija (upisi i obnove)
 */
@Component
public class StudentProfileTabsController {

    private final StudentApiService api;

    public StudentProfileTabsController(StudentApiService api) {
        this.api = api;
    }

    // Header
    @FXML private Label lblHeader;
    @FXML private Label lblIndeks;
    @FXML private Label lblProgram;
    @FXML private Label lblEspb;
    @FXML private Label lblProsek;

    // Tabs
    @FXML private Tab tabLicni;
    @FXML private Tab tabNepolozeni;
    @FXML private Tab tabPolozeni;
    @FXML private Tab tabUplate;
    @FXML private Tab tabTok;

    // Lični podaci
    @FXML private ProgressIndicator piLicni;
    @FXML private VBox boxLicni;
    @FXML private Label lblIme;
    @FXML private Label lblPrezime;
    @FXML private Label lblEmail;
    @FXML private Label lblJmbg;
    @FXML private Label lblDatumRodjenja;
    @FXML private Label lblAdresa;
    @FXML private Label lblTelefon;

    // Nepoloženi
    @FXML private ProgressIndicator piNepolozeni;
    @FXML private TableView<SlusaPredmet> tblNepolozeni;
    @FXML private TableColumn<SlusaPredmet, String> colNepPredmet;
    @FXML private TableColumn<SlusaPredmet, String> colNepEspb;
    @FXML private TableColumn<SlusaPredmet, String> colNepNastavnik;
    @FXML private TableColumn<SlusaPredmet, String> colNepSkGod;

    // Položeni
    @FXML private ProgressIndicator piPolozeni;
    @FXML private TableView<PolozenPredmetResponse> tblPolozeni;
    @FXML private TableColumn<PolozenPredmetResponse, String> colPolPredmet;
    @FXML private TableColumn<PolozenPredmetResponse, String> colPolOcena;
    @FXML private TableColumn<PolozenPredmetResponse, String> colPolEspb;
    @FXML private TableColumn<PolozenPredmetResponse, String> colPolDatum;
    @FXML private TableColumn<PolozenPredmetResponse, String> colPolPriznat;
    @FXML private Button btnPrevPol;
    @FXML private Button btnNextPol;
    @FXML private Label lblPolPage;

    // Uplate
    @FXML private ProgressIndicator piUplate;
    @FXML private TableView<UplataResponse> tblUplate;
    @FXML private TableColumn<UplataResponse, String> colUplDatum;
    @FXML private TableColumn<UplataResponse, String> colUplIznos;
    @FXML private TableColumn<UplataResponse, String> colUplKurs;

    // Tok studija
    @FXML private ProgressIndicator piTok;
    @FXML private TableView<UpisGodineResponse> tblUpisi;
    @FXML private TableColumn<UpisGodineResponse, String> colUpSkGod;
    @FXML private TableColumn<UpisGodineResponse, String> colUpGodStud;
    @FXML private TableColumn<UpisGodineResponse, String> colUpDatum;
    @FXML private TableColumn<UpisGodineResponse, String> colUpPredmeti;

    @FXML private TableView<ObnovaGodineResponse> tblObnove;
    @FXML private TableColumn<ObnovaGodineResponse, String> colObSkGod;
    @FXML private TableColumn<ObnovaGodineResponse, String> colObGodStud;
    @FXML private TableColumn<ObnovaGodineResponse, String> colObDatum;
    @FXML private TableColumn<ObnovaGodineResponse, String> colObPredmeti;

    private StudentIndeksResponse indeksRes;
    private StudentProfileDTO profile;

    private boolean licniLoaded = false;
    private boolean nepolozeniLoaded = false;
    private boolean polozeniLoaded = false;
    private boolean uplateLoaded = false;
    private boolean tokLoaded = false;

    private int polPage = 0;
    private int polSize = 50; // dovoljno veliko za većinu slučajeva
    private int polTotalPages = 1;

    private final DateTimeFormatter df = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    @FXML
    public void initialize() {
        setupNepolozeniTable();
        setupPolozeniTable();
        setupUplateTable();
        setupTokTables();

        tabLicni.setOnSelectionChanged(e -> {
            if (tabLicni.isSelected() && !licniLoaded) loadLicniPodaci();
        });
        tabNepolozeni.setOnSelectionChanged(e -> {
            if (tabNepolozeni.isSelected() && !nepolozeniLoaded) loadNepolozeni();
        });
        tabPolozeni.setOnSelectionChanged(e -> {
            if (tabPolozeni.isSelected() && !polozeniLoaded) loadPolozeni(0);
        });
        tabUplate.setOnSelectionChanged(e -> {
            if (tabUplate.isSelected() && !uplateLoaded) loadUplate();
        });
        tabTok.setOnSelectionChanged(e -> {
            if (tabTok.isSelected() && !tokLoaded) loadTokStudija();
        });
    }

    public void setData(StudentIndeksResponse indeksRes, StudentProfileDTO profile) {
        this.indeksRes = indeksRes;
        this.profile = profile;

        // Ime/Prezime: prvo iz fastsearch response (imeStudenta/prezimeStudenta),
        // a fallback iz profila ako treba
        String ime = null;
        String prezime = null;

        if (indeksRes != null) {
            try {
                // nova polja sa servera
                ime = indeksRes.getImeStudenta();
                prezime = indeksRes.getPrezimeStudenta();
            } catch (Exception ignore) {
                // ako si još na starom jar-u, ignoriši (ali posle clean install neće trebati)
            }
        }

        if ((ime == null || prezime == null) && profile != null && profile.getIndeks() != null
                && profile.getIndeks().getStudent() != null) {
            if (ime == null) ime = profile.getIndeks().getStudent().getIme();
            if (prezime == null) prezime = profile.getIndeks().getStudent().getPrezime();
        }

        String imePrezime = ((ime == null ? "" : ime) + " " + (prezime == null ? "" : prezime)).trim();
        if (imePrezime.isBlank()) imePrezime = "(nepoznato)";

        lblHeader.setText("Profil: " + imePrezime);

        // indeks prikaz: RN 24/1
        int godina = indeksRes != null ? indeksRes.getGodina() : 0;
        int yy = godina > 0 ? (godina % 100) : 0;
        String program = indeksRes != null ? safe(indeksRes.getStudProgramOznaka()) : "";
        Integer broj = indeksRes != null ? indeksRes.getBroj() : null;
        String indeksStr = program + " " + yy + "/" + (broj == null ? "" : broj);
        lblIndeks.setText(indeksStr.trim());

        // Program (možeš i studijskiProgramNaziv ako želiš)
        lblProgram.setText(indeksRes != null ? safe(indeksRes.getStudProgramOznaka()) : "");

        Integer espb = indeksRes != null ? indeksRes.getOstvarenoEspb() : null;
        lblEspb.setText(String.valueOf(espb == null ? 0 : espb));

        lblProsek.setText("—");

        Platform.runLater(() -> {
            if (!licniLoaded) tabLicni.getTabPane().getSelectionModel().select(tabLicni);
        });
    }


    private void setupNepolozeniTable() {
        colNepPredmet.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue() != null && c.getValue().getDrziPredmet() != null && c.getValue().getDrziPredmet().getPredmet() != null
                        ? safe(c.getValue().getDrziPredmet().getPredmet().getNaziv())
                        : ""));
        colNepEspb.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue() != null && c.getValue().getDrziPredmet() != null && c.getValue().getDrziPredmet().getPredmet() != null
                        ? String.valueOf(c.getValue().getDrziPredmet().getPredmet().getEspb())
                        : ""));
        colNepNastavnik.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue() != null && c.getValue().getDrziPredmet() != null && c.getValue().getDrziPredmet().getNastavnik() != null
                        ? (safe(c.getValue().getDrziPredmet().getNastavnik().getIme()) + " " + safe(c.getValue().getDrziPredmet().getNastavnik().getPrezime())).trim()
                        : ""));
        colNepSkGod.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue() != null && c.getValue().getDrziPredmet() != null && c.getValue().getDrziPredmet().getSkolskaGodina() != null
                        ? safe(c.getValue().getDrziPredmet().getSkolskaGodina().getOznaka())
                        : ""));
    }

    private void setupPolozeniTable() {
        colPolPredmet.setCellValueFactory(c -> new SimpleStringProperty(safe(c.getValue().getPredmetNaziv())));
        colPolOcena.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getOcena() == null ? "" : String.valueOf(c.getValue().getOcena())));
        colPolEspb.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getEspb() == null ? "" : String.valueOf(c.getValue().getEspb())));
        colPolDatum.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getDatumPolaganja() == null ? "" : df.format(c.getValue().getDatumPolaganja())));
        colPolPriznat.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().isPriznat() ? "DA" : "NE"));

        btnPrevPol.setOnAction(e -> {
            if (polPage > 0) loadPolozeni(polPage - 1);
        });
        btnNextPol.setOnAction(e -> {
            if (polPage + 1 < polTotalPages) loadPolozeni(polPage + 1);
        });
    }

    private void setupUplateTable() {
        colUplDatum.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getDatumUplate() == null ? "" : df.format(c.getValue().getDatumUplate())));
        colUplIznos.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getIznosRsd() == null ? "" : String.format("%.2f", c.getValue().getIznosRsd())));
        colUplKurs.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getSrednjiKurs() == null ? "" : String.format("%.4f", c.getValue().getSrednjiKurs())));
    }

    private void setupTokTables() {
        colUpSkGod.setCellValueFactory(c -> new SimpleStringProperty(safe(c.getValue().getSkolskaGodinaNaziv())));
        colUpGodStud.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getGodinaStudija() == null ? "" : String.valueOf(c.getValue().getGodinaStudija())));
        colUpDatum.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getDatumUpisa() == null ? "" : df.format(c.getValue().getDatumUpisa())));
        colUpPredmeti.setCellValueFactory(c -> new SimpleStringProperty(joinList(c.getValue().getPredmeti())));

        colObSkGod.setCellValueFactory(c -> new SimpleStringProperty(safe(c.getValue().getSkolskaGodinaNaziv())));
        colObGodStud.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getGodinaStudija() == null ? "" : String.valueOf(c.getValue().getGodinaStudija())));
        colObDatum.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getDatumObnove() == null ? "" : df.format(c.getValue().getDatumObnove())));
        colObPredmeti.setCellValueFactory(c -> new SimpleStringProperty(joinList(c.getValue().getPredmeti())));
    }

    private void loadLicniPodaci() {
        licniLoaded = true;
        piLicni.setVisible(true);
        boxLicni.setDisable(true);

        Long studentId = indeksRes != null ? indeksRes.getId(): null;
        if (studentId == null) {
            showError("Ne mogu da učitam lične podatke: studentId nedostaje.");
            piLicni.setVisible(false);
            boxLicni.setDisable(false);
            return;
        }

        api.getStudentPodaci(studentId)
                .subscribe(
                        data -> Platform.runLater(() -> {
                            fillLicni(data);
                            piLicni.setVisible(false);
                            boxLicni.setDisable(false);
                        }),
                        err -> Platform.runLater(() -> {
                            piLicni.setVisible(false);
                            boxLicni.setDisable(false);
                            showError("Greška pri učitavanju ličnih podataka: " + err.getMessage());
                        })
                );
    }

    private void fillLicni(StudentPodaciResponse d) {
        lblIme.setText(safe(d.getIme()));
        lblPrezime.setText(safe(d.getPrezime()));
        lblEmail.setText(safe(d.getEmail()));
        lblJmbg.setText(safe(d.getJmbg()));
        lblDatumRodjenja.setText(d.getDatumRodjenja() == null ? "" : df.format(d.getDatumRodjenja()));
        lblAdresa.setText(safe(d.getAdresa()));

        String tel = "";
        if (d.getBrojTelefonaMobilni() != null) tel += d.getBrojTelefonaMobilni();
        if (d.getBrojTelefonaFiksni() != null && !d.getBrojTelefonaFiksni().isBlank()) {
            if (!tel.isBlank()) tel += " / ";
            tel += d.getBrojTelefonaFiksni();
        }
        lblTelefon.setText(tel);
    }

    private void loadNepolozeni() {
        nepolozeniLoaded = true;
        piNepolozeni.setVisible(true);
        tblNepolozeni.setDisable(true);

        List<SlusaPredmet> list = profile != null ? profile.getNepolozeniPredmeti() : null;
        Platform.runLater(() -> {
            tblNepolozeni.getItems().setAll(list == null ? List.of() : list);
            piNepolozeni.setVisible(false);
            tblNepolozeni.setDisable(false);
        });
    }

    private void loadPolozeni(int page) {
        polozeniLoaded = true;
        piPolozeni.setVisible(true);
        tblPolozeni.setDisable(true);
        btnPrevPol.setDisable(true);
        btnNextPol.setDisable(true);

        Long indeksId = indeksRes != null ? indeksRes.getId() : null;
        if (indeksId == null) {
            showError("Ne mogu da učitam položene: indeksId nedostaje.");
            piPolozeni.setVisible(false);
            tblPolozeni.setDisable(false);
            return;
        }

        api.getPolozeni(indeksId, page, polSize)
                .subscribe(
                        resp -> Platform.runLater(() -> {
                            polPage = resp.getNumber();
                            polTotalPages = Math.max(1, resp.getTotalPages());
                            tblPolozeni.getItems().setAll(resp.getContent() == null ? List.of() : resp.getContent());
                            lblPolPage.setText((polPage + 1) + " / " + polTotalPages);
                            btnPrevPol.setDisable(polPage <= 0);
                            btnNextPol.setDisable(polPage + 1 >= polTotalPages);
                            piPolozeni.setVisible(false);
                            tblPolozeni.setDisable(false);

                            // Izračunaj prosek (na osnovu učitanog sadržaja). Ako ima više strana,
                            // prosek će biti približan. U praksi je često dovoljno, jer je polSize velik.
                            computeAndShowProsek(resp.getContent());
                        }),
                        err -> Platform.runLater(() -> {
                            piPolozeni.setVisible(false);
                            tblPolozeni.setDisable(false);
                            showError("Greška pri učitavanju položenih: " + err.getMessage());
                        })
                );
    }

    private void computeAndShowProsek(List<PolozenPredmetResponse> polozeni) {
        if (polozeni == null || polozeni.isEmpty()) {
            lblProsek.setText("—");
            return;
        }

        // ponderisan ESPB-om (ako fali ESPB, tretiramo kao 0)
        double sumW = 0;
        double sum = 0;
        for (PolozenPredmetResponse p : polozeni) {
            if (p.getOcena() == null) continue;
            int espb = p.getEspb() == null ? 0 : p.getEspb();
            if (espb <= 0) espb = 1; // fallback
            sumW += espb;
            sum += p.getOcena() * espb;
        }
        if (sumW <= 0) {
            lblProsek.setText("—");
            return;
        }
        double avg = sum / sumW;
        lblProsek.setText(String.format("%.2f", avg));
    }

    private void loadUplate() {
        uplateLoaded = true;
        piUplate.setVisible(true);
        tblUplate.setDisable(true);

        Long indeksId = indeksRes != null ? indeksRes.getId() : null;
        if (indeksId == null) {
            showError("Ne mogu da učitam uplate: indeksId nedostaje.");
            piUplate.setVisible(false);
            tblUplate.setDisable(false);
            return;
        }

        api.getUplate(indeksId)
                .subscribe(
                        list -> Platform.runLater(() -> {
                            tblUplate.getItems().setAll(list == null ? List.of() : list);
                            piUplate.setVisible(false);
                            tblUplate.setDisable(false);
                        }),
                        err -> Platform.runLater(() -> {
                            piUplate.setVisible(false);
                            tblUplate.setDisable(false);
                            showError("Greška pri učitavanju uplata: " + err.getMessage());
                        })
                );
    }

    private void loadTokStudija() {
        tokLoaded = true;
        piTok.setVisible(true);
        tblUpisi.setDisable(true);
        tblObnove.setDisable(true);

        Long indeksId = indeksRes != null ? indeksRes.getId() : null;
        if (indeksId == null) {
            showError("Ne mogu da učitam tok studija: indeksId nedostaje.");
            piTok.setVisible(false);
            tblUpisi.setDisable(false);
            tblObnove.setDisable(false);
            return;
        }

        Mono<List<UpisGodineResponse>> upisiM = api.getUpisi(indeksId);
        Mono<List<ObnovaGodineResponse>> obnoveM = api.getObnove(indeksId);

        Mono.zip(upisiM, obnoveM)
                .subscribe(
                        tup -> Platform.runLater(() -> {
                            List<UpisGodineResponse> upisi = tup.getT1();
                            List<ObnovaGodineResponse> obnove = tup.getT2();
                            tblUpisi.getItems().setAll(upisi == null ? List.of() : upisi);
                            tblObnove.getItems().setAll(obnove == null ? List.of() : obnove);
                            piTok.setVisible(false);
                            tblUpisi.setDisable(false);
                            tblObnove.setDisable(false);
                        }),
                        err -> Platform.runLater(() -> {
                            piTok.setVisible(false);
                            tblUpisi.setDisable(false);
                            tblObnove.setDisable(false);
                            showError("Greška pri učitavanju toka studija: " + err.getMessage());
                        })
                );
    }

    private void showError(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle("Greška");
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    private static String joinList(List<String> xs) {
        if (xs == null || xs.isEmpty()) return "";
        return xs.stream().filter(Objects::nonNull).collect(Collectors.joining(", "));
    }
}
