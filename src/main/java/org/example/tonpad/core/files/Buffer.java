package org.example.tonpad.core.files;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@RequiredArgsConstructor
@Setter
@Getter
@Component
public class Buffer {
    private List<Path> copyBuffer = new ArrayList<>();
    private boolean cutMode = false;
}
