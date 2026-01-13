package org.raflab.studsluzba;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import org.raflab.studsluzba.navigation.NavigationService;
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

        Scene scene = new Scene(loader.load(), 1000, 700);

        final NavigationService nav = ctx.getBean(NavigationService.class);

        // Mouse back/forward (buttons 4/5)
        scene.addEventFilter(MouseEvent.MOUSE_PRESSED, e -> {
            if (e.getButton() == MouseButton.BACK) {
                nav.back();
                e.consume();
            } else if (e.getButton() == MouseButton.FORWARD) {
                nav.forward();
                e.consume();
            }
        });

        // Ctrl + [  and Ctrl + ]
        scene.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (e.isControlDown() && e.getCode() == KeyCode.OPEN_BRACKET) {
                nav.back();
                e.consume();
            } else if (e.isControlDown() && e.getCode() == KeyCode.CLOSE_BRACKET) {
                nav.forward();
                e.consume();
            }
        });

        stage.setTitle("Studentska služba");
        stage.setScene(scene);
        stage.show();
    }

    @Override
    public void stop() {
        if (ctx != null) ctx.close();
        Platform.exit();
    }
}
