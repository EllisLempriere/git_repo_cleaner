package Business;

import Business.Models.Branch;
import Business.Models.SendEmailException;
import Business.Models.Tag;

public interface INotificationLogic {

    void sendNotificationPendingArchival(Branch branch, String repoId) throws SendEmailException;

    void sendNotificationArchival(Branch branch, Tag tag, String repoId) throws SendEmailException;

    void sendNotificationPendingTagDeletion(Tag tag, String repoId) throws SendEmailException;

    void sendNotificationTagDeletion(Tag tag, String repoId) throws SendEmailException;
}
