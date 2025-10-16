package com.cqwu.cqwu_appointment_system.service.impl;

import com.cqwu.cqwu_appointment_system.domain.query.VenueQuery;
import com.cqwu.cqwu_appointment_system.entity.Venue;
import com.cqwu.cqwu_appointment_system.service.VenueService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class VenueServiceImplTest {

    @Autowired
    private VenueService venueService;

    private Venue testVenue1;
    private Venue testVenue2;
    private Venue testVenue3; // 为分页和组合查询新增的数据

    /**
     * 在每个测试方法运行前执行，用于准备一些可复用的测试数据
     */
    @BeforeEach
    void setUp() {
        // 准备三条基础测试数据
        testVenue1 = new Venue();
        testVenue1.setName("基础测试-报告厅");
        testVenue1.setLocation("教学楼A栋101");
        testVenue1.setCapacity(150);
        testVenue1.setStatus("available");
        testVenue1.setOpenTime(LocalTime.of(8, 0));
        testVenue1.setCloseTime(LocalTime.of(18, 0));

        testVenue2 = new Venue();
        testVenue2.setName("基础测试-会议室");
        testVenue2.setLocation("行政楼B栋303");
        testVenue2.setCapacity(50);
        testVenue2.setStatus("maintenance");
        testVenue2.setOpenTime(LocalTime.of(9, 0));
        testVenue2.setCloseTime(LocalTime.of(17, 0));

        testVenue3 = new Venue();
        testVenue3.setName("高级测试-实验室");
        testVenue3.setLocation("教学楼A栋202");
        testVenue3.setCapacity(30);
        testVenue3.setStatus("available");

        venueService.insertVenues(List.of(testVenue1, testVenue2, testVenue3));

        assertNotNull(testVenue1.getId());
    }

    @Test
    void testSearchVenues_withVenueQueryObject() {
        // --- 场景1: 不设置任何筛选条件，只测试分页 ---
        VenueQuery query1 = new VenueQuery();
        query1.setPage(0);
        query1.setSize(2);

        Page<Venue> resultPage1 = venueService.searchVenues(query1);

        // 断言分页结果
        assertEquals(3, resultPage1.getTotalElements(), "场景1: 总记录数应为3");
        assertEquals(2, resultPage1.getTotalPages(), "场景1: 每页2条，总共应有2页");
        assertEquals(2, resultPage1.getContent().size(), "场景1: 第一页应返回2条记录");


        // --- 场景2: 设置组合筛选条件 ---
        VenueQuery query2 = new VenueQuery();
        query2.setLocation("教学楼A栋"); // 模糊匹配
        query2.setStatus("available");   // 精确匹配
        query2.setPage(0);
        query2.setSize(10);

        Page<Venue> resultPage2 = venueService.searchVenues(query2);

        // 断言筛选结果
        assertEquals(2, resultPage2.getTotalElements(), "场景2: 应找到2条'教学楼A栋'且'available'的记录");
        List<String> names = resultPage2.getContent().stream().map(Venue::getName).toList();
        assertTrue(names.contains("基础测试-报告厅"), "场景2: 结果应包含'报告厅'");
        assertTrue(names.contains("高级测试-实验室"), "场景2: 结果应包含'实验室'");


        // --- 场景3: 测试楼层筛选 (假设 testVenue1 的 floor 是 1) ---
        // 为了测试 floor，我们需要先更新一条数据
        testVenue1.setFloor(1);
        venueService.updateVenue(testVenue1);

        VenueQuery query3 = new VenueQuery();
        query3.setFloor(1); // 精确匹配楼层
        query3.setPage(0);
        query3.setSize(10);

        Page<Venue> resultPage3 = venueService.searchVenues(query3);

        // 断言楼层筛选结果
        assertEquals(1, resultPage3.getTotalElements(), "场景3: 应只找到1条在1楼的记录");
        assertEquals("基础测试-报告厅", resultPage3.getContent().get(0).getName(), "场景3: 找到的应该是报告厅");


        // --- 场景4: 搜索一个不存在的条件组合 ---
        VenueQuery query4 = new VenueQuery();
        query4.setName("这是一个不存在的名称");
        query4.setPage(0);
        query4.setSize(10);

        Page<Venue> resultPage4 = venueService.searchVenues(query4);

        // 断言空结果
        assertEquals(0, resultPage4.getTotalElements(), "场景4: 找不到结果时总数应为0");
        assertTrue(resultPage4.getContent().isEmpty(), "场景4: 找不到结果时内容列表应为空");
    }

    @Test
    @DisplayName("测试批量插入多个场所")
    void testInsertVenues_withValidList_shouldSucceed() {
        // --- Arrange (准备数据) ---
        Venue batchVenue1 = new Venue();
        batchVenue1.setName("批量测试-001");
        batchVenue1.setLocation("批量楼-1F");
        batchVenue1.setStatus("available");

        Venue batchVenue2 = new Venue();
        batchVenue2.setName("批量测试-002");
        batchVenue2.setLocation("批量楼-2F");
        batchVenue2.setStatus("maintenance");

        List<Venue> venueList = new ArrayList<>();
        venueList.add(batchVenue1);
        venueList.add(batchVenue2);

        // --- Act (执行操作) ---
        int affectedRows = venueService.insertVenues(venueList);

        // --- Assert (验证结果) ---

        // 1. 验证返回的受影响行数是否正确
        assertEquals(2, affectedRows, "批量插入应该返回 2，表示影响了 2 行");

        // 2. 回查数据库，验证数据是否真的被插入
        //    (注意：批量插入通常不会回填ID到List中的每个对象，所以我们需要通过其他条件查询)
        // List<Venue> result1 = venueService.selectVenueListByName("批量测试-001");
        // List<Venue> result2 = venueService.selectVenueListByName("批量测试-002");

        // assertFalse(result1.isEmpty(), "应该能查询到'批量测试-001'");
        // assertEquals("批量楼-1F", result1.get(0).getLocation());

        // assertFalse(result2.isEmpty(), "应该能查询到'批量测试-002'");
        // assertEquals("maintenance", result2.get(0).getStatus());
    }
}