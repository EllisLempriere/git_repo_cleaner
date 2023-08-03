package Business;

import Business.Models.Branch;
import Business.Models.SendEmailException;
import Business.Models.Tag;

public interface INotificationLogic {

    void sendNotificationPendingArchival(Branch branch) throws SendEmailException;

    void sendNotificationArchival(Branch branch, Tag tag) throws SendEmailException;

    void sendNotificationPendingTagDeletion(Tag tag) throws SendEmailException;

    void sendNotificationTagDeletion(Tag tag) throws SendEmailException;
}
