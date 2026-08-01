package com.pablomusaber.watson.shared.memory;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LongTermMemoryRepository extends JpaRepository<LongTermMemory, Long> {

    List<LongTermMemory> findByCategory(String category);

    List<LongTermMemory> findTop50ByOrderBySavedAtDesc();
}
