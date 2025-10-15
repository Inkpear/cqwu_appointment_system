package com.cqwu.cqwu_appointment_system.services.impl;


import com.cqwu.cqwu_appointment_system.entity.Reservation;
import com.cqwu.cqwu_appointment_system.service.IReservationService;
import com.cqwu.cqwu_appointment_system.service.impl.IReservationServiceImpl;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@SpringBootTest
public class ReservationServiceImplTest {
    @Resource
    private IReservationServiceImpl reservationServiceIpml;

    @Test
//    @Transactional
    void createNormalReservationTest()
    {
        //构建Reservation对象
        Reservation reservation = new Reservation();

//        reservation.setId(1L);
        reservation.setStartTime(LocalDateTime.of(2025, 10, 17, 10, 0)); // 2025-10-16 10:00
        reservation.setEndTime(LocalDateTime.of(2025, 10, 18, 12, 0));   // 2025-10-16 12:00
        reservation.setVenueId(1L);
        reservation.setUserId(2L);
        reservationServiceIpml.createReservation(reservation);
        System.out.println("you can see me");

    }

    @Test
    void findReservationsByUserIdTest()
    {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Reservation> page = reservationServiceIpml.findReservationsByUserId(2L,pageable);
        System.out.println("总页数: " + page.getTotalPages());
        System.out.println("总条数: " + page.getTotalElements());
        System.out.println("当前页内容: " + page.getContent());
    }

    @Test
    void cancelReservationByUserTest()
    {
        reservationServiceIpml.cancelReservationByUser(10L,2L);
        findReservationsByUserIdTest();
    }
}
