import { authenticatedBackendFetch } from "@/lib/authenticated-backend-client";

export type ActivityResponseStatus = "OPEN" | "ACCEPTED" | "DECLINED" | "MAYBE";

export type ActivityError = {
  code: string;
  message: string;
  field?: string | null;
};

export type ActivityParticipantCounts = {
  open: number;
  accepted: number;
  declined: number;
  maybe: number;
};

export type ActivityParticipant = {
  id: number;
  groupMembershipId: number;
  userId?: string | null;
  displayName: string;
  inviteEmail?: string | null;
  admin: boolean;
  membershipStatus: "INVITED" | "ACTIVE" | "DECLINED" | "LEFT" | "REMOVED";
  responseStatus: ActivityResponseStatus;
  responseNote?: string | null;
  respondedAt?: string | null;
  createdAt: string;
  removedAt?: string | null;
};

export type ActivitySummary = {
  id: number;
  groupId: number;
  groupName: string;
  description: string;
  location: string;
  scheduledAt: string;
  currentUserResponseStatus?: ActivityResponseStatus | null;
  currentUserCanManage: boolean;
  currentUserCanRespond: boolean;
  participantCounts: ActivityParticipantCounts;
};

export type ActivityDetail = {
  id: number;
  groupId: number;
  groupName: string;
  description: string;
  details?: string | null;
  location: string;
  scheduledAt: string;
  createdAt: string;
  updatedAt: string;
  currentUserParticipantId?: number | null;
  currentUserResponseStatus?: ActivityResponseStatus | null;
  currentUserCanManage: boolean;
  currentUserCanRespond: boolean;
  participantCounts: ActivityParticipantCounts;
  participants: ActivityParticipant[];
};

export type ActivityListResponse = {
  activities: ActivitySummary[];
};

export type CreateActivityPayload = {
  description: string;
  details?: string;
  location: string;
  scheduledAt: string;
};

export type UpdateActivityPayload = CreateActivityPayload;

export type ActivityResponsePayload = {
  responseStatus: ActivityResponseStatus;
  responseNote?: string;
};

export type AddActivityParticipantPayload = {
  groupMembershipId: number;
};

export function formatActivityStatus(status: ActivityResponseStatus | null | undefined) {
  switch (status) {
    case "ACCEPTED":
      return "zugesagt";
    case "DECLINED":
      return "abgesagt";
    case "MAYBE":
      return "weiss ich noch nicht";
    case "OPEN":
      return "offen";
    default:
      return "nicht zugewiesen";
  }
}

export function activityStatusBadgeClass(status: ActivityResponseStatus | null | undefined) {
  switch (status) {
    case "ACCEPTED":
      return "badge-success";
    case "DECLINED":
      return "badge-error";
    case "MAYBE":
      return "badge-warning";
    case "OPEN":
      return "badge-info";
    default:
      return "badge-neutral";
  }
}

export function formatActivityDateTime(value: string) {
  return new Intl.DateTimeFormat("de-DE", {
    dateStyle: "medium",
    timeStyle: "short"
  }).format(new Date(value));
}

export function toDateInputValue(value: string) {
  const date = new Date(value);
  const year = date.getFullYear();
  const month = `${date.getMonth() + 1}`.padStart(2, "0");
  const day = `${date.getDate()}`.padStart(2, "0");
  return `${year}-${month}-${day}`;
}

export function toTimeInputValue(value: string) {
  const date = new Date(value);
  const hours = `${date.getHours()}`.padStart(2, "0");
  const minutes = `${date.getMinutes()}`.padStart(2, "0");
  return `${hours}:${minutes}`;
}

export function combineDateTimeToIso(date: string, time: string) {
  return new Date(`${date}T${time}`).toISOString();
}

export async function fetchGroupActivities(groupId: number) {
  const response = await authenticatedBackendFetch(`/api/private/groups/${groupId}/activities`, { method: "GET" });

  if (!response.ok) {
    throw new Error("Aktivitaeten konnten nicht geladen werden");
  }

  return (await response.json()) as ActivityListResponse;
}
