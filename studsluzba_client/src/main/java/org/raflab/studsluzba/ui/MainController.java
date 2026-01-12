package org.raflab.studsluzba.ui;



import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.layout.StackPane;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Component
public class MainController {

    private final ApplicationContext ctx;

    public MainController(ApplicationContext ctx) {
        this.ctx = ctx;
    }

    @FXML private StackPane contentHost;
    @FXML
    public void initialize() {
        openSearchByIndex();
    }

    @FXML
    public void openSearchByIndex() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/SearchStudent.fxml"));
            loader.setControllerFactory(ctx::getBean);
            Parent view = loader.load();
            contentHost.getChildren().setAll(view);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void showView(Parent view) {
        contentHost.getChildren().setAll(view);
    }
}
