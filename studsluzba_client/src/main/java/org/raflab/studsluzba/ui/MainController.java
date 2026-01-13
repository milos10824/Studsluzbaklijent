package org.raflab.studsluzba.ui;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.layout.StackPane;
import org.raflab.studsluzba.navigation.NavigationService;
import org.raflab.studsluzba.navigation.Route;
import org.raflab.studsluzba.navigation.RouteType;
import org.raflab.studsluzba.navigation.StudentTab;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Component
public class MainController {

    private final ApplicationContext ctx;
    private final NavigationService nav;

    public MainController(ApplicationContext ctx, NavigationService nav) {
        this.ctx = ctx;
        this.nav = nav;
    }

    @FXML private StackPane contentHost;

    @FXML
    public void initialize() {
        nav.setRenderer(this::renderRoute);
        nav.setInitial(Route.searchByIndex(null));
    }

    @FXML
    public void openSearchByIndex() {
        nav.navigate(Route.searchByIndex(null));
    }

    private void renderRoute(Route route) {
        try {
            if (route.getType() == RouteType.SEARCH_BY_INDEX) {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/SearchStudent.fxml"));
                loader.setControllerFactory(ctx::getBean);
                Parent view = loader.load();

                SearchStudentController ctrl = loader.getController();
                ctrl.setInitialIndeksText(route.getSearchText());

                showView(view);
                return;
            }

            if (route.getType() == RouteType.STUDENT_PROFILE) {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/StudentProfileTabs.fxml"));
                loader.setControllerFactory(ctx::getBean);
                Parent view = loader.load();

                StudentProfileTabsController ctrl = loader.getController();
                ctrl.setData(route.getIndeks(), route.getProfile());
                ctrl.selectTab(route.getStudentTab() != null ? route.getStudentTab() : StudentTab.LICNI);

                showView(view);
                return;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showView(Parent view) {
        contentHost.getChildren().setAll(view);
    }
}
