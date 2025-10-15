package com.cqwu.cqwu_appointment_system.service.impl;

import com.cqwu.cqwu_appointment_system.domain.query.VenueQuery;
import com.cqwu.cqwu_appointment_system.entity.Venue;
import com.cqwu.cqwu_appointment_system.service.VenueService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
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
        venueService.insertVenue(testVenue1);

        testVenue2 = new Venue();
        testVenue2.setName("基础测试-会议室");
        testVenue2.setLocation("行政楼B栋303");
        testVenue2.setCapacity(50);
        testVenue2.setStatus("maintenance");
        testVenue2.setOpenTime(LocalTime.of(9, 0));
        testVenue2.setCloseTime(LocalTime.of(17, 0));
        venueService.insertVenue(testVenue2);

        testVenue3 = new Venue();
        testVenue3.setName("高级测试-实验室");
        testVenue3.setLocation("教学楼A栋202");
        testVenue3.setCapacity(30);
        testVenue3.setStatus("available");
        venueService.insertVenue(testVenue3);
    }

    @Test
    void testInsertAndSelectById() {
        Venue newVenue = new Venue();
        newVenue.setName("待插入的新场所");
        newVenue.setLocation("科技楼C栋");
        newVenue.setStatus("available");
        int affectedRows = venueService.insertVenue(newVenue);
        assertNotNull(newVenue.getId());
        Venue selectedVenue = venueService.selectVenueById(newVenue.getId());
        assertEquals(1, affectedRows);
        assertNotNull(selectedVenue);
        assertEquals("待插入的新场所", selectedVenue.getName());
    }

    @Test
    void testUpdateVenue() {
        Long idToUpdate = testVenue1.getId();
        Venue venueToUpdate = new Venue();
        venueToUpdate.setId(idToUpdate);
        venueToUpdate.setName("更新后的-报告厅");
        venueToUpdate.setCapacity(180);
        int affectedRows = venueService.updateVenue(venueToUpdate);
        Venue updatedVenue = venueService.selectVenueById(idToUpdate);
        assertEquals(1, affectedRows);
        assertNotNull(updatedVenue);
        assertEquals("更新后的-报告厅", updatedVenue.getName());
        assertEquals(180, updatedVenue.getCapacity());
        assertEquals("教学楼A栋101", updatedVenue.getLocation());
    }

    @Test
    void testDeleteVenueByIds() {
        Venue venueToDelete = new Venue();
        venueToDelete.setName("待删除的场所");
        venueService.insertVenue(venueToDelete);
        assertNotNull(venueToDelete.getId());
        Long[] idsToDelete = {testVenue1.getId(), venueToDelete.getId()};
        int deletedCount = venueService.deleteVenueByIds(idsToDelete);
        assertNull(venueService.selectVenueById(testVenue1.getId()));
        assertNull(venueService.selectVenueById(venueToDelete.getId()));
        assertNotNull(venueService.selectVenueById(testVenue2.getId()));
        assertEquals(2, deletedCount);
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
}