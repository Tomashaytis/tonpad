package org.example.tonpad.ui.service;

import java.util.Objects;

import org.springframework.stereotype.Service;

import javafx.collections.ObservableList;
import javafx.scene.Scene;
import lombok.NoArgsConstructor;

@Service
@NoArgsConstructor
public class ThemeService {
    private final String lightUrl = Objects.requireNonNull(getClass().getResource("/ui/css/themes/light.css")).toExternalForm();
    private final String darkUrl = Objects.requireNonNull(getClass().getResource("/ui/css/themes/dark.css")).toExternalForm();

    public enum Theme {LIGHT, DARK}

    public void apply(Scene scene, Theme theme) {
        if(scene == null) return;
        scene.getStylesheets().remove(lightUrl);
        scene.getStylesheets().remove(darkUrl);

        switch(theme) {
            case LIGHT -> scene.getStylesheets().add(lightUrl);
            case DARK -> scene.getStylesheets().add(darkUrl);
        }

        scene.getRoot().applyCss();
    }

    public void setLight(Scene scene, boolean enableLight) {
        setTheme(scene, enableLight, lightUrl);
    }
    
    public void setDark(Scene scene, boolean enableLight) {
        setTheme(scene, enableLight, darkUrl);
    }

    private void setTheme(Scene scene, boolean enableLight, String themeUrl) {
        if(scene == null) return;
        ObservableList<String> list = scene.getStylesheets();
        if(enableLight) {
            if(!list.contains(themeUrl)) {
                list.add(themeUrl);
            }
        }
        else {
            list.remove(themeUrl);
        }
        scene.getRoot().applyCss();
    }
}
