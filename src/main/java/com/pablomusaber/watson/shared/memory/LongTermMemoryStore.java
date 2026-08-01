package com.pablomusaber.watson.shared.memory;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LongTermMemoryStore {

    public record MemoryCandidate(String fact, String category) {}

    private final LongTermMemoryRepository repository;

    public void saveOrUpdate(String fact, String category) {
        List<LongTermMemory> existing = repository.findByCategory(category);
        existing.stream()
                .filter(m -> jaccardSimilarity(m.getFact(), fact) > 0.4)
                .findFirst()
                .ifPresentOrElse(
                        m -> {
                            m.setFact(fact);
                            m.setSavedAt(Instant.now().toString());
                            repository.save(m);
                        },
                        () -> repository.save(new LongTermMemory(fact, category, Instant.now().toString()))
                );
    }

    public String formatForPrompt() {
        List<LongTermMemory> memories = repository.findTop50ByOrderBySavedAtDesc();
        if (memories.isEmpty()) {
            return "";
        }
        String lines = memories.stream()
                .map(m -> "[" + m.getCategory() + "] " + m.getFact())
                .collect(Collectors.joining("\n"));
        return """
                --- What I know about this user ---
                %s
                ---""".formatted(lines);
    }

    private double jaccardSimilarity(String a, String b) {
        Set<String> setA = words(a);
        Set<String> setB = words(b);
        Set<String> intersection = new HashSet<>(setA);
        intersection.retainAll(setB);
        Set<String> union = new HashSet<>(setA);
        union.addAll(setB);
        return union.isEmpty() ? 0.0 : (double) intersection.size() / union.size();
    }

    private Set<String> words(String text) {
        return Arrays.stream(text.toLowerCase().split("[\\s\\p{Punct}]+"))
                .filter(w -> !w.isBlank())
                .collect(Collectors.toSet());
    }
}
