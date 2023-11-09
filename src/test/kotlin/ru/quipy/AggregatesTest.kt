package ru.quipy

import org.junit.jupiter.api.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import ru.quipy.api.ProjectAggregate
import ru.quipy.api.UserAggregate
import ru.quipy.core.EventSourcingService
import ru.quipy.logic.*
import java.lang.IllegalArgumentException
import java.util.*
import java.util.logging.Logger


@SpringBootTest
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AggregatesTest {
    @Autowired
    private lateinit var projectEsService: EventSourcingService<UUID, ProjectAggregate, ProjectAggregateState>
    @Autowired
    private lateinit var userEsService: EventSourcingService<UUID, UserAggregate, UserAggregateState>

    private lateinit var userId1: UUID
    private lateinit var userMemberId11: UUID
    private lateinit var userMemberId12: UUID

    private lateinit var userId2: UUID
    private lateinit var userMemberId22: UUID
    private lateinit var userMemberId21: UUID

    private lateinit var projectId1: UUID
    private lateinit var statusIdDefault1: UUID

    private lateinit var projectId2: UUID
    private lateinit var statusIdDefault2: UUID

    val logger: org.slf4j.Logger = org.slf4j.LoggerFactory.getLogger(AggregatesTest::class.java)

    @BeforeAll
    fun setup() {

        userId1 = UUID.randomUUID()
        userId2 = UUID.randomUUID()

        userEsService.create { it.createUser(userId1, "lievvl", "Eugene", "superpassword") }
        userEsService.create { it.createUser(userId2, "m", "Mikhail", "superpassword") }

        projectId1 = UUID.randomUUID()
        var event = projectEsService.create { it.create(projectId1, "SuperProject1", userId1)}
        userMemberId11 = event.creatorMemberId
        statusIdDefault1 = event.defaultStatusId

        projectId2 = UUID.randomUUID()
        event = projectEsService.create { it.create(projectId2, "SuperProject2", userId2)}
        userMemberId22 = event.creatorMemberId
        statusIdDefault2 = event.defaultStatusId

        userMemberId12 = UUID.randomUUID()
        projectEsService.update(projectId1) { it.addUser(userId1, userMemberId12) }

        userMemberId21 = UUID.randomUUID()
        projectEsService.update(projectId2) { it.addUser(userId2, userMemberId21) }

        val project1 = projectEsService.getState(projectId1)
        val project2 = projectEsService.getState(projectId2)

        Assertions.assertNotNull(project1)
        Assertions.assertNotNull(project2)

        Assertions.assertEquals(2, project1!!.members.size)
        Assertions.assertEquals(2, project2!!.members.size)
    }

    @Test
    fun addTaskToProject1WithProject2Default_ThrowException() {
        var taskId = UUID.randomUUID()
        Assertions.assertThrows(IllegalArgumentException::class.java) {projectEsService.update(projectId1) { it.createTask(taskId, "testTask", statusIdDefault2)}}
    }

    @Test
    fun addTaskToProject1WithProject1Default_tryToAssignDefault2_ThrowException_tryToAssignInProject2_ThrowException() {
        var taskId = UUID.randomUUID()
        projectEsService.update(projectId1) { it.createTask(taskId, "testTask", statusIdDefault1)}
        Assertions.assertThrows(IllegalArgumentException::class.java) {projectEsService.update(projectId1) { it.changeTaskStatus(taskId, statusIdDefault2)}}
        Assertions.assertThrows(IllegalArgumentException::class.java) {projectEsService.update(projectId1) { it.changeTaskStatus(taskId, statusIdDefault2)}}
    }

    @Test
    fun addTaskToProject1WithProject1Default_tryToAssignuserMember22_ThrowException() {
        var taskId = UUID.randomUUID()
        projectEsService.update(projectId1) { it.createTask(taskId, "testTask", statusIdDefault1)}
        Assertions.assertThrows(IllegalArgumentException::class.java) {projectEsService.update(projectId1) { it.assignUser(UUID.randomUUID(), taskId, userMemberId22)}}
    }

    @Test
    fun addTaskToProject1WithProject1Default_rowException() {
        var taskId = UUID.randomUUID()
        projectEsService.update(projectId1) { it.createTask(taskId, "testTask", statusIdDefault1)}
        Assertions.assertThrows(IllegalArgumentException::class.java) {projectEsService.update(projectId1) { it.changeTaskStatus(taskId, statusIdDefault2)}}
    }

    @Test
    fun addTaskToProject1_addStatus_assignNewStatusOnTask() {
        var taskId = UUID.randomUUID()
        projectEsService.update(projectId1) { it.createTask(taskId, "testTask", statusIdDefault1)}
        var statusId = UUID.randomUUID()
        projectEsService.update(projectId1) { it.createStatus(statusId, "New status", "Red")}
        projectEsService.update(projectId1) { it.changeTaskStatus(taskId, statusId)}
        var project = projectEsService.getState(projectId1)
        Assertions.assertEquals(statusId, project!!.tasks[taskId]!!.status)
    }

    @Test
    fun addTaskToProject1_addStatus1_addStatus2_assignTaskToStatus1_tryToDeleteStatus1_ThrowException_assignStatus2OnTask_tryToDeleteStatus1() {
        var statusId1 = UUID.randomUUID()
        projectEsService.update(projectId1) { it.createStatus(statusId1, "New status 1", "Red")}
        var statusId2 = UUID.randomUUID()
        projectEsService.update(projectId1) { it.createStatus(statusId2, "New status 2", "Black")}

        var taskId = UUID.randomUUID()
        projectEsService.update(projectId1) { it.createTask(taskId, "testTask", statusId1)}


        Assertions.assertThrows(IllegalArgumentException::class.java) { projectEsService.update(projectId1) { it.deleteStatus(statusId1) } }

        projectEsService.update(projectId1) { it.changeTaskStatus(taskId, statusId2)}
        projectEsService.update(projectId1) { it.deleteStatus(statusId1) }

        var project = projectEsService.getState(projectId1)
        Assertions.assertNotNull(project!!.statuses[statusId2])
        Assertions.assertNull(project.statuses[statusId1])
    }
}