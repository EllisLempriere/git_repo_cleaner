package Business.Models;

import java.util.List;

public record NotificationMessage(List<CommitAuthor> to, String subject, String body) {
}
