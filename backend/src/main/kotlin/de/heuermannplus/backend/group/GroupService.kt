package de.heuermannplus.backend.group

import de.heuermannplus.backend.registration.AppUserStore
import de.heuermannplus.backend.registration.VerificationTokenService
import java.time.Clock
import java.time.Instant
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.util.StringUtils

@Service
class GroupService(
    private val groupStore: GroupStore,
    private val membershipStore: GroupMembershipStore,
    private val invitationStore: GroupInvitationStore,
    private val tokenStore: GroupInvitationTokenStore,
    private val joinRequestStore: GroupJoinRequestStore,
    private val appUserStore: AppUserStore,
    private val groupMailService: GroupMailService,
    private val tokenService: VerificationTokenService,
    private val groupProperties: GroupProperties,
    private val clock: Clock
) {
    private val emailRegex = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")
    private val inviteSuggestionLimit = 8

    @Transactional
    fun list(currentUser: CurrentUser): GroupListResponse {
        claimPendingInvitations(currentUser)

        val activeGroups = groupStore.findAllActive()
        val memberships = membershipStore.findByUserIdAndStatuses(
            currentUser.userId,
            setOf(GroupMembershipStatus.ACTIVE, GroupMembershipStatus.INVITED)
        )
        val membershipsByGroup = memberships.associateBy { it.groupId }
        val groups = activeGroups.filter { membershipsByGroup.containsKey(it.id) }
        val groupIds = groups.mapNotNull { it.id }.toSet()
        val joinRequests = joinRequestStore.findByRequestedByUserIdAndStatus(currentUser.userId, GroupJoinRequestStatus.PENDING)
        val pendingRequestGroupIds = joinRequests.map { it.groupId }.toSet()

        return GroupListResponse(
            groups = groups.map { group -> group.toSummary(membershipsByGroup[group.id]!!, activeMemberCount(group.id!!)) },
            invitations = memberships
                .filter { it.status == GroupMembershipStatus.INVITED }
                .mapNotNull { membership ->
                    val group = activeGroups.firstOrNull { it.id == membership.groupId } ?: return@mapNotNull null
                    GroupInvitationSummaryResponse(
                        membershipId = membership.id!!,
                        groupId = group.id!!,
                        groupName = group.name,
                        displayName = membership.displayName,
                        inviteEmail = membership.inviteEmail,
                        invitedAt = membership.createdAt
                    )
                },
            joinRequests = joinRequests.mapNotNull { request ->
                val group = activeGroups.firstOrNull { it.id == request.groupId } ?: return@mapNotNull null
                request.toResponse(group.name)
            },
            availableGroups = activeGroups
                .filter { it.id !in groupIds && it.id !in pendingRequestGroupIds }
                .map { group ->
                    GroupSummaryResponse(
                        id = group.id!!,
                        name = group.name,
                        description = group.description,
                        createdAt = group.createdAt,
                        membershipStatus = null,
                        currentUserAdmin = false,
                        memberCount = activeMemberCount(group.id)
                    )
                }
        )
    }

    @Transactional
    fun createGroup(request: CreateGroupRequest, currentUser: CurrentUser): GroupResponse {
        val now = Instant.now(clock)
        val name = request.name.requireField("name", "Bitte geben Sie einen Gruppennamen ein")
        val normalizedName = name.normalizeName()

        if (groupStore.findActiveByNormalizedName(normalizedName) != null) {
            throw GroupException(HttpStatus.CONFLICT, "GROUP_NAME_ALREADY_EXISTS", "Gruppenname bereits vergeben", "name")
        }

        val group = groupStore.save(
            Group(
                name = name,
                normalizedName = normalizedName,
                description = request.description.normalizeOptional(),
                createdByUserId = currentUser.userId,
                createdAt = now,
                updatedAt = now
            )
        )

        membershipStore.save(
            GroupMembership(
                groupId = group.id!!,
                userId = currentUser.userId,
                inviteEmail = currentUser.email,
                normalizedInviteEmail = currentUser.email.normalizeEmail(),
                displayName = currentUser.nickname,
                status = GroupMembershipStatus.ACTIVE,
                isAdmin = true,
                createdAt = now,
                updatedAt = now,
                joinedAt = now
            )
        )

        return getGroup(group.id, currentUser)
    }

    @Transactional
    fun getGroup(groupId: Long, currentUser: CurrentUser): GroupResponse {
        claimPendingInvitations(currentUser)
        val group = requireGroup(groupId)
        val currentMembership = membershipStore.findByGroupIdAndUserId(groupId, currentUser.userId)
            ?.takeIf { it.status == GroupMembershipStatus.ACTIVE || it.status == GroupMembershipStatus.INVITED }
            ?: throw GroupException(HttpStatus.FORBIDDEN, "FORBIDDEN_GROUP_MEMBER", "Gruppendetails koennen nur fuer Mitglieder angezeigt werden")
        val memberships = membershipStore.findByGroupId(groupId)
            .filter { it.status == GroupMembershipStatus.ACTIVE || it.status == GroupMembershipStatus.INVITED }
            .sortedWith(compareByDescending<GroupMembership> { it.isAdmin }.thenBy { it.displayName.lowercase() })
        val isAdmin = currentMembership.isAdmin && currentMembership.status == GroupMembershipStatus.ACTIVE
        val invitations = if (isAdmin) invitationStore.findByGroupId(groupId) else emptyList()
        val joinRequests = if (isAdmin) {
            joinRequestStore.findByGroupId(groupId).filter { it.status == GroupJoinRequestStatus.PENDING }
        } else {
            emptyList()
        }
        val tokens = if (isAdmin) tokenStore.findByGroupId(groupId) else emptyList()

        return GroupResponse(
            id = group.id!!,
            name = group.name,
            description = group.description,
            createdAt = group.createdAt,
            updatedAt = group.updatedAt,
            currentMembershipId = currentMembership.id,
            currentMembershipStatus = currentMembership.status,
            currentUserAdmin = isAdmin,
            members = memberships.map { it.toResponse() },
            invitations = invitations.map { it.toResponse() },
            joinRequests = joinRequests.map { it.toResponse(group.name) },
            tokens = tokens.map { it.toResponse() }
        )
    }

    @Transactional
    fun updateGroup(groupId: Long, request: UpdateGroupRequest, currentUser: CurrentUser): GroupResponse {
        val adminMembership = requireAdminMembership(groupId, currentUser.userId)
        val group = requireGroup(groupId)
        val now = Instant.now(clock)
        val name = request.name.requireField("name", "Bitte geben Sie einen Gruppennamen ein")
        val normalizedName = name.normalizeName()
        val conflicting = groupStore.findActiveByNormalizedName(normalizedName)
        if (conflicting != null && conflicting.id != groupId) {
            throw GroupException(HttpStatus.CONFLICT, "GROUP_NAME_ALREADY_EXISTS", "Gruppenname bereits vergeben", "name")
        }

        groupStore.save(
            group.copy(
                name = name,
                normalizedName = normalizedName,
                description = request.description.normalizeOptional(),
                updatedAt = now
            )
        )

        return getGroup(groupId, currentUser.copy(nickname = adminMembership.displayName))
    }

    @Transactional
    fun deleteGroup(groupId: Long, currentUser: CurrentUser) {
        requireAdminMembership(groupId, currentUser.userId)
        val group = requireGroup(groupId)
        groupStore.save(group.copy(deletedAt = Instant.now(clock), updatedAt = Instant.now(clock)))
    }

    @Transactional
    fun inviteMember(groupId: Long, request: InviteGroupMemberRequest, currentUser: CurrentUser): GroupResponse {
        requireAdminMembership(groupId, currentUser.userId)
        val group = requireGroup(groupId)
        val target = request.nicknameOrEmail.requireField("nicknameOrEmail", "Bitte Nickname oder Email-Adresse eingeben")
        val now = Instant.now(clock)
        val byEmail = emailRegex.matches(target)
        val normalizedEmail = target.normalizeEmail()
        val appUser = if (byEmail) appUserStore.findByEmail(target.trim()) else appUserStore.findByNickname(target.trim())

        if (!byEmail && appUser == null) {
            throw GroupException(HttpStatus.BAD_REQUEST, "INVITEE_NOT_FOUND", "Nickname ist unbekannt", "nicknameOrEmail")
        }

        val knownUserId = appUser?.keycloakUserId
        val knownUserEmail = appUser?.email
        ensureNoOpenRelation(
            groupId = groupId,
            userId = knownUserId,
            normalizedEmail = (knownUserEmail ?: normalizedEmail)
        )

        val membership = membershipStore.save(
            GroupMembership(
                groupId = groupId,
                userId = knownUserId,
                inviteEmail = knownUserEmail ?: target.takeIf { byEmail }?.trim(),
                normalizedInviteEmail = (knownUserEmail ?: target.takeIf { byEmail })?.normalizeEmail(),
                displayName = appUser?.nickname ?: target.trim(),
                status = GroupMembershipStatus.INVITED,
                isAdmin = false,
                createdAt = now,
                updatedAt = now
            )
        )

        invitationStore.save(
            GroupInvitation(
                groupId = groupId,
                membershipId = membership.id,
                tokenId = null,
                invitedByUserId = currentUser.userId,
                channel = if (byEmail) GroupInvitationChannel.EMAIL else GroupInvitationChannel.NICKNAME,
                mailType = if (appUser != null) GroupInvitationMailType.KNOWN_USER else GroupInvitationMailType.UNKNOWN_EMAIL,
                targetLabel = target.trim(),
                targetEmail = knownUserEmail ?: target.takeIf { byEmail }?.trim(),
                normalizedTargetEmail = (knownUserEmail ?: target.takeIf { byEmail })?.normalizeEmail(),
                createdAt = now
            )
        )

        if (appUser != null && appUser.email.isNotBlank()) {
            groupMailService.sendKnownUserInvitation(appUser.email, appUser.nickname, group.name, currentUser.nickname)
        } else if (byEmail) {
            groupMailService.sendUnknownEmailInvitation(target.trim(), group.name, currentUser.nickname)
        }

        return getGroup(groupId, currentUser)
    }

    @Transactional(readOnly = true)
    fun inviteSuggestions(groupId: Long, query: String?, currentUser: CurrentUser): List<GroupInviteSuggestionResponse> {
        requireAdminMembership(groupId, currentUser.userId)
        requireGroup(groupId)
        val blockedUserIds = membershipStore.findByGroupId(groupId)
            .asSequence()
            .filter { it.status == GroupMembershipStatus.ACTIVE || it.status == GroupMembershipStatus.INVITED }
            .mapNotNull { it.userId }
            .toSet()

        return appUserStore.searchInviteSuggestions(query.orEmpty(), currentUser.userId, inviteSuggestionLimit * 2)
            .asSequence()
            .filterNot { it.keycloakUserId in blockedUserIds }
            .take(inviteSuggestionLimit)
            .map { user ->
                GroupInviteSuggestionResponse(
                    userId = user.keycloakUserId,
                    nickname = user.nickname,
                    email = user.email
                )
            }
            .toList()
    }

    @Transactional
    fun acceptInvitation(groupId: Long, membershipId: Long, currentUser: CurrentUser): GroupResponse {
        val membership = requireMembership(groupId, membershipId)
        if (membership.userId != currentUser.userId || membership.status != GroupMembershipStatus.INVITED) {
            throw GroupException(HttpStatus.FORBIDDEN, "FORBIDDEN_GROUP_MEMBER", "Einladung kann nicht angenommen werden")
        }

        val now = Instant.now(clock)
        membershipStore.save(
            membership.copy(
                status = GroupMembershipStatus.ACTIVE,
                updatedAt = now,
                joinedAt = now
            )
        )

        return getGroup(groupId, currentUser)
    }

    @Transactional
    fun removeMember(groupId: Long, membershipId: Long, currentUser: CurrentUser): GroupResponse {
        requireAdminMembership(groupId, currentUser.userId)
        val membership = requireMembership(groupId, membershipId)
        if (membership.userId == currentUser.userId) {
            throw GroupException(HttpStatus.BAD_REQUEST, "LAST_ADMIN_REQUIRED", "Verwende zum Verlassen der Gruppe die Funktion Gruppe verlassen")
        }
        if (membership.isAdmin && membership.status == GroupMembershipStatus.ACTIVE && activeAdminCount(groupId) <= 1) {
            throw GroupException(HttpStatus.BAD_REQUEST, "LAST_ADMIN_REQUIRED", "Mindestens ein Gruppenverwalter muss erhalten bleiben")
        }

        val now = Instant.now(clock)
        membershipStore.save(
            membership.copy(
                status = GroupMembershipStatus.REMOVED,
                updatedAt = now,
                removedAt = now
            )
        )

        return getGroup(groupId, currentUser)
    }

    @Transactional
    fun requestMembership(groupId: Long, request: CreateMembershipRequest, currentUser: CurrentUser): GroupListResponse {
        requireGroup(groupId)
        ensureNoOpenRelation(groupId, currentUser.userId, currentUser.email.normalizeEmail())
        if (joinRequestStore.findPendingByGroupIdAndUserId(groupId, currentUser.userId) != null) {
            throw GroupException(HttpStatus.CONFLICT, "REQUEST_ALREADY_EXISTS", "Mitgliedschaftsantrag existiert bereits")
        }

        joinRequestStore.save(
            GroupJoinRequest(
                groupId = groupId,
                requestedByUserId = currentUser.userId,
                requestedByDisplayName = currentUser.nickname,
                status = GroupJoinRequestStatus.PENDING,
                comment = request.comment.normalizeOptional(),
                reviewComment = null,
                reviewedByUserId = null,
                createdAt = Instant.now(clock)
            )
        )

        return list(currentUser)
    }

    @Transactional
    fun approveJoinRequest(groupId: Long, requestId: Long, decision: MembershipDecisionRequest, currentUser: CurrentUser): GroupResponse {
        requireAdminMembership(groupId, currentUser.userId)
        val joinRequest = requireJoinRequest(groupId, requestId)
        if (joinRequest.status != GroupJoinRequestStatus.PENDING) {
            return getGroup(groupId, currentUser)
        }
        val now = Instant.now(clock)

        val existingMembership = membershipStore.findByGroupIdAndUserId(groupId, joinRequest.requestedByUserId)
        if (existingMembership != null && existingMembership.status == GroupMembershipStatus.INVITED) {
            membershipStore.save(
                existingMembership.copy(
                    status = GroupMembershipStatus.ACTIVE,
                    updatedAt = now,
                    joinedAt = now
                )
            )
        } else {
            val user = appUserStore.findById(joinRequest.requestedByUserId)
            membershipStore.save(
                GroupMembership(
                    groupId = groupId,
                    userId = joinRequest.requestedByUserId,
                    inviteEmail = user?.email,
                    normalizedInviteEmail = user?.email.normalizeEmail(),
                    displayName = user?.nickname ?: joinRequest.requestedByDisplayName,
                    status = GroupMembershipStatus.ACTIVE,
                    isAdmin = false,
                    createdAt = now,
                    updatedAt = now,
                    joinedAt = now
                )
            )
        }

        joinRequestStore.save(
            joinRequest.copy(
                status = GroupJoinRequestStatus.APPROVED,
                reviewComment = decision.comment.normalizeOptional(),
                reviewedByUserId = currentUser.userId,
                reviewedAt = now
            )
        )

        return getGroup(groupId, currentUser)
    }

    @Transactional
    fun rejectJoinRequest(groupId: Long, requestId: Long, decision: MembershipDecisionRequest, currentUser: CurrentUser): GroupResponse {
        requireAdminMembership(groupId, currentUser.userId)
        val joinRequest = requireJoinRequest(groupId, requestId)
        if (joinRequest.status == GroupJoinRequestStatus.PENDING) {
            joinRequestStore.save(
                joinRequest.copy(
                    status = GroupJoinRequestStatus.REJECTED,
                    reviewComment = decision.comment.normalizeOptional(),
                    reviewedByUserId = currentUser.userId,
                    reviewedAt = Instant.now(clock)
                )
            )
        }

        return getGroup(groupId, currentUser)
    }

    @Transactional
    fun createInvitationToken(groupId: Long, currentUser: CurrentUser): GroupTokenResponse {
        requireAdminMembership(groupId, currentUser.userId)
        val now = Instant.now(clock)
        val token = tokenService.generateToken()
        val savedToken = tokenStore.save(
            GroupInvitationToken(
                groupId = groupId,
                createdByUserId = currentUser.userId,
                tokenHash = tokenService.hash(token),
                createdAt = now,
                expiresAt = now.plus(groupProperties.invitationTtl)
            )
        )

        invitationStore.save(
            GroupInvitation(
                groupId = groupId,
                membershipId = null,
                tokenId = savedToken.id,
                invitedByUserId = currentUser.userId,
                channel = GroupInvitationChannel.TOKEN,
                mailType = GroupInvitationMailType.TOKEN,
                targetLabel = "Gruppeneinladungstoken",
                targetEmail = null,
                normalizedTargetEmail = null,
                createdAt = now
            )
        )

        return savedToken.toResponse(token)
    }

    @Transactional
    fun joinByToken(request: JoinGroupByTokenRequest, currentUser: CurrentUser): GroupResponse {
        claimPendingInvitations(currentUser)
        val token = request.token.requireField("token", "Bitte Gruppeneinladungstoken eingeben")
        val now = Instant.now(clock)
        val invitationToken = tokenStore.findByTokenHash(tokenService.hash(token))
            ?: throw GroupException(HttpStatus.BAD_REQUEST, "INVALID_GROUP_TOKEN", "Gruppeneinladungstoken ist ungueltig", "token")

        if (invitationToken.usedAt != null) {
            throw GroupException(HttpStatus.BAD_REQUEST, "INVALID_GROUP_TOKEN", "Gruppeneinladungstoken ist ungueltig", "token")
        }
        if (invitationToken.expiresAt.isBefore(now)) {
            throw GroupException(HttpStatus.BAD_REQUEST, "TOKEN_EXPIRED", "Gruppeneinladungstoken ist abgelaufen", "token")
        }

        val groupId = invitationToken.groupId
        val existingMembership = membershipStore.findByGroupIdAndUserId(groupId, currentUser.userId)
        if (existingMembership != null && existingMembership.status == GroupMembershipStatus.ACTIVE) {
            throw GroupException(HttpStatus.CONFLICT, "MEMBER_ALREADY_EXISTS", "Mitglied existiert bereits")
        }
        if (joinRequestStore.findPendingByGroupIdAndUserId(groupId, currentUser.userId) != null) {
            throw GroupException(HttpStatus.CONFLICT, "REQUEST_ALREADY_EXISTS", "Mitgliedschaftsantrag existiert bereits")
        }

        if (existingMembership != null && existingMembership.status == GroupMembershipStatus.INVITED) {
            membershipStore.save(
                existingMembership.copy(
                    status = GroupMembershipStatus.ACTIVE,
                    updatedAt = now,
                    joinedAt = now
                )
            )
        } else {
            membershipStore.save(
                GroupMembership(
                    groupId = groupId,
                    userId = currentUser.userId,
                    inviteEmail = currentUser.email,
                    normalizedInviteEmail = currentUser.email.normalizeEmail(),
                    displayName = currentUser.nickname,
                    status = GroupMembershipStatus.ACTIVE,
                    isAdmin = false,
                    createdAt = now,
                    updatedAt = now,
                    joinedAt = now
                )
            )
        }

        tokenStore.save(invitationToken.copy(usedAt = now, usedByUserId = currentUser.userId))
        invitationStore.findByTokenId(invitationToken.id!!).forEach { invitation ->
            invitationStore.save(invitation.copy(claimedAt = now, claimedByUserId = currentUser.userId))
        }

        return getGroup(groupId, currentUser)
    }

    @Transactional
    fun leaveGroup(groupId: Long, currentUser: CurrentUser): GroupListResponse {
        val membership = membershipStore.findByGroupIdAndUserId(groupId, currentUser.userId)
            ?: throw GroupException(HttpStatus.NOT_FOUND, "GROUP_NOT_FOUND", "Gruppe nicht gefunden")
        if (membership.isAdmin && membership.status == GroupMembershipStatus.ACTIVE && activeAdminCount(groupId) <= 1) {
            throw GroupException(HttpStatus.BAD_REQUEST, "LAST_ADMIN_REQUIRED", "Mindestens ein Gruppenverwalter muss erhalten bleiben")
        }
        val now = Instant.now(clock)
        membershipStore.save(
            membership.copy(
                status = GroupMembershipStatus.LEFT,
                updatedAt = now,
                leftAt = now
            )
        )

        return list(currentUser)
    }

    @Transactional
    fun grantAdmin(groupId: Long, membershipId: Long, currentUser: CurrentUser): GroupResponse {
        requireAdminMembership(groupId, currentUser.userId)
        val membership = requireMembership(groupId, membershipId)
        if (membership.status != GroupMembershipStatus.ACTIVE) {
            throw GroupException(HttpStatus.BAD_REQUEST, "MEMBER_ALREADY_EXISTS", "Adminrechte koennen nur aktiven Mitgliedern gegeben werden")
        }
        membershipStore.save(membership.copy(isAdmin = true, updatedAt = Instant.now(clock)))
        return getGroup(groupId, currentUser)
    }

    @Transactional
    fun revokeAdmin(groupId: Long, membershipId: Long, currentUser: CurrentUser): GroupResponse {
        requireAdminMembership(groupId, currentUser.userId)
        val membership = requireMembership(groupId, membershipId)
        if (membership.status != GroupMembershipStatus.ACTIVE) {
            throw GroupException(HttpStatus.BAD_REQUEST, "MEMBER_ALREADY_EXISTS", "Adminrechte koennen nur aktiven Mitgliedern entzogen werden")
        }
        if (membership.isAdmin && membership.status == GroupMembershipStatus.ACTIVE && activeAdminCount(groupId) <= 1) {
            throw GroupException(HttpStatus.BAD_REQUEST, "LAST_ADMIN_REQUIRED", "Mindestens ein Gruppenverwalter muss erhalten bleiben")
        }
        membershipStore.save(membership.copy(isAdmin = false, updatedAt = Instant.now(clock)))
        return getGroup(groupId, currentUser)
    }

    private fun requireGroup(groupId: Long): Group =
        groupStore.findActiveById(groupId)
            ?: throw GroupException(HttpStatus.NOT_FOUND, "GROUP_NOT_FOUND", "Gruppe nicht gefunden")

    private fun requireMembership(groupId: Long, membershipId: Long): GroupMembership {
        val membership = membershipStore.findById(membershipId)
            ?: throw GroupException(HttpStatus.NOT_FOUND, "GROUP_NOT_FOUND", "Mitgliedschaft nicht gefunden")
        if (membership.groupId != groupId) {
            throw GroupException(HttpStatus.NOT_FOUND, "GROUP_NOT_FOUND", "Mitgliedschaft nicht gefunden")
        }
        return membership
    }

    private fun requireJoinRequest(groupId: Long, requestId: Long): GroupJoinRequest {
        val request = joinRequestStore.findById(requestId)
            ?: throw GroupException(HttpStatus.NOT_FOUND, "GROUP_NOT_FOUND", "Mitgliedschaftsantrag nicht gefunden")
        if (request.groupId != groupId) {
            throw GroupException(HttpStatus.NOT_FOUND, "GROUP_NOT_FOUND", "Mitgliedschaftsantrag nicht gefunden")
        }
        return request
    }

    private fun requireAdminMembership(groupId: Long, userId: String): GroupMembership {
        val membership = membershipStore.findByGroupIdAndUserId(groupId, userId)
            ?: throw GroupException(HttpStatus.FORBIDDEN, "FORBIDDEN_GROUP_ADMIN", "Aktion ist nur fuer Gruppenverwalter erlaubt")
        if (!membership.isAdmin || membership.status != GroupMembershipStatus.ACTIVE) {
            throw GroupException(HttpStatus.FORBIDDEN, "FORBIDDEN_GROUP_ADMIN", "Aktion ist nur fuer Gruppenverwalter erlaubt")
        }
        return membership
    }

    private fun activeAdminCount(groupId: Long): Int =
        membershipStore.findByGroupId(groupId).count { it.status == GroupMembershipStatus.ACTIVE && it.isAdmin }

    private fun activeMemberCount(groupId: Long): Int =
        membershipStore.findByGroupId(groupId).count { it.status == GroupMembershipStatus.ACTIVE }

    private fun claimPendingInvitations(currentUser: CurrentUser) {
        val normalizedEmail = currentUser.email.normalizeEmail() ?: return
        val now = Instant.now(clock)
        membershipStore.findInvitedByEmailWithoutUser(normalizedEmail)
            .filter { groupStore.findActiveById(it.groupId) != null }
            .forEach { membership ->
                membershipStore.save(
                    membership.copy(
                        userId = currentUser.userId,
                        displayName = currentUser.nickname,
                        updatedAt = now
                    )
                )
                invitationStore.findByMembershipId(membership.id!!).forEach { invitation ->
                    if (invitation.claimedByUserId == null) {
                        invitationStore.save(
                            invitation.copy(
                                claimedAt = now,
                                claimedByUserId = currentUser.userId
                            )
                        )
                    }
                }
            }
    }

    private fun ensureNoOpenRelation(groupId: Long, userId: String?, normalizedEmail: String?) {
        if (userId != null) {
            val membership = membershipStore.findByGroupIdAndUserId(groupId, userId)
            if (membership != null && (membership.status == GroupMembershipStatus.ACTIVE || membership.status == GroupMembershipStatus.INVITED)) {
                throw GroupException(HttpStatus.CONFLICT, "MEMBER_ALREADY_EXISTS", "Mitglied existiert bereits")
            }
        }
        if (normalizedEmail != null) {
            val invited = membershipStore.findByGroupIdAndNormalizedInviteEmailAndStatuses(
                groupId,
                normalizedEmail,
                setOf(GroupMembershipStatus.INVITED)
            )
            if (invited.isNotEmpty()) {
                throw GroupException(HttpStatus.CONFLICT, "INVITATION_ALREADY_EXISTS", "Mitglied existiert bereits")
            }
        }
    }

    private fun Group.toSummary(membership: GroupMembership, memberCount: Int): GroupSummaryResponse =
        GroupSummaryResponse(
            id = id!!,
            name = name,
            description = description,
            createdAt = createdAt,
            membershipStatus = membership.status,
            currentUserAdmin = membership.isAdmin && membership.status == GroupMembershipStatus.ACTIVE,
            memberCount = memberCount
        )

    private fun GroupMembership.toResponse(): GroupMemberResponse =
        GroupMemberResponse(
            id = id!!,
            userId = userId,
            displayName = displayName,
            inviteEmail = inviteEmail,
            status = status,
            admin = isAdmin,
            createdAt = createdAt,
            joinedAt = joinedAt
        )

    private fun GroupInvitation.toResponse(): GroupInvitationResponse =
        GroupInvitationResponse(
            id = id!!,
            membershipId = membershipId,
            channel = channel,
            mailType = mailType,
            targetLabel = targetLabel,
            targetEmail = targetEmail,
            createdAt = createdAt,
            claimedAt = claimedAt
        )

    private fun GroupInvitationToken.toResponse(token: String? = null): GroupTokenResponse =
        GroupTokenResponse(
            id = id!!,
            token = token,
            createdAt = createdAt,
            expiresAt = expiresAt,
            usedAt = usedAt
        )

    private fun GroupJoinRequest.toResponse(groupName: String): GroupJoinRequestResponse =
        GroupJoinRequestResponse(
            id = id!!,
            groupId = groupId,
            groupName = groupName,
            requestedByUserId = requestedByUserId,
            requestedByDisplayName = requestedByDisplayName,
            status = status,
            comment = comment,
            reviewComment = reviewComment,
            createdAt = createdAt,
            reviewedAt = reviewedAt
        )

    private fun String?.requireField(field: String, message: String): String {
        val value = this?.trim()
        if (!StringUtils.hasText(value)) {
            throw GroupException(HttpStatus.BAD_REQUEST, "FIELD_REQUIRED", message, field)
        }
        return value!!
    }

    private fun String?.normalizeOptional(): String? = this?.trim()?.takeIf(StringUtils::hasText)

    private fun String.normalizeName(): String = trim().lowercase()

    private fun String?.normalizeEmail(): String? = this?.trim()?.lowercase()?.takeIf(StringUtils::hasText)
}
