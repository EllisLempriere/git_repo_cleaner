package Business;

import Business.Models.Branch;
import Business.Models.NotificationMessage;
import Business.Models.SendEmailException;
import Business.Models.Tag;

public interface INotificationHandler {

    NotificationMessage buildPendingArchivalMessage(Branch branch);

    NotificationMessage buildArchivalMessage(Branch branch, Tag tag);

    NotificationMessage buildPendingTagDeletionMessage(Tag tag);

    NotificationMessage buildTagDeletionMessage(Tag tag);

    void sendMessage(NotificationMessage message) throws SendEmailException;
}
