package com.antigravity.trading.domain.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * 시스템 설정 엔티티.
 * DB 기반의 동적 설정 관리를 위해 사용될 수 있습니다.
 */
@Entity
@Table(name = "system_config")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SystemConfig {

    @Id
    @Column(length = 50)
    private String configKey;

    @Column(nullable = false)
    private String configValue;

    private String description;
}
