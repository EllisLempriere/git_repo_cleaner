package Business;

import Business.Models.*;
import Provider.IEmailProvider;

import java.util.Collections;

public class NotificationHandler implements INotificationHandler {

    private final int DAYS_TO_STALE_BRANCH;
    private final int DAYS_TO_STALE_TAG;
    private final int PRECEDING_DAYS_TO_WARN;
    private final IEmailProvider EMAIL;

    public NotificationHandler(int daysToStaleBranch, int daysToStaleTag, int precedingDaysToWarn, IEmailProvider email) {
        this.DAYS_TO_STALE_BRANCH = daysToStaleBranch;
        this.DAYS_TO_STALE_TAG = daysToStaleTag;
        this.PRECEDING_DAYS_TO_WARN = precedingDaysToWarn;
        this.EMAIL = email;
    }

    @Override
    public NotificationMessage buildPendingArchivalMessage(Branch branch) {
        CommitAuthor author = branch.commits().get(0).author();

        String subject = "Pending archival of branch " + branch.name();

        String body = String.format("Branch %s has been inactive for %d days. Branch will be archived in %d days. " +
                "Commit to it again to prevent archival.",
                branch.name(), DAYS_TO_STALE_BRANCH - PRECEDING_DAYS_TO_WARN, PRECEDING_DAYS_TO_WARN);

        return new NotificationMessage(Collections.singletonList(author), subject, body);
    }

    @Override
    public NotificationMessage buildArchivalMessage(Branch branch, Tag tag) {
        CommitAuthor author = branch.commits().get(0).author();

        String subject = "Archival of branch " + branch.name();

        String body = String.format("Branch %s has been inactive for %d days. Branch has been archived as tag %s. " +
                "Checkout the tag and recreate the branch to revive it.",
                branch.name(), DAYS_TO_STALE_BRANCH, tag.name());

        return new NotificationMessage(Collections.singletonList(author), subject, body);
    }

    @Override
    public NotificationMessage buildPendingTagDeletionMessage(Tag tag) {
        CommitAuthor author = tag.commit().author();

        String subject = "Pending deletion of archive tag " + tag.name();

        String body = String.format("Archive tag %s has been inactive for %d days. Tag will be deleted in %d days. " +
                "Create a new tag or branch on archive tag or commits will be lost.",
                tag.name(), DAYS_TO_STALE_TAG - PRECEDING_DAYS_TO_WARN, PRECEDING_DAYS_TO_WARN);

        return new NotificationMessage(Collections.singletonList(author), subject, body);
    }

    @Override
    public NotificationMessage buildTagDeletionMessage(Tag tag) {
        CommitAuthor author = tag.commit().author();

        String subject = "Deletion of archive tag " + tag.name();

        String body = String.format("Archive tag %s has been inactive for %d days. Tag has been deleted, " +
                "commits no longer guaranteed to be accessible", tag.name(), DAYS_TO_STALE_TAG);

        return new NotificationMessage(Collections.singletonList(author), subject, body);
    }

    @Override
    public void sendMessage(NotificationMessage message) throws SendEmailException {
        EMAIL.sendNotificationEmail(message);
    }
}
