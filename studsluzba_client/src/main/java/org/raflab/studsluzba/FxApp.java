package org.raflab.studsluzba;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.springframework.context.ConfigurableApplicationContext;

public class FxApp extends Application {

    private static ConfigurableApplicationContext ctx;

    public static void main(String[] args) {
        ctx = ClientSpringApp.start(args);
        launch(args);
    }

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/MainWindow.fxml"));
        loader.setControllerFactory(ctx::getBean);

        stage.setTitle("Studentska služba");
        stage.setScene(new Scene(loader.load(), 1000, 700));
        stage.show();
    }

    @Override
    public void stop() {
        if (ctx != null) ctx.close();
        Platform.exit();
    }
}
