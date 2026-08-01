package com.pablomusaber.watson.shared.memory;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Entity
@Table(name = "long_term_memory")
public class LongTermMemory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String fact;

    @Column(nullable = false)
    private String category;

    @Column(nullable = false)
    private String savedAt;

    public LongTermMemory(String fact, String category, String savedAt) {
        this.fact = fact;
        this.category = category;
        this.savedAt = savedAt;
    }
}
