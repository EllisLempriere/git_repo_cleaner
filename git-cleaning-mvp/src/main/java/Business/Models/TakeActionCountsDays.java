package Business.Models;

public record TakeActionCountsDays(int staleBranchInactivityDays, int staleTagDays, int notificationBeforeActionDays) {
}
