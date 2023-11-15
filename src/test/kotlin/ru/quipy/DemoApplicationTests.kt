package ru.quipy

import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import ru.quipy.api.ProjectAggregate
import ru.quipy.api.UserAggregate
import ru.quipy.core.EventSourcingService
import ru.quipy.logic.*
import java.lang.RuntimeException
import java.util.*
import java.util.concurrent.CountDownLatch

@SpringBootTest
class DemoApplicationTests {

	@Autowired
	private lateinit var projectEsService: EventSourcingService<UUID, ProjectAggregate, ProjectAggregateState>
	@Autowired
	private lateinit var userEsService: EventSourcingService<UUID, UserAggregate, UserAggregateState>

	val logger: org.slf4j.Logger = org.slf4j.LoggerFactory.getLogger(AggregatesTest::class.java)

	@Test
	fun createUsersConcurrently() {
		val userId1 = UUID.randomUUID()
		val userId2 = UUID.randomUUID()

		val startLatch = CountDownLatch(1)
		val finishLatch = CountDownLatch(2)

		val createUserThread1 = Thread {
			startLatch.await()
			userEsService.create{it.createUser(userId1, "user1", "user1", "user")}
			finishLatch.countDown()
		}

		val createUserThread2 = Thread {
			startLatch.await()
			userEsService.create{it.createUser(userId2, "user2", "user2", "user")}
			finishLatch.countDown()
		}

		createUserThread1.start()
		createUserThread2.start()

		startLatch.countDown()
		finishLatch.await()

		val user1 = userEsService.getState(userId1)
		val user2 = userEsService.getState(userId2)

		Assertions.assertNotNull(user1)
		Assertions.assertNotNull(user2)
	}
}
