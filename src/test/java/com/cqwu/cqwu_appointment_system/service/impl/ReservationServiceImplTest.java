package com.cqwu.cqwu_appointment_system.service.impl;


import com.cqwu.cqwu_appointment_system.entity.Reservation;
import jakarta.annotation.Resource;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;


import java.util.List;

@SpringBootTest
@org.springframework.transaction.annotation.Transactional
public class ReservationServiceImplTest {
    @Resource
    private IReservationServiceImpl reservationServiceIpml;

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
    void searchReservationsTest()
    {
        Pageable pageable = PageRequest.of(0, 10);
//        Page<Reservation> page = reservationServiceIpml.searchReservations(null,2L,null,null,pageable);
        Page<Reservation> page = reservationServiceIpml.searchReservations(1L,null,null,null,pageable);

        System.out.println("总页数: " + page.getTotalPages());
        System.out.println("总条数: " + page.getTotalElements());
        System.out.println("当前页内容: " + page.getContent());
    }

    @Test
    void findReservationByIdTest()
    {
        var tmp = reservationServiceIpml.findReservationById(10L);
        System.out.println(tmp);
    }

    @Test
    void findReservationByCodeTest()
    {
        var tmp = reservationServiceIpml.findReservationByCode("413ce0232a1346ae");
        System.out.println(tmp);
    }

    @Test
    void getUpcomingReservationsTest()
    {
        List<Reservation> tmp = reservationServiceIpml.getUpcomingReservations(1L,1);
        for(Reservation reservation : tmp)
        {
            System.out.println(reservation);
        }
    }
}
