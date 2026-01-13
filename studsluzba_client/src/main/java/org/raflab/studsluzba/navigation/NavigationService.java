package org.raflab.studsluzba.navigation;

import org.raflab.studsluzba.service.StudentApiService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayDeque;
import java.util.Deque;

@Service
public class NavigationService {



    public interface Renderer {
        void render(Route route);
    }

    private final int maxDepth;

    private final Deque<Route> backStack = new ArrayDeque<>();
    private final Deque<Route> forwardStack = new ArrayDeque<>();

    private Route current;
    private Renderer renderer;

    public NavigationService(@Value("${app.history.maxDepth:10}") int maxDepth) {
        this.maxDepth = Math.max(1, maxDepth);
    }

    public void setRenderer(Renderer renderer) {
        this.renderer = renderer;
    }

    public Route getCurrent() {
        return current;
    }

    public void setInitial(Route route) {
        if (route == null) return;
        backStack.clear();
        forwardStack.clear();
        current = route;
        render(route);
    }

    public void navigate(Route next) {
        if (next == null) return;

        if (current != null) {
            backStack.push(current);
            trim(backStack);
        }
        forwardStack.clear();

        current = next;
        render(next);
    }

    /** Update state of current route WITHOUT pushing history (npr. pamti text/tab). */
    public void updateCurrent(Route updated) {
        if (updated == null) return;
        current = updated;
    }

    public void back() {
        if (backStack.isEmpty()) return;

        if (current != null) {
            forwardStack.push(current);
            trim(forwardStack);
        }

        current = backStack.pop();
        render(current);
    }

    public void forward() {
        if (forwardStack.isEmpty()) return;

        if (current != null) {
            backStack.push(current);
            trim(backStack);
        }

        current = forwardStack.pop();
        render(current);
    }

    private void render(Route r) {
        if (renderer != null) {
            renderer.render(r);
        }
    }

    private void trim(Deque<Route> stack) {
        while (stack.size() > maxDepth) {
            stack.removeLast();
        }
    }
}
