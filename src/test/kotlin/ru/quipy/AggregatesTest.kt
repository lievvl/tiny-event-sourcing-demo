package ru.quipy

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import ru.quipy.api.ProjectAggregate
import ru.quipy.api.UserAggregate
import ru.quipy.core.EventSourcingService
import ru.quipy.logic.*
import java.util.*


@SpringBootTest
@ActiveProfiles("test")
class AggregatesTest {
    @Autowired
    private lateinit var projectEsService: EventSourcingService<UUID, ProjectAggregate, ProjectAggregateState>
    @Autowired
    private lateinit var userEsService: EventSourcingService<UUID, UserAggregate, UserAggregateState>
    @Test
    fun test() {
        val userId1 = UUID.randomUUID()
        val userId2 = UUID.randomUUID()

        userEsService.create { it.createUser(userId1, "lievvl", "Eugene", "superpassword") }
        userEsService.create { it.createUser(userId2, "m", "Mikhail", "superpassword") }

        val receivedUser1 = userEsService.getState(userId1)
        val receivedUser2 = userEsService.getState(userId2)

        Assertions.assertNotNull(receivedUser1)
        Assertions.assertNotNull(receivedUser2)

        val projectId = UUID.randomUUID()
        val event = projectEsService.create { it.create(projectId, "SuperProject", userId1)}
        val userMemberId1 = event.creatorMemberId
        val statusIdDefault = event.defaultStatusId
        val receivedProject = projectEsService.getState(projectId)
        Assertions.assertNotNull(receivedProject);
        Assertions.assertEquals(receivedProject!!.getId(), projectId)
        Assertions.assertEquals(receivedProject.creatorId, userId1)

        val userMemberId2 = UUID.randomUUID()
        projectEsService.update(projectId) { it.addUser(userId2, userMemberId2)}
        Assertions.assertEquals(2, projectEsService.getState(projectId)!!.members.size)

        val taskId = UUID.randomUUID()
        projectEsService.update(projectId) {it.createTask(taskId, "CreateTest", statusIdDefault)}

        Assertions.assertEquals(1, projectEsService.getState(projectId)!!.tasks.size)

        val executorId1 = UUID.randomUUID()
        val executorId2 = UUID.randomUUID()
        projectEsService.update(projectId) { it.assignUser(executorId1, taskId, userMemberId2)}
        projectEsService.update(projectId) { it.assignUser(executorId2, taskId, userMemberId1)}
        Assertions.assertEquals(2, projectEsService.getState(projectId)!!.tasks[taskId]!!.executors.size)

        val statusIdDone = UUID.randomUUID()
        projectEsService.update(projectId) { it.createStatus(statusIdDone,"Done", "Red") }
        Assertions.assertEquals(2, projectEsService.getState(projectId)!!.statuses.size)


        projectEsService.update(projectId) { it.changeTaskStatus(taskId, statusIdDone)}
        Assertions.assertEquals(statusIdDone, projectEsService.getState(projectId)!!.tasks[taskId]!!.status)
    }
}