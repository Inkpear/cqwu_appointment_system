package com.cqwu.cqwu_appointment_system;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.cqwu.cqwu_appointment_system.entity.Venue;
import com.cqwu.cqwu_appointment_system.mapper.VenueMapper;

import java.sql.Time;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class CqwuAppointmentSystemApplicationTests {

	@Autowired
	private VenueMapper venueMapper; // 变量名遵循 Java 命名规范 (camelCase)

	/**
	 * 测试向 venue 表插入一条数据，并验证插入是否成功
	 */
	@Test
	// @Transactional // 关键注解：让测试运行在事务中，测试结束后自动回滚，不污染数据库
	void test_venue_mapper_insert_data() {
		// 1. 准备要插入的数据 (Arrange)
		Venue newVenue = new Venue();
		newVenue.setName("多功能报告厅A厅");
		newVenue.setLocation("图书馆一楼");
		newVenue.setCapacity(200);
		newVenue.setCategoryId(1L); // 假设分类ID为1
		newVenue.setEquipment("投影仪, 音响, 无线麦克风");
		newVenue.setStatus("available"); // 使用表定义中的默认值
		newVenue.setOpenTime(Time.valueOf(LocalTime.of(8, 0, 0)));
		newVenue.setCloseTime(Time.valueOf(LocalTime.of(22, 0, 0)));

		// 2. 执行插入操作 (Act)
		// insertSelective 方法只会插入那些值不为 null 的字段，更灵活
		int affectedRows = venueMapper.insertSelective(newVenue);

		// 3. 验证结果 (Assert)
		
		// 断言1：确认 insertSelective 操作返回了 1，表示有一行受到了影响
		assertEquals(1, affectedRows, "插入操作应该影响1行数据");

		// 断言2：确认数据被正确插入。
		// Mybatis Generator 在执行 insert 后，会自动将生成的主键 ID 回填到传入的对象中
		assertNotNull(newVenue.getId(), "插入后，实体的主键ID不应为null");
		assertTrue(newVenue.getId() > 0, "插入后，实体的主键ID应该大于0");

		// 断言3：从数据库中根据刚生成的主键ID将数据重新查出来，进行比对
		Venue insertedVenue = venueMapper.selectByPrimaryKey(newVenue.getId());
		assertNotNull(insertedVenue, "根据新ID应该能从数据库中查出记录");
		assertEquals("多功能报告厅A厅", insertedVenue.getName(), "查询出的场所名称应与插入时一致");
		assertEquals(200, insertedVenue.getCapacity(), "查询出的场所容量应与插入时一致");
        assertEquals(Time.valueOf(LocalTime.of(8, 0, 0)), insertedVenue.getOpenTime(), "查询出的开放时间应与插入时一致");

		System.out.println("测试成功：成功插入场所数据，ID为: " + newVenue.getId());
		System.out.println("从数据库回查的数据为: " + insertedVenue);
	}
}