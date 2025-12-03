package com.cqwu.cqwu_appointment_system.domain.dto;

import com.cqwu.cqwu_appointment_system.entity.Venue;
import com.cqwu.cqwu_appointment_system.entity.VenueDetail;
import lombok.Data;

import java.time.LocalTime;

/**
 * 场所详情DTO（包含扩展信息）
 */
@Data
public class VenueDetailDto {
    private Long id;
    private String name;
    private String location;
    private Integer floor;
    private Integer capacity;
    private Long categoryId;
    private String equipment;
    private String baseStatus;
    private String realtimeStatus;
    private LocalTime openTime;
    private LocalTime closeTime;
    private Boolean requiresApproval;
    private Integer maxBookingDurationMinutes;
    
    /**
     * 场所描述
     */
    private String description;

    /**
     * 从Venue和VenueDetail构建DTO
     */
    public static VenueDetailDto from(Venue venue, VenueDetail detail) {
        VenueDetailDto dto = new VenueDetailDto();
        dto.setId(venue.getId());
        dto.setName(venue.getName());
        dto.setLocation(venue.getLocation());
        dto.setFloor(venue.getFloor());
        dto.setCapacity(venue.getCapacity());
        dto.setCategoryId(venue.getCategoryId());
        dto.setEquipment(venue.getEquipment());
        dto.setBaseStatus(venue.getBaseStatus());
        dto.setRealtimeStatus(venue.getRealtimeStatus());
        dto.setOpenTime(venue.getOpenTime());
        dto.setCloseTime(venue.getCloseTime());
        dto.setRequiresApproval(venue.getRequiresApproval());
        dto.setMaxBookingDurationMinutes(venue.getMaxBookingDurationMinutes());
        
        if (detail != null) {
            dto.setDescription(detail.getDescription());
        }
        
        return dto;
    }

    public static VenueDetailDto from(Venue venue) {
        return from(venue, null);
    }
}
