package de.heuermannplus.backend.api

import de.heuermannplus.backend.group.GroupInvitationResultResponse
import de.heuermannplus.backend.group.GroupService
import de.heuermannplus.backend.group.RespondToGroupInvitationRequest
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/public/group-invitations")
class GroupInvitationPublicController(
    private val groupService: GroupService
) {

    @PostMapping("/respond")
    fun respond(@RequestBody request: RespondToGroupInvitationRequest): GroupInvitationResultResponse =
        groupService.respondToInvitation(request)
}
