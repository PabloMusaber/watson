package com.pablomusaber.watson.shared;

import java.util.List;

public interface PersonaProfile {

    String name();

    String systemPrompt();

    String goalDescription();

    List<String> watchlist();

    Class<?> goalClass();
}
