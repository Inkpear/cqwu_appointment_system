package com.cqwu.cqwu_appointment_system.domain;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CasUserPrincipal {
    private String userId;
    private Map<String, String> attributes;
}
