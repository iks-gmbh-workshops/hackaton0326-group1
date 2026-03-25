export type GroupMembershipStatus = "INVITED" | "ACTIVE" | "LEFT" | "REMOVED";
export type GroupInvitationChannel = "NICKNAME" | "EMAIL" | "TOKEN";
export type GroupInvitationMailType = "KNOWN_USER" | "UNKNOWN_EMAIL" | "TOKEN";
export type GroupJoinRequestStatus = "PENDING" | "APPROVED" | "REJECTED";

export type GroupError = {
  code: string;
  message: string;
  field?: string | null;
};

export type GroupSummary = {
  id: number;
  name: string;
  description?: string | null;
  createdAt: string;
  membershipStatus?: GroupMembershipStatus | null;
  isCurrentUserAdmin: boolean;
  memberCount: number;
};

export type GroupInvitationSummary = {
  membershipId: number;
  groupId: number;
  groupName: string;
  displayName: string;
  inviteEmail?: string | null;
  invitedAt: string;
};

export type GroupJoinRequest = {
  id: number;
  groupId: number;
  groupName: string;
  requestedByUserId: string;
  requestedByDisplayName: string;
  status: GroupJoinRequestStatus;
  comment?: string | null;
  reviewComment?: string | null;
  createdAt: string;
  reviewedAt?: string | null;
};

export type GroupMember = {
  id: number;
  userId?: string | null;
  displayName: string;
  inviteEmail?: string | null;
  status: GroupMembershipStatus;
  isAdmin: boolean;
  createdAt: string;
  joinedAt?: string | null;
};

export type GroupInvitation = {
  id: number;
  membershipId?: number | null;
  channel: GroupInvitationChannel;
  mailType: GroupInvitationMailType;
  targetLabel: string;
  targetEmail?: string | null;
  createdAt: string;
  claimedAt?: string | null;
};

export type GroupToken = {
  id: number;
  token?: string | null;
  createdAt: string;
  expiresAt: string;
  usedAt?: string | null;
};

export type GroupDetail = {
  id: number;
  name: string;
  description?: string | null;
  createdAt: string;
  updatedAt: string;
  currentMembershipId?: number | null;
  currentMembershipStatus?: GroupMembershipStatus | null;
  isCurrentUserAdmin: boolean;
  members: GroupMember[];
  invitations: GroupInvitation[];
  joinRequests: GroupJoinRequest[];
  tokens: GroupToken[];
};

export type GroupListResponse = {
  groups: GroupSummary[];
  invitations: GroupInvitationSummary[];
  joinRequests: GroupJoinRequest[];
  availableGroups: GroupSummary[];
};

export type CreateGroupPayload = {
  name: string;
  description?: string;
};

export type UpdateGroupPayload = {
  name: string;
  description?: string;
};

export type InviteGroupMemberPayload = {
  nicknameOrEmail: string;
};

export type JoinByTokenPayload = {
  token: string;
};

export type CreateMembershipRequestPayload = {
  comment?: string;
};

export function formatDate(value: string) {
  return new Intl.DateTimeFormat("de-DE", {
    dateStyle: "medium",
    timeStyle: "short"
  }).format(new Date(value));
}

export function membershipLabel(status: GroupMembershipStatus | null | undefined) {
  switch (status) {
    case "INVITED":
      return "eingeladen";
    case "ACTIVE":
      return "aktiv";
    case "LEFT":
      return "abgemeldet";
    case "REMOVED":
      return "entfernt";
    default:
      return "offen";
  }
}
