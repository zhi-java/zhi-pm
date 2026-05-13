package io.github.zhi.pm.danmaku.filter;

import java.util.Collections;
import java.util.List;

public class ContentFilter {
    private final List<String> sensitiveWords;

    public ContentFilter(List<String> sensitiveWords) {
        this.sensitiveWords = sensitiveWords == null ? Collections.emptyList() : sensitiveWords;
    }

    public boolean isBlocked(String content) {
        if (content == null || content.isBlank()) {
            return false;
        }
        String lower = content.toLowerCase();
        return sensitiveWords.stream().anyMatch(word -> lower.contains(word.toLowerCase()));
    }
}
