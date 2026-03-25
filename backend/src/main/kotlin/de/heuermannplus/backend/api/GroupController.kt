package de.heuermannplus.backend.api

import de.heuermannplus.backend.group.CreateGroupRequest
import de.heuermannplus.backend.group.CreateMembershipRequest
import de.heuermannplus.backend.group.GroupListResponse
import de.heuermannplus.backend.group.GroupResponse
import de.heuermannplus.backend.group.GroupService
import de.heuermannplus.backend.group.GroupTokenResponse
import de.heuermannplus.backend.group.InviteGroupMemberRequest
import de.heuermannplus.backend.group.JoinGroupByTokenRequest
import de.heuermannplus.backend.group.MembershipDecisionRequest
import de.heuermannplus.backend.group.UpdateGroupRequest
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
@RequestMapping("/api/private/groups")
class GroupController(
    private val groupService: GroupService,
    private val appUserStore: AppUserStore
) {

    @GetMapping
    fun list(authentication: JwtAuthenticationToken): GroupListResponse =
        groupService.list(authentication.toCurrentUser(appUserStore))

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        @RequestBody request: CreateGroupRequest,
        authentication: JwtAuthenticationToken
    ): GroupResponse =
        groupService.createGroup(request, authentication.toCurrentUser(appUserStore))

    @GetMapping("/{groupId}")
    fun get(
        @PathVariable groupId: Long,
        authentication: JwtAuthenticationToken
    ): GroupResponse =
        groupService.getGroup(groupId, authentication.toCurrentUser(appUserStore))

    @PutMapping("/{groupId}")
    fun update(
        @PathVariable groupId: Long,
        @RequestBody request: UpdateGroupRequest,
        authentication: JwtAuthenticationToken
    ): GroupResponse =
        groupService.updateGroup(groupId, request, authentication.toCurrentUser(appUserStore))

    @DeleteMapping("/{groupId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(
        @PathVariable groupId: Long,
        authentication: JwtAuthenticationToken
    ) {
        groupService.deleteGroup(groupId, authentication.toCurrentUser(appUserStore))
    }

    @PostMapping("/{groupId}/members/invite")
    fun invite(
        @PathVariable groupId: Long,
        @RequestBody request: InviteGroupMemberRequest,
        authentication: JwtAuthenticationToken
    ): GroupResponse =
        groupService.inviteMember(groupId, request, authentication.toCurrentUser(appUserStore))

    @PostMapping("/{groupId}/members/{membershipId}/accept")
    fun acceptInvitation(
        @PathVariable groupId: Long,
        @PathVariable membershipId: Long,
        authentication: JwtAuthenticationToken
    ): GroupResponse =
        groupService.acceptInvitation(groupId, membershipId, authentication.toCurrentUser(appUserStore))

    @DeleteMapping("/{groupId}/members/{membershipId}")
    fun removeMember(
        @PathVariable groupId: Long,
        @PathVariable membershipId: Long,
        authentication: JwtAuthenticationToken
    ): GroupResponse =
        groupService.removeMember(groupId, membershipId, authentication.toCurrentUser(appUserStore))

    @PostMapping("/{groupId}/membership-requests")
    fun requestMembership(
        @PathVariable groupId: Long,
        @RequestBody request: CreateMembershipRequest,
        authentication: JwtAuthenticationToken
    ): GroupListResponse =
        groupService.requestMembership(groupId, request, authentication.toCurrentUser(appUserStore))

    @PostMapping("/{groupId}/membership-requests/{requestId}/approve")
    fun approveJoinRequest(
        @PathVariable groupId: Long,
        @PathVariable requestId: Long,
        @RequestBody(required = false) request: MembershipDecisionRequest?,
        authentication: JwtAuthenticationToken
    ): GroupResponse =
        groupService.approveJoinRequest(groupId, requestId, request ?: MembershipDecisionRequest(), authentication.toCurrentUser(appUserStore))

    @PostMapping("/{groupId}/membership-requests/{requestId}/reject")
    fun rejectJoinRequest(
        @PathVariable groupId: Long,
        @PathVariable requestId: Long,
        @RequestBody(required = false) request: MembershipDecisionRequest?,
        authentication: JwtAuthenticationToken
    ): GroupResponse =
        groupService.rejectJoinRequest(groupId, requestId, request ?: MembershipDecisionRequest(), authentication.toCurrentUser(appUserStore))

    @PostMapping("/{groupId}/tokens")
    fun createToken(
        @PathVariable groupId: Long,
        authentication: JwtAuthenticationToken
    ): GroupTokenResponse =
        groupService.createInvitationToken(groupId, authentication.toCurrentUser(appUserStore))

    @PostMapping("/join-by-token")
    fun joinByToken(
        @RequestBody request: JoinGroupByTokenRequest,
        authentication: JwtAuthenticationToken
    ): GroupResponse =
        groupService.joinByToken(request, authentication.toCurrentUser(appUserStore))

    @PostMapping("/{groupId}/leave")
    fun leaveGroup(
        @PathVariable groupId: Long,
        authentication: JwtAuthenticationToken
    ): GroupListResponse =
        groupService.leaveGroup(groupId, authentication.toCurrentUser(appUserStore))

    @PostMapping("/{groupId}/admins/{membershipId}/grant")
    fun grantAdmin(
        @PathVariable groupId: Long,
        @PathVariable membershipId: Long,
        authentication: JwtAuthenticationToken
    ): GroupResponse =
        groupService.grantAdmin(groupId, membershipId, authentication.toCurrentUser(appUserStore))

    @PostMapping("/{groupId}/admins/{membershipId}/revoke")
    fun revokeAdmin(
        @PathVariable groupId: Long,
        @PathVariable membershipId: Long,
        authentication: JwtAuthenticationToken
    ): GroupResponse =
        groupService.revokeAdmin(groupId, membershipId, authentication.toCurrentUser(appUserStore))
}
