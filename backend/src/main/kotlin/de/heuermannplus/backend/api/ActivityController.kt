package de.heuermannplus.backend.api

import de.heuermannplus.backend.activity.ActivityDetailResponse
import de.heuermannplus.backend.activity.ActivityListResponse
import de.heuermannplus.backend.activity.ActivityResponseRequest
import de.heuermannplus.backend.activity.ActivityService
import de.heuermannplus.backend.activity.AddActivityParticipantRequest
import de.heuermannplus.backend.activity.CreateActivityRequest
import de.heuermannplus.backend.activity.UpdateActivityRequest
import de.heuermannplus.backend.group.toCurrentUser
import de.heuermannplus.backend.registration.AppUserStore
import org.springframework.http.HttpStatus
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/private")
class ActivityController(
    private val activityService: ActivityService,
    private val appUserStore: AppUserStore
) {

    @GetMapping("/activities")
    fun listUpcoming(authentication: JwtAuthenticationToken): ActivityListResponse =
        activityService.listUpcomingForCurrentUser(authentication.toCurrentUser(appUserStore))

    @GetMapping("/groups/{groupId}/activities")
    fun listGroupActivities(
        @PathVariable groupId: Long,
        authentication: JwtAuthenticationToken
    ): ActivityListResponse =
        activityService.listGroupActivities(groupId, authentication.toCurrentUser(appUserStore))

    @PostMapping("/groups/{groupId}/activities")
    @ResponseStatus(HttpStatus.CREATED)
    fun createActivity(
        @PathVariable groupId: Long,
        @RequestBody request: CreateActivityRequest,
        authentication: JwtAuthenticationToken
    ): ActivityDetailResponse =
        activityService.createActivity(groupId, request, authentication.toCurrentUser(appUserStore))

    @GetMapping("/groups/{groupId}/activities/{activityId}")
    fun getActivity(
        @PathVariable groupId: Long,
        @PathVariable activityId: Long,
        authentication: JwtAuthenticationToken
    ): ActivityDetailResponse =
        activityService.getActivity(groupId, activityId, authentication.toCurrentUser(appUserStore))

    @PutMapping("/groups/{groupId}/activities/{activityId}")
    fun updateActivity(
        @PathVariable groupId: Long,
        @PathVariable activityId: Long,
        @RequestBody request: UpdateActivityRequest,
        authentication: JwtAuthenticationToken
    ): ActivityDetailResponse =
        activityService.updateActivity(groupId, activityId, request, authentication.toCurrentUser(appUserStore))

    @DeleteMapping("/groups/{groupId}/activities/{activityId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteActivity(
        @PathVariable groupId: Long,
        @PathVariable activityId: Long,
        authentication: JwtAuthenticationToken
    ) {
        activityService.deleteActivity(groupId, activityId, authentication.toCurrentUser(appUserStore))
    }

    @PostMapping("/groups/{groupId}/activities/{activityId}/participants")
    fun addParticipant(
        @PathVariable groupId: Long,
        @PathVariable activityId: Long,
        @RequestBody request: AddActivityParticipantRequest,
        authentication: JwtAuthenticationToken
    ): ActivityDetailResponse =
        activityService.addParticipant(groupId, activityId, request, authentication.toCurrentUser(appUserStore))

    @DeleteMapping("/groups/{groupId}/activities/{activityId}/participants/{participantId}")
    fun removeParticipant(
        @PathVariable groupId: Long,
        @PathVariable activityId: Long,
        @PathVariable participantId: Long,
        authentication: JwtAuthenticationToken
    ): ActivityDetailResponse =
        activityService.removeParticipant(groupId, activityId, participantId, authentication.toCurrentUser(appUserStore))

    @PostMapping("/groups/{groupId}/activities/{activityId}/response")
    fun respond(
        @PathVariable groupId: Long,
        @PathVariable activityId: Long,
        @RequestBody request: ActivityResponseRequest,
        authentication: JwtAuthenticationToken
    ): ActivityDetailResponse =
        activityService.respond(groupId, activityId, request, authentication.toCurrentUser(appUserStore))
}
