package com.cqwu.cqwu_appointment_system.service.impl;

import com.cqwu.cqwu_appointment_system.entity.Venue;
import com.cqwu.cqwu_appointment_system.service.VenueService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 专门用于测试 VenueService 的更新和删除操作。
 * 使用 @Transactional 注解确保每个测试都在一个独立的回滚事务中进行，不会污染数据库。
 */
@SpringBootTest
@Transactional
class VenueServiceUpdateDeleteTest {

    @Autowired
    private VenueService venueService;

    private Venue testVenue1;
    private Venue testVenue2;
    private Venue testVenue3;

    @BeforeEach
    void setUp() {
        // 准备三条基础测试数据，与您之前的测试文件一致
        testVenue1 = new Venue();
        testVenue1.setName("报告厅 A");
        testVenue1.setBaseStatus("AVAILABLE");
        testVenue1.setRequiresApproval(false);

        testVenue2 = new Venue();
        testVenue2.setName("会议室 B");
        testVenue2.setBaseStatus("MAINTENANCE");
        testVenue2.setRequiresApproval(true);

        testVenue3 = new Venue();
        testVenue3.setName("实验室 C");
        testVenue3.setBaseStatus("AVAILABLE");
        testVenue3.setRequiresApproval(true);

        venueService.insertVenues(List.of(testVenue1, testVenue2, testVenue3));

        assertNotNull(testVenue1.getId());
        assertNotNull(testVenue2.getId());
        assertNotNull(testVenue3.getId());
    }

    @Test
    @DisplayName("测试单体更新方法 updateVenue")
    void testUpdateVenue_ShouldUpdateSuccessfully() {
        // Arrange: 准备更新数据
        Venue venueToUpdate = venueService.selectVenueById(testVenue1.getId());
        assertNotNull(venueToUpdate);
        
        String newName = "已更新的报告厅 A";
        String newBaseStatus = "MAINTENANCE";
        
        venueToUpdate.setName(newName);
        venueToUpdate.setBaseStatus(newBaseStatus);
        venueToUpdate.setCapacity(200);

        // Act: 执行更新操作
        int affectedRows = venueService.updateVenue(venueToUpdate);

        // Assert: 验证结果
        assertEquals(1, affectedRows, "单体更新应影响1行");
        
        Venue updatedVenue = venueService.selectVenueById(testVenue1.getId());
        assertEquals(newName, updatedVenue.getName(), "名称应被更新");
        assertEquals(newBaseStatus, updatedVenue.getBaseStatus(), "基础状态应被更新");
        assertEquals(200, updatedVenue.getCapacity(), "容量应被更新");
    }

    @Test
    @DisplayName("测试批量删除方法 deleteVenueByIds")
    void testDeleteVenueByIds_ShouldDeleteSpecifiedVenues() {
        // Arrange: 准备要删除的ID列表
        Long[] idsToDelete = {testVenue1.getId(), testVenue3.getId()};

        // Act: 执行批量删除
        int affectedRows = venueService.deleteVenueByIds(idsToDelete);

        // Assert: 验证结果
        assertEquals(2, affectedRows, "批量删除应影响2行");
        
        assertNull(venueService.selectVenueById(testVenue1.getId()), "testVenue1 应已被删除");
        assertNull(venueService.selectVenueById(testVenue3.getId()), "testVenue3 应已被删除");
        assertNotNull(venueService.selectVenueById(testVenue2.getId()), "testVenue2 应仍然存在");
    }

    @Test
    @DisplayName("测试通用批量更新方法 updateVenuesBatch")
    void testUpdateVenuesBatch_ShouldUpdateMultipleVenues() {
        // Arrange: 准备要更新的ID列表和更新数据
        Long[] idsToUpdate = {testVenue1.getId(), testVenue3.getId()};
        
        Venue updateData = new Venue();
        updateData.setBaseStatus("DISABLED"); // 将两个场所都设为禁用
        updateData.setRequiresApproval(true);   // 将两个场所都设为需要审批

        // Act: 执行通用批量更新
        int affectedRows = venueService.updateVenuesBatch(idsToUpdate, updateData);

        // Assert: 验证结果
        assertEquals(2, affectedRows, "批量更新应影响2行");

        Venue updatedVenue1 = venueService.selectVenueById(testVenue1.getId());
        Venue updatedVenue3 = venueService.selectVenueById(testVenue3.getId());
        Venue unaffectedVenue2 = venueService.selectVenueById(testVenue2.getId());

        // 验证已更新的记录
        assertEquals("DISABLED", updatedVenue1.getBaseStatus(), "testVenue1 的基础状态应被更新");
        assertTrue(updatedVenue1.getRequiresApproval(), "testVenue1 的审批要求应被更新");
        
        assertEquals("DISABLED", updatedVenue3.getBaseStatus(), "testVenue3 的基础状态应被更新");
        assertTrue(updatedVenue3.getRequiresApproval(), "testVenue3 的审批要求应被更新");
        
        // 验证未受影响的记录
        assertEquals("MAINTENANCE", unaffectedVenue2.getBaseStatus(), "testVenue2 的状态不应被改变");
    }

    @Test
    @DisplayName("测试通用批量更新方法 - 使用不存在的ID")
    void testUpdateVenuesBatch_WithNonExistentIds_ShouldReturnZero() {
        // Arrange
        Long[] nonExistentIds = {9999L, 10000L};
        Venue updateData = new Venue();
        updateData.setBaseStatus("AVAILABLE");

        // Act
        int affectedRows = venueService.updateVenuesBatch(nonExistentIds, updateData);

        // Assert
        assertEquals(0, affectedRows, "对不存在的ID进行批量更新，应影响0行");
    }
}