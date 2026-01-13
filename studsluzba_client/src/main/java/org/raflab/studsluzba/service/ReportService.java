package org.raflab.studsluzba.service;

import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import org.raflab.studsluzba.controllers.response.PolozenPredmetResponse;
import org.raflab.studsluzba.controllers.response.StudentIndeksResponse;
import org.raflab.studsluzba.model.dtos.StudentProfileDTO;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ReportService {

    private final DateTimeFormatter df = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    public Path generatePotvrdaStudiranja(StudentIndeksResponse indeks, StudentProfileDTO profile) throws Exception {
        InputStream jrxml = getClass().getResourceAsStream("/reports/potvrda_studiranja.jrxml");
        JasperReport report = JasperCompileManager.compileReport(jrxml);

        Map<String, Object> params = baseParams(indeks, profile);

        // potvrdа nema detaljne stavke, ali jasper traži datasource
        JRDataSource ds = new net.sf.jasperreports.engine.JREmptyDataSource(1);


        JasperPrint print = JasperFillManager.fillReport(report, params, ds);

        Path out = createOutFile("potvrda_studiranja");
        JasperExportManager.exportReportToPdfFile(print, out.toString());
        return out;

    }
    private Path createOutFile(String prefix) throws Exception {
        Path dir = Path.of("C:\\StudsluzbaPDF");
        if (!Files.exists(dir)) Files.createDirectories(dir);

        String ts = java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        return dir.resolve(prefix + "_" + ts + ".pdf");
    }

    public Path generateUverenjePolozeni(StudentIndeksResponse indeks, StudentProfileDTO profile, List<PolozenPredmetResponse> polozeni) throws Exception {
        InputStream jrxml = getClass().getResourceAsStream("/reports/uverenje_polozeni.jrxml");
        JasperReport report = JasperCompileManager.compileReport(jrxml);

        Map<String, Object> params = baseParams(indeks, profile);

        JRDataSource ds = new JRBeanCollectionDataSource(polozeni == null ? List.of() : polozeni);
        JasperPrint print = JasperFillManager.fillReport(report, params, ds);

        Path out = createOutFile("uverenje_polozeni");
        JasperExportManager.exportReportToPdfFile(print, out.toString());
        return out;

    }

    private Map<String, Object> baseParams(StudentIndeksResponse indeks, StudentProfileDTO profile) {
        Map<String, Object> p = new HashMap<>();

        String ime = indeks != null ? indeks.getImeStudenta() : "";
        String prezime = indeks != null ? indeks.getPrezimeStudenta() : "";

        if ((ime == null || ime.isBlank()) && profile != null && profile.getIndeks() != null && profile.getIndeks().getStudent() != null) {
            ime = profile.getIndeks().getStudent().getIme();
            prezime = profile.getIndeks().getStudent().getPrezime();
        }

        p.put("imePrezime", (safe(ime) + " " + safe(prezime)).trim());
        p.put("indeks", formatIndeks(indeks));
        p.put("program", indeks != null ? safe(indeks.getStudijskiProgramNaziv()) : "");
        p.put("datum", df.format(LocalDate.now()));
        p.put("espb", indeks != null && indeks.getOstvarenoEspb() != null ? indeks.getOstvarenoEspb() : 0);

        return p;
    }

    private static String safe(String s) { return s == null ? "" : s; }

    private static String formatIndeks(StudentIndeksResponse indeksRes) {
        if (indeksRes == null) return "";
        String program = safe(indeksRes.getStudProgramOznaka());
        int godina = indeksRes.getGodina();     // int (nije nullable)
        int yy = godina > 0 ? (godina % 100) : 0;
        Integer broj = indeksRes.getBroj();     // ovo ti je verovatno Integer
        return (program + " " + String.format("%02d", yy) + "/" + (broj == null ? "" : broj)).trim();
    }

}
