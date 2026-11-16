package com.mafe.incidentai.service;

import com.mafe.incidentai.domain.entity.User;
import com.mafe.incidentai.domain.entity.UserPreferences;
import com.mafe.incidentai.dto.UserPreferencesDTO;
import com.mafe.incidentai.exception.ResourceNotFoundException;
import com.mafe.incidentai.repository.UserPreferencesRepository;
import com.mafe.incidentai.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Reads and saves the interface preferences of each user in the database
@Service
@RequiredArgsConstructor
public class UserPreferencesService {

    private final UserPreferencesRepository preferencesRepository;
    private final UserRepository userRepository;

    // Preferences of the user (all empty if the user never changed any)
    @Transactional(readOnly = true)
    public UserPreferencesDTO get(Long userId) {
        return preferencesRepository.findById(userId)
                .map(p -> new UserPreferencesDTO(p.getSidebarCollapsed(), p.getKnowledgeView()))
                .orElse(new UserPreferencesDTO(null, null));
    }

    // Saves the fields that were sent; the rest keep their stored value
    @Transactional
    public UserPreferencesDTO update(Long userId, UserPreferencesDTO changes) {
        UserPreferences prefs = preferencesRepository.findById(userId).orElseGet(() -> {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));
            UserPreferences created = new UserPreferences();
            created.setUser(user);
            return created;
        });
        if (changes.sidebarCollapsed() != null) {
            prefs.setSidebarCollapsed(changes.sidebarCollapsed());
        }
        if (changes.knowledgeView() != null) {
            prefs.setKnowledgeView(changes.knowledgeView());
        }
        UserPreferences saved = preferencesRepository.save(prefs);
        return new UserPreferencesDTO(saved.getSidebarCollapsed(), saved.getKnowledgeView());
    }
}
