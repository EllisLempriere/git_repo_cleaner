package Business;

import Business.Models.*;
import Provider.IEmailProvider;

import java.util.Collections;
import java.util.List;

public class NotificationLogic implements INotificationLogic {

    private final IEmailProvider EMAIL_PROVIDER;

    public NotificationLogic(IEmailProvider emailProvider) {
        this.EMAIL_PROVIDER = emailProvider;
    }

    @Override
    public void sendNotificationPendingArchival(Branch branch) throws SendEmailException {
        String authorEmail = branch.commits().get(0).authorEmail();
        List<String> recipients = Collections.singletonList(authorEmail);

        String subject = "Pending archival of branch " + branch.name();

        String body = String.format("Branch %s will be archived soon. Commit to it again to prevent archival.",
                branch.name());

        for (String author : recipients)
            EMAIL_PROVIDER.sendEmail(author, subject, body);
    }

    @Override
    public void sendNotificationArchival(Branch branch, Tag tag) throws SendEmailException {
        String authorEmail = branch.commits().get(0).authorEmail();
        List<String> recipients = Collections.singletonList(authorEmail);

        String subject = "Archival of branch " + branch.name();

        String body = String.format("Branch %s has been archived as tag %s." +
                "Checkout the tag and recreate the branch to revive it.",
                branch.name(), tag.name());

        for (String author : recipients)
            EMAIL_PROVIDER.sendEmail(author, subject, body);
    }

    @Override
    public void sendNotificationPendingTagDeletion(Tag tag) throws SendEmailException {
        String authorEmail = tag.commit().authorEmail();
        List<String> recipients = Collections.singletonList(authorEmail);

        String subject = "Pending deletion of archive tag " + tag.name();

        String body = String.format("Archive tag %s will be deleted in soon. " +
                "Create a new tag or branch on archive tag or commits will be lost.",
                tag.name());

        for (String author : recipients)
            EMAIL_PROVIDER.sendEmail(author, subject, body);
    }

    @Override
    public void sendNotificationTagDeletion(Tag tag) throws SendEmailException {
        String authorEmail = tag.commit().authorEmail();
        List<String> recipients = Collections.singletonList(authorEmail);

        String subject = "Deletion of archive tag " + tag.name();

        String body = String.format("Archive tag %s has been deleted, commits no longer guaranteed to be accessible",
                tag.name());

        for (String author : recipients)
            EMAIL_PROVIDER.sendEmail(author, subject, body);
    }
}
