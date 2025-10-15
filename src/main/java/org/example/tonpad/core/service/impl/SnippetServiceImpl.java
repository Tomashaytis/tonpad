package org.example.tonpad.core.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.tonpad.core.files.regularFiles.RegularFileService;
import org.example.tonpad.core.service.MarkdownService;
import org.example.tonpad.core.service.SnippetService;
import org.springframework.stereotype.Service;

import java.nio.file.Path;

@Service
@RequiredArgsConstructor
public class SnippetServiceImpl implements SnippetService {

    private final RegularFileService fileService;

    private final MarkdownService markdownService;

    @Override
    public String getSnippetHtml(Path filePath) {
        String fileContent = fileService.readFile(filePath);
        return markdownService.renderMarkdownFileToHtml(markdownService.parseMarkdownFile(fileContent));
    }

}
