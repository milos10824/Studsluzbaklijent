package org.raflab.studsluzba.navigation;

import org.raflab.studsluzba.controllers.response.StudentIndeksResponse;
import org.raflab.studsluzba.model.dtos.StudentProfileDTO;

import java.util.Objects;

public final class Route {

    private final RouteType type;

    // SEARCH_BY_INDEX state
    private final String searchText;

    // STUDENT_PROFILE state
    private final StudentIndeksResponse indeks;
    private final StudentProfileDTO profile;
    private final StudentTab studentTab;

    private Route(RouteType type,
                  String searchText,
                  StudentIndeksResponse indeks,
                  StudentProfileDTO profile,
                  StudentTab studentTab) {
        this.type = type;
        this.searchText = searchText;
        this.indeks = indeks;
        this.profile = profile;
        this.studentTab = studentTab;
    }

    public static Route searchByIndex(String searchText) {
        return new Route(RouteType.SEARCH_BY_INDEX, searchText, null, null, null);
    }

    public static Route studentProfile(StudentIndeksResponse indeks, StudentProfileDTO profile, StudentTab tab) {
        return new Route(RouteType.STUDENT_PROFILE, null, indeks, profile, tab);
    }

    public RouteType getType() { return type; }

    public String getSearchText() { return searchText; }

    public StudentIndeksResponse getIndeks() { return indeks; }

    public StudentProfileDTO getProfile() { return profile; }

    public StudentTab getStudentTab() { return studentTab; }

    public Route withSearchText(String newText) {
        if (type != RouteType.SEARCH_BY_INDEX) return this;
        return Route.searchByIndex(newText);
    }

    public Route withStudentTab(StudentTab tab) {
        if (type != RouteType.STUDENT_PROFILE) return this;
        return Route.studentProfile(indeks, profile, tab);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Route)) return false;
        Route route = (Route) o;
        return type == route.type
                && Objects.equals(searchText, route.searchText)
                && Objects.equals(indeks, route.indeks)
                && Objects.equals(profile, route.profile)
                && studentTab == route.studentTab;
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, searchText, indeks, profile, studentTab);
    }
}
