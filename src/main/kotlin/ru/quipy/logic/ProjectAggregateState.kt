package ru.quipy.logic

import ru.quipy.api.*
import ru.quipy.core.annotations.StateTransitionFunc
import ru.quipy.domain.AggregateState
import java.util.*

// Service's business logic
class ProjectAggregateState : AggregateState<UUID, ProjectAggregate> {
    private lateinit var projectId: UUID
    var createdAt: Long = System.currentTimeMillis()
    var updatedAt: Long = System.currentTimeMillis()

    lateinit var projectTitle: String
    lateinit var creatorId: UUID
    var tasks = mutableMapOf<UUID, TaskEntity>()
    var statuses = mutableMapOf<UUID, StatusEntity>()
    var members = mutableMapOf<UUID, ProjectMemberEntity>()

    override fun getId() = projectId

    // State transition functions which is represented by the class member function
    @StateTransitionFunc
    fun projectCreatedApply(event: ProjectCreatedEvent) {
        projectId = event.projectId
        projectTitle = event.title
        creatorId = event.creatorId
        members[event.creatorMemberId] = ProjectMemberEntity(event.creatorMemberId, event.creatorId)
        statuses[event.defaultStatusId] = StatusEntity(event.defaultStatusId, "CREATED", "White")
        updatedAt = createdAt
    }

    @StateTransitionFunc
    fun taskCreatedApply(event: TaskCreatedEvent) {
        tasks[event.taskId] = TaskEntity(event.taskId, event.taskName, event.statusId, mutableSetOf())
        updatedAt = createdAt
    }

    @StateTransitionFunc
    fun userAssignedApply(event: UserAssignedEvent) {
        members[event.memberExecutorId] = ProjectMemberEntity(event.memberExecutorId, event.memberId)
        updatedAt = createdAt
    }

    @StateTransitionFunc
    fun taskStatusChangedApply(event: TaskStatusChangedEvent) {
        tasks[event.taskId]!!.status = event.statusId
        updatedAt = createdAt
    }

    @StateTransitionFunc
    fun taskStatusCreatedApply(event: TaskStatusCreatedEvent) {
        statuses[event.statusId] = StatusEntity(event.statusId, event.statusText, event.statusColor)
        updatedAt = createdAt
    }

    @StateTransitionFunc
    fun userAddedToProjectApply(event: UserAddedToProjectEvent) {
        members[event.userMemberId] = ProjectMemberEntity(event.userMemberId, event.userId)
        updatedAt = createdAt
    }

    @StateTransitionFunc
    fun taskTittleChangedApply(event: TaskTittleChangedEvent) {
        tasks[event.taskId]!!.name = event.newTittle
        updatedAt = createdAt
    }

    @StateTransitionFunc
    fun taskStatusDeletedApply(event: TaskStatusDeletedEvent) {
        tasks.remove(event.statusId)
        updatedAt = createdAt
    }
}

data class TaskEntity(
    val id: UUID = UUID.randomUUID(),
    var name: String,
    var status: UUID,
    val executors: MutableSet<UUID>,
)

data class StatusEntity(
    val id: UUID = UUID.randomUUID(),
    val statusText: String,
    val statusColor: String,
)

data class ProjectMemberEntity(
    val id: UUID = UUID.randomUUID(),
    val userId: UUID,
)

data class ExecutorEntity(
    val id: UUID = UUID.randomUUID(),
    val projectMemberId: UUID,
)
