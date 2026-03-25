package com.cqwu.cqwu_appointment_system.service;

import com.cqwu.cqwu_appointment_system.domain.CasUserPrincipal;

public interface CasClientService {
    CasUserPrincipal validateServiceTicket(String ticket, String serviceUrl);
}
