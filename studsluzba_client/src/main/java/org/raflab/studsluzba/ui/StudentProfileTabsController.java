package org.raflab.studsluzba.ui;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import org.raflab.studsluzba.controllers.request.ObnovaGodineRequest;
import org.raflab.studsluzba.controllers.request.UpisGodineRequest;
import org.raflab.studsluzba.controllers.response.*;
import org.raflab.studsluzba.model.SlusaPredmet;
import org.raflab.studsluzba.model.dtos.StudentProfileDTO;
import org.raflab.studsluzba.navigation.NavigationService;
import org.raflab.studsluzba.navigation.Route;
import org.raflab.studsluzba.navigation.StudentTab;
import org.raflab.studsluzba.service.ReportService;
import org.raflab.studsluzba.service.StudentApiService;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
public class StudentProfileTabsController {

    private final StudentApiService api;
    private final NavigationService nav;
    private final ReportService reportService;


    public StudentProfileTabsController(StudentApiService api, NavigationService nav, ReportService reportService) {
        this.api = api;
        this.nav = nav;
        this.reportService = reportService;
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
    @FXML private Button btnNovaUplata;

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
    @FXML private Button btnUpisiGodinu;
    @FXML private Button btnObnoviGodinu;


    private StudentIndeksResponse indeksRes;
    private StudentProfileDTO profile;

    private boolean licniLoaded;
    private boolean nepolozeniLoaded;
    private boolean polozeniLoaded;
    private boolean uplateLoaded;
    private boolean tokLoaded;

    private int polPage = 0;
    private final int polSize = 50;
    private int polTotalPages = 1;

    private final DateTimeFormatter df = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    @FXML
    public void onPotvrdaStudiranja() {
        try {
            var pdf = reportService.generatePotvrdaStudiranja(indeksRes, profile);
            openFile(pdf);
        } catch (Exception e) {
            showError("Ne mogu da generišem potvrdu: " + e.getMessage());
        }
    }

    @FXML
    public void onUverenjePolozeni() {
        Long indeksId = indeksRes != null ? indeksRes.getId() : null;
        if (indeksId == null) { showError("Nema indeksId."); return; }

        api.getPolozeni(indeksId, 0, 1000).subscribe(
                page -> Platform.runLater(() -> {
                    try {
                        var pdf = reportService.generateUverenjePolozeni(indeksRes, profile, page.getContent());
                        openFile(pdf);
                    } catch (Exception e) {
                        showError("Ne mogu da generišem uverenje: " + e.getMessage());
                    }
                }),
                err -> Platform.runLater(() -> showError("Ne mogu da učitam položene: " + err.getMessage()))
        );
    }
    private void openFile(java.nio.file.Path path) {
        try {
            if (java.awt.Desktop.isDesktopSupported()) {
                java.awt.Desktop.getDesktop().open(path.toFile());
            } else {
                showInfo("PDF je sačuvan na:\n" + path);
            }
        } catch (Exception e) {
            showInfo("PDF je sačuvan na:\n" + path);
        }
    }


    // da ne guramo history dok programatski biramo tab (restore/back/forward)
    private boolean ignoreHistory = false;
    @FXML
    public void onNovaUplata() {
        Long indeksId = indeksRes != null ? indeksRes.getId() : null;
        if (indeksId == null) {
            showError("Ne mogu da unesem uplatu: indeksId nedostaje.");
            return;
        }

        TextInputDialog d = new TextInputDialog();
        d.setTitle("Nova uplata");
        d.setHeaderText("Unesite iznos uplate u RSD");
        d.setContentText("Iznos (RSD):");

        var opt = d.showAndWait();
        if (opt.isEmpty()) return;

        Double iznos = parseRsd(opt.get());
        if (iznos == null || iznos <= 0) {
            showError("Iznos mora biti pozitivan broj (npr. 15000 ili 15000,50).");
            return;
        }

        piUplate.setVisible(true);
        tblUplate.setDisable(true);
        btnNovaUplata.setDisable(true);

        api.addUplata(indeksId, iznos)
                .then(api.getUplate(indeksId)) // odmah posle unosa povuci sve uplate ponovo
                .subscribe(
                        list -> Platform.runLater(() -> {
                            tblUplate.getItems().setAll(list == null ? List.of() : list);
                            piUplate.setVisible(false);
                            tblUplate.setDisable(false);
                            btnNovaUplata.setDisable(false);
                            showInfo("Uplata je uspešno sačuvana.");
                        }),
                        err -> Platform.runLater(() -> {
                            piUplate.setVisible(false);
                            tblUplate.setDisable(false);
                            btnNovaUplata.setDisable(false);
                            showError("Greška pri unosu uplate: " + err.getMessage());
                        })
                );
    }

    @FXML
    public void initialize() {
        setupNepolozeniTable();
        setupPolozeniTable();
        setupUplateTable();
        setupTokTables();

        // 1) jedinstvena logika: kad se promeni selektovan tab -> load (ako treba) + history entry
        hookTab(tabLicni, StudentTab.LICNI);
        hookTab(tabNepolozeni, StudentTab.NEPOLOZENI);
        hookTab(tabPolozeni, StudentTab.POLOZENI);
        hookTab(tabUplate, StudentTab.UPLATE);
        hookTab(tabTok, StudentTab.TOK);

        // 2) osiguraj da se inicijalni selektovani tab učita i kad JavaFX ne okine event
        Platform.runLater(this::ensureSelectedTabLoaded);
    }

    private void hookTab(Tab tab, StudentTab studentTab) {
        tab.setOnSelectionChanged(e -> {
            if (!tab.isSelected()) return;

            // load sadržaja (ne zavisi od history)
            if (tab == tabLicni && !licniLoaded) loadLicniPodaci();
            else if (tab == tabNepolozeni && !nepolozeniLoaded) loadNepolozeni();
            else if (tab == tabPolozeni && !polozeniLoaded) loadPolozeni(0);
            else if (tab == tabUplate && !uplateLoaded) loadUplate();
            else if (tab == tabTok && !tokLoaded) loadTokStudija();

            // history (mora i za tabove)
            pushHistory(studentTab);
        });
    }
    @FXML
    public void onUpisiGodinu() {
        if (indeksRes == null) {
            showError("Nema indeksa – ne mogu da upišem godinu.");
            return;
        }

        piTok.setVisible(true);
        btnUpisiGodinu.setDisable(true);
        btnObnoviGodinu.setDisable(true);

        api.getSkolskeGodine().subscribe(
                godine -> Platform.runLater(() -> {
                    piTok.setVisible(false);
                    btnUpisiGodinu.setDisable(false);
                    btnObnoviGodinu.setDisable(false);

                    UpisGodineRequest req = showUpisDialog(godine);
                    if (req == null) return;

                    piTok.setVisible(true);
                    btnUpisiGodinu.setDisable(true);
                    btnObnoviGodinu.setDisable(true);

                    api.addUpisGodine(req)
                            .then(Mono.fromRunnable(this::loadTokStudija))
                            .subscribe(
                                    ok -> {},
                                    err -> Platform.runLater(() -> {
                                        piTok.setVisible(false);
                                        btnUpisiGodinu.setDisable(false);
                                        btnObnoviGodinu.setDisable(false);
                                        showError("Greška pri upisu godine: " + err.getMessage());
                                    })
                            );
                }),
                err -> Platform.runLater(() -> {
                    piTok.setVisible(false);
                    btnUpisiGodinu.setDisable(false);
                    btnObnoviGodinu.setDisable(false);
                    showError("Ne mogu da učitam školske godine: " + err.getMessage());
                })
        );
    }
    @FXML
    public void onObnoviGodinu() {
        if (indeksRes == null) {
            showError("Nema indeksa – ne mogu da obnovim godinu.");
            return;
        }

        piTok.setVisible(true);
        btnUpisiGodinu.setDisable(true);
        btnObnoviGodinu.setDisable(true);

        api.getSkolskeGodine().subscribe(
                godine -> Platform.runLater(() -> {
                    piTok.setVisible(false);
                    btnUpisiGodinu.setDisable(false);
                    btnObnoviGodinu.setDisable(false);

                    ObnovaGodineRequest req = showObnovaDialog(godine);
                    if (req == null) return;

                    piTok.setVisible(true);
                    btnUpisiGodinu.setDisable(true);
                    btnObnoviGodinu.setDisable(true);

                    api.addObnovaGodine(req)
                            .then(Mono.fromRunnable(this::loadTokStudija))
                            .subscribe(
                                    ok -> {},
                                    err -> Platform.runLater(() -> {
                                        piTok.setVisible(false);
                                        btnUpisiGodinu.setDisable(false);
                                        btnObnoviGodinu.setDisable(false);
                                        showError("Greška pri obnovi godine: " + err.getMessage());
                                    })
                            );
                }),
                err -> Platform.runLater(() -> {
                    piTok.setVisible(false);
                    btnUpisiGodinu.setDisable(false);
                    btnObnoviGodinu.setDisable(false);
                    showError("Ne mogu da učitam školske godine: " + err.getMessage());
                })
        );
    }
    private UpisGodineRequest showUpisDialog(List<SkolskaGodinaResponse> godine) {
        Dialog<UpisGodineRequest> dialog = new Dialog<>();
        dialog.setTitle("Upis godine");

        ButtonType okType = new ButtonType("Sačuvaj", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(okType, ButtonType.CANCEL);

        ComboBox<SkolskaGodinaResponse> cmbGodina = new ComboBox<>();
        cmbGodina.getItems().setAll(godine == null ? List.of() : godine);
        cmbGodina.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(SkolskaGodinaResponse item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.getOznaka());
            }
        });
        cmbGodina.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(SkolskaGodinaResponse item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.getOznaka());
            }
        });

        // izaberi aktivnu ako postoji
        if (godine != null) {
            godine.stream().filter(SkolskaGodinaResponse::isAktivna).findFirst().ifPresent(cmbGodina::setValue);
        }

        Spinner<Integer> spGodStud = new Spinner<>(1, 4, 1);
        spGodStud.setEditable(false);

        TextArea txtNapomena = new TextArea();
        txtNapomena.setPromptText("Napomena (opciono)");
        txtNapomena.setPrefRowCount(2);

        ListView<DrziPredmetResponse> lvPredmeti = new ListView<>();
        lvPredmeti.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        Label lblEspb = new Label("Ukupno ESPB: 0");
        ProgressIndicator pi = new ProgressIndicator();
        pi.setVisible(false);
        pi.setPrefSize(24, 24);

        // layout
        GridPane gp = new GridPane();
        gp.setHgap(10);
        gp.setVgap(10);

        gp.addRow(0, new Label("Školska godina:"), cmbGodina, pi);
        gp.addRow(1, new Label("Godina studija:"), spGodStud);
        gp.addRow(2, new Label("Predmeti:"), lvPredmeti);
        gp.addRow(3, new Label(""), lblEspb);
        gp.addRow(4, new Label("Napomena:"), txtNapomena);

        dialog.getDialogPane().setContent(gp);
        lvPredmeti.setPrefHeight(260);

        lvPredmeti.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(DrziPredmetResponse item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(""); return; }
                String naziv = item.getPredmetNaziv();
                Integer espb = item.getEspb();
                String nastavnik = ((item.getNastavnikIme() == null ? "" : item.getNastavnikIme()) + " "
                        + (item.getNastavnikPrezime() == null ? "" : item.getNastavnikPrezime())).trim();
                setText(naziv + (espb != null ? (" (" + espb + " ESPB)") : "") + (nastavnik.isBlank() ? "" : (" — " + nastavnik)));
            }
        });

        Runnable recompute = () -> {
            int sum = 0;
            for (DrziPredmetResponse dp : lvPredmeti.getSelectionModel().getSelectedItems()) {
                if (dp.getEspb() != null) sum += dp.getEspb();
            }
            lblEspb.setText("Ukupno ESPB: " + sum);
        };
        lvPredmeti.getSelectionModel().getSelectedItems().addListener((javafx.collections.ListChangeListener<? super DrziPredmetResponse>) c -> recompute.run());

        // kad se promeni šk. godina ili godina studija -> povuci drziPredmet i filtriraj po semestru
        Runnable reloadPredmeti = () -> {
            SkolskaGodinaResponse sk = cmbGodina.getValue();
            if (sk == null || sk.getId() == null) {
                lvPredmeti.getItems().clear();
                recompute.run();
                return;
            }
            String program = indeksRes.getStudProgramOznaka();
            int godinaStud = spGodStud.getValue();

            pi.setVisible(true);
            api.getDrziPredmet(sk.getId(), program).subscribe(
                    list -> Platform.runLater(() -> {
                        pi.setVisible(false);
                        lvPredmeti.getItems().setAll(filterByGodinaStudija(list, godinaStud));
                        lvPredmeti.getSelectionModel().clearSelection();
                        recompute.run();
                    }),
                    err -> Platform.runLater(() -> {
                        pi.setVisible(false);
                        showError("Ne mogu da učitam predmete: " + err.getMessage());
                    })
            );
        };

        cmbGodina.valueProperty().addListener((obs, o, n) -> reloadPredmeti.run());
        spGodStud.valueProperty().addListener((obs, o, n) -> reloadPredmeti.run());

        // inicijalno učitaj
        Platform.runLater(reloadPredmeti);

        dialog.setResultConverter(bt -> {
            if (bt != okType) return null;

            SkolskaGodinaResponse sk = cmbGodina.getValue();
            if (sk == null || sk.getId() == null) {
                showError("Izaberi školsku godinu.");
                return null;
            }

            var selected = lvPredmeti.getSelectionModel().getSelectedItems();
            if (selected == null || selected.isEmpty()) {
                showError("Izaberi bar jedan predmet.");
                return null;
            }

            UpisGodineRequest req = new UpisGodineRequest();
            req.setStudentIndeksId(indeksRes.getId());
            req.setSkolskaGodinaId(sk.getId());
            req.setGodinaStudija(spGodStud.getValue());
            req.setNapomena(txtNapomena.getText());

            req.setDrziPredmetIds(
                    selected.stream()
                            .map(DrziPredmetResponse::getId)
                            .collect(Collectors.toList())
            );

            return req;
        });

        return dialog.showAndWait().orElse(null);
    }
    private ObnovaGodineRequest showObnovaDialog(List<SkolskaGodinaResponse> godine) {
        Dialog<ObnovaGodineRequest> dialog = new Dialog<>();
        dialog.setTitle("Obnova godine");

        ButtonType okType = new ButtonType("Sačuvaj", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(okType, ButtonType.CANCEL);

        ComboBox<SkolskaGodinaResponse> cmbGodina = new ComboBox<>();
        cmbGodina.getItems().setAll(godine == null ? List.of() : godine);
        cmbGodina.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(SkolskaGodinaResponse item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.getOznaka());
            }
        });
        cmbGodina.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(SkolskaGodinaResponse item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.getOznaka());
            }
        });

        if (godine != null) {
            godine.stream().filter(SkolskaGodinaResponse::isAktivna).findFirst().ifPresent(cmbGodina::setValue);
        }

        Spinner<Integer> spGodStud = new Spinner<>(1, 4, 1);

        TextArea txtNapomena = new TextArea();
        txtNapomena.setPromptText("Napomena (opciono)");
        txtNapomena.setPrefRowCount(2);

        ListView<DrziPredmetResponse> lvPredmeti = new ListView<>();
        lvPredmeti.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        Label lblEspb = new Label("Ukupno ESPB: 0 (max 60)");
        ProgressIndicator pi = new ProgressIndicator();
        pi.setVisible(false);
        pi.setPrefSize(24, 24);

        GridPane gp = new GridPane();
        gp.setHgap(10);
        gp.setVgap(10);
        gp.addRow(0, new Label("Školska godina:"), cmbGodina, pi);
        gp.addRow(1, new Label("Godina studija:"), spGodStud);
        gp.addRow(2, new Label("Predmeti:"), lvPredmeti);
        gp.addRow(3, new Label(""), lblEspb);
        gp.addRow(4, new Label("Napomena:"), txtNapomena);

        dialog.getDialogPane().setContent(gp);
        lvPredmeti.setPrefHeight(260);

        lvPredmeti.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(DrziPredmetResponse item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(""); return; }
                String naziv = item.getPredmetNaziv();
                Integer espb = item.getEspb();
                setText(naziv + (espb != null ? (" (" + espb + " ESPB)") : ""));
            }
        });

        Runnable recompute = () -> {
            int sum = 0;
            for (DrziPredmetResponse dp : lvPredmeti.getSelectionModel().getSelectedItems()) {
                if (dp.getEspb() != null) sum += dp.getEspb();
            }
            lblEspb.setText("Ukupno ESPB: " + sum + " (max 60)");
        };
        lvPredmeti.getSelectionModel().getSelectedItems().addListener((javafx.collections.ListChangeListener<? super DrziPredmetResponse>) c -> recompute.run());

        Runnable reloadPredmeti = () -> {
            SkolskaGodinaResponse sk = cmbGodina.getValue();
            if (sk == null || sk.getId() == null) {
                lvPredmeti.getItems().clear();
                recompute.run();
                return;
            }
            String program = indeksRes.getStudProgramOznaka();
            int godinaStud = spGodStud.getValue();

            pi.setVisible(true);
            api.getDrziPredmet(sk.getId(), program).subscribe(
                    list -> Platform.runLater(() -> {
                        pi.setVisible(false);
                        lvPredmeti.getItems().setAll(filterByGodinaStudija(list, godinaStud));
                        lvPredmeti.getSelectionModel().clearSelection();
                        recompute.run();
                    }),
                    err -> Platform.runLater(() -> {
                        pi.setVisible(false);
                        showError("Ne mogu da učitam predmete: " + err.getMessage());
                    })
            );
        };

        cmbGodina.valueProperty().addListener((obs, o, n) -> reloadPredmeti.run());
        spGodStud.valueProperty().addListener((obs, o, n) -> reloadPredmeti.run());
        Platform.runLater(reloadPredmeti);

        dialog.setResultConverter(bt -> {
            if (bt != okType) return null;

            SkolskaGodinaResponse sk = cmbGodina.getValue();
            if (sk == null || sk.getId() == null) {
                showError("Izaberi školsku godinu.");
                return null;
            }

            var selected = lvPredmeti.getSelectionModel().getSelectedItems();
            if (selected == null || selected.isEmpty()) {
                showError("Izaberi bar jedan predmet.");
                return null;
            }

            int sum = 0;
            for (DrziPredmetResponse dp : selected) if (dp.getEspb() != null) sum += dp.getEspb();
            if (sum > 60) {
                showError("Za obnovu maksimalno 60 ESPB. Trenutno: " + sum);
                return null;
            }

            ObnovaGodineRequest req = new ObnovaGodineRequest();
            req.setStudentIndeksId(indeksRes.getId());
            req.setSkolskaGodinaId(sk.getId());
            req.setGodinaStudija(spGodStud.getValue());
            req.setNapomena(txtNapomena.getText());
            req.setDrziPredmetIds(
                    selected.stream()
                            .map(DrziPredmetResponse::getId)
                            .collect(Collectors.toList())
            );

            return req;
        });

        return dialog.showAndWait().orElse(null);
    }
    private static List<DrziPredmetResponse> filterByGodinaStudija(List<DrziPredmetResponse> list, int godinaStudija) {
        if (list == null) return List.of();
        int minSem = (godinaStudija - 1) * 2 + 1;
        int maxSem = godinaStudija * 2;


        return list.stream()
                .filter(dp -> dp.getSemestar() != null
                        && dp.getSemestar() >= minSem
                        && dp.getSemestar() <= maxSem)
                .collect(Collectors.toList());

    }


    private void ensureSelectedTabLoaded() {
        TabPane pane = tabLicni != null ? tabLicni.getTabPane() : null;
        if (pane == null) return;

        Tab selected = pane.getSelectionModel().getSelectedItem();
        if (selected == tabLicni && !licniLoaded) loadLicniPodaci();
        else if (selected == tabNepolozeni && !nepolozeniLoaded) loadNepolozeni();
        else if (selected == tabPolozeni && !polozeniLoaded) loadPolozeni(0);
        else if (selected == tabUplate && !uplateLoaded) loadUplate();
        else if (selected == tabTok && !tokLoaded) loadTokStudija();
    }

    public void setData(StudentIndeksResponse indeksRes, StudentProfileDTO profile) {
        this.indeksRes = indeksRes;
        this.profile = profile;

        // reset tab-state ako se ista instanca kontrolera nekad ponovo koristi
        licniLoaded = false;
        nepolozeniLoaded = false;
        polozeniLoaded = false;
        uplateLoaded = false;
        tokLoaded = false;

        // ===== header =====
        String ime = indeksRes != null ? indeksRes.getImeStudenta() : null;
        String prezime = indeksRes != null ? indeksRes.getPrezimeStudenta() : null;

        if ((ime == null || prezime == null) && profile != null
                && profile.getIndeks() != null
                && profile.getIndeks().getStudent() != null) {
            if (ime == null) ime = profile.getIndeks().getStudent().getIme();
            if (prezime == null) prezime = profile.getIndeks().getStudent().getPrezime();
        }

        String imePrezime = ((ime == null ? "" : ime) + " " + (prezime == null ? "" : prezime)).trim();
        if (imePrezime.isBlank()) imePrezime = "(nepoznato)";
        lblHeader.setText("Profil: " + imePrezime);

        String programOznaka = indeksRes != null ? safe(indeksRes.getStudProgramOznaka()) : "";
        String programNaziv = indeksRes != null ? safe(indeksRes.getStudijskiProgramNaziv()) : "";
        lblProgram.setText(!programNaziv.isBlank() ? programNaziv : programOznaka);

        int godina = indeksRes != null ? indeksRes.getGodina() : 0;
        int yy = godina > 0 ? (godina % 100) : 0;
        Integer broj = indeksRes != null ? indeksRes.getBroj() : null;
        lblIndeks.setText((programOznaka + " " + String.format("%02d", yy) + "/" + (broj == null ? "" : broj)).trim());

        Integer espb = indeksRes != null ? indeksRes.getOstvarenoEspb() : null;
        lblEspb.setText(String.valueOf(espb == null ? 0 : espb));

        lblProsek.setText("—");

        // ako je već neki tab selektovan (npr. back/forward) - učitaj ga
        Platform.runLater(this::ensureSelectedTabLoaded);
    }

    public void selectTab(StudentTab tab) {
        if (tab == null) tab = StudentTab.LICNI;

        ignoreHistory = true;
        try {
            TabPane pane = tabLicni.getTabPane();
            if (pane == null) return;

            switch (tab) {
                case LICNI:
                    pane.getSelectionModel().select(tabLicni);
                    break;
                case NEPOLOZENI:
                    pane.getSelectionModel().select(tabNepolozeni);
                    break;
                case POLOZENI:
                    pane.getSelectionModel().select(tabPolozeni);
                    break;
                case UPLATE:
                    pane.getSelectionModel().select(tabUplate);
                    break;
                case TOK:
                    pane.getSelectionModel().select(tabTok);
                    break;
                default:
                    pane.getSelectionModel().select(tabLicni);
                    break;
            }
        } finally {
            ignoreHistory = false;
        }

        // za slučaj da JavaFX ne okine selection event (ako je već bio selektovan isti tab)
        Platform.runLater(this::ensureSelectedTabLoaded);
    }

    private void pushHistory(StudentTab tab) {
        if (indeksRes == null || profile == null) return;
        if (ignoreHistory) return;


        nav.navigate(Route.studentProfile(indeksRes, profile, tab));
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

        // BITNO: endpoint /api/student/podaci/{id} očekuje StudentPodaci ID
        Long studentPodaciId = (indeksRes != null) ? indeksRes.getStudentId() : null;

        if (studentPodaciId == null) {
            showError("Ne mogu da učitam lične podatke: studentPodaciId nedostaje.");
            piLicni.setVisible(false);
            boxLicni.setDisable(false);
            return;
        }

        api.getStudentPodaci(studentPodaciId)
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

        double sumW = 0;
        double sum = 0;
        for (PolozenPredmetResponse p : polozeni) {
            if (p.getOcena() == null) continue;
            int espb = p.getEspb() == null ? 0 : p.getEspb();
            if (espb <= 0) espb = 1;
            sumW += espb;
            sum += p.getOcena() * espb;
        }

        if (sumW <= 0) {
            lblProsek.setText("—");
            return;
        }

        lblProsek.setText(String.format("%.2f", (sum / sumW)));
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
                            tblUpisi.getItems().setAll(tup.getT1() == null ? List.of() : tup.getT1());
                            tblObnove.getItems().setAll(tup.getT2() == null ? List.of() : tup.getT2());
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
    private static Double parseRsd(String s) {
        if (s == null) return null;
        String t = s.trim().replace(" ", "");
        if (t.isBlank()) return null;

        // Podrži formate: "15000", "15000,50", "15.000,50"
        if (t.contains(",") && t.contains(".")) {
            // pretpostavi "." hiljade, "," decimale
            t = t.replace(".", "").replace(",", ".");
        } else if (t.contains(",")) {
            t = t.replace(",", ".");
        }
        try {
            return Double.parseDouble(t);
        } catch (Exception e) {
            return null;
        }
    }

    private void showInfo(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("Info");
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }

    private static String joinList(List<String> xs) {
        if (xs == null || xs.isEmpty()) return "";
        return xs.stream().filter(Objects::nonNull).collect(Collectors.joining(", "));
    }
}
