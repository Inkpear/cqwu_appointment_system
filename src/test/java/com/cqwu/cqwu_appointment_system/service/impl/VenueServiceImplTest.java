package com.cqwu.cqwu_appointment_system.service.impl;

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
// @Transactional
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
    void testSelectVenueListByName() {
        List<Venue> result1 = venueService.selectVenueListByName("基础测试-报告厅");
        List<Venue> result2 = venueService.selectVenueListByName("测试");
        List<Venue> result3 = venueService.selectVenueListByName("不存在的名称");
        assertEquals(1, result1.size());
        assertEquals("教学楼A栋101", result1.get(0).getLocation());
        assertEquals(3, result2.size(), "模糊搜索'测试'应该找到三条记录");
        assertTrue(result3.isEmpty());
    }

    @Test
    void testSelectVenueListByStatus() {
        List<Venue> availableVenues = venueService.selectVenueListByStatus("available");
        List<Venue> maintenanceVenues = venueService.selectVenueListByStatus("maintenance");
        assertEquals(2, availableVenues.size(), "状态为 'available' 的应该有两条");
        assertEquals(1, maintenanceVenues.size());
    }
    
    @Test
    void testSelectBenueListByLocation() {
        List<Venue> result = venueService.selectBenueListByLocation("A栋");
        assertEquals(2, result.size(), "位置包含 'A栋' 的应该有两条");
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

    // =================================================================
    //  ↓ ↓ ↓        新增的分页模糊搜索服务测试函数        ↓ ↓ ↓
    // =================================================================

    @Test
    void testSearchVenues_PagingAndFiltering() {
        // 场景1: 不带任何条件，测试分页
        Page<Venue> page1 = venueService.searchVenues(null, null, null, 0, 2);
        assertEquals(3, page1.getTotalElements(), "总数应为3");
        assertEquals(2, page1.getTotalPages(), "每页2条，应有2页");
        assertEquals(2, page1.getContent().size(), "第一页应有2条记录");

        // 场景2: 带组合条件，测试过滤
        Page<Venue> page2 = venueService.searchVenues(null, "available", "教学楼A栋", 0, 10);
        assertEquals(2, page2.getTotalElements(), "应找到2条 '教学楼A栋' 且 'available' 的记录");
        List<String> names = page2.getContent().stream().map(Venue::getName).toList();
        assertTrue(names.contains("基础测试-报告厅"));
        assertTrue(names.contains("高级测试-实验室"));

        // 场景3: 找不到结果
        Page<Venue> page3 = venueService.searchVenues("不存在的名称", null, null, 0, 10);
        assertEquals(0, page3.getTotalElements(), "找不到结果时总数应为0");
        assertTrue(page3.getContent().isEmpty(), "找不到结果时内容应为空列表");
    }
}