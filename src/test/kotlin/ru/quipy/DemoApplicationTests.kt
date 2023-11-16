package ru.quipy

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.loadtest4j.LoadTester
import org.loadtest4j.Request
import org.loadtest4j.drivers.jmeter.JMeterBuilder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.SpringExtension
import ru.quipy.api.ProjectAggregate
import ru.quipy.api.UserAggregate
import ru.quipy.core.EventSourcingService
import ru.quipy.logic.*
import ru.quipy.utils.createProject
import ru.quipy.utils.createUser
import java.util.*

@ExtendWith(SpringExtension::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@ActiveProfiles("test")
class DemoApplicationTests {
	@Autowired
	private lateinit var projectEsService: EventSourcingService<UUID, ProjectAggregate, ProjectAggregateState>

	@Autowired
	private lateinit var userEsService: EventSourcingService<UUID, UserAggregate, UserAggregateState>

	val logger: org.slf4j.Logger = org.slf4j.LoggerFactory.getLogger(AggregatesTest::class.java)

	@Test
	fun loadGeneralTest() {
		logger.info("Started LoadTestLoadTest4j")
		val loadTester: LoadTester = JMeterBuilder.withUrl("http", "localhost", 8080)
			.withNumThreads(100)
			.withRampUp(5)
			.build();

		val userId = createUser(userEsService, "k", "k", "k")
		val data = createProject(projectEsService, "k", userId)
		val projectId = data.first.toString()
		val statusDefaultId = data.third.toString()

		val result = loadTester.run( listOf(Request.post("/projects/$projectId/tasks/TASK?statusId=$statusDefaultId").withHeader("Accept", "*/*")) )
		logger.info("KEKW")
		logger.info(result.percentOk.toString())
		logger.info(result.responseTime.median.toString())
		Assertions.assertTrue(result.percentOk > 99.99f)
	}
}
