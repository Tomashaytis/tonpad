package org.example.tonpad.ui.extentions;

import org.example.tonpad.core.editor.Editor;

import java.nio.file.Path;

public record TabParams(Editor editor, Path path) {
}