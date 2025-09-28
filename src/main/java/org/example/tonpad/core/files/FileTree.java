package org.example.tonpad.core.files;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.nio.file.Path;
import java.util.List;

/**
 * Класс представляет собой файловую систему в виде дерева
 *
 * <p>{@code path} - путь до файла</p>
 * <p>{@code children} - список вложенных файлов, если {@code path} - директория, иначе null</p>
 */
@Getter
@Setter
@AllArgsConstructor
public class FileTree {

    private Path path;

    private List<FileTree> children;
}
