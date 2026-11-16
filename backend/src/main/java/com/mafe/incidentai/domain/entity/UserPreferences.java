package com.mafe.incidentai.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

// Interface preferences of a user, stored in the database so they follow the user on any browser
@Entity
@Table(name = "user_preferences")
@Getter
@Setter
@NoArgsConstructor
public class UserPreferences {

    // Same id as the user (1:1 relationship sharing the primary key)
    @Id
    @Column(name = "user_id")
    private Long userId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    // true = collapsed side menu; null = automatic (depends on the screen size)
    @Column(name = "sidebar_collapsed")
    private Boolean sidebarCollapsed;

    // How the knowledge base is shown: "cards" or "table"
    @Column(name = "knowledge_view", length = 10)
    private String knowledgeView;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Refreshes the update date on every insert and change
    @PrePersist
    @PreUpdate
    protected void touch() {
        this.updatedAt = LocalDateTime.now();
    }
}
