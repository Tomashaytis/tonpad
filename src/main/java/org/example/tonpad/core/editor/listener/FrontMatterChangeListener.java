package org.example.tonpad.core.editor.listener;

import org.example.tonpad.core.editor.event.FrontMatterChangeEvent;

@FunctionalInterface
public interface FrontMatterChangeListener {

    void onFrontMatterChanged(FrontMatterChangeEvent event);
}