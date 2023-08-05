package Business.Models;

public record DaysToActions(int daysToStaleBranch, int daysToStaleTag, int precedingDaysToWarn) {
}
