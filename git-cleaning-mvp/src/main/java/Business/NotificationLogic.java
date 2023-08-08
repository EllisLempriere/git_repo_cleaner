package Business;

import Business.Models.*;
import Provider.IEmailProvider;

import java.util.ArrayList;
import java.util.List;

public class NotificationLogic implements INotificationLogic {

    private final IEmailProvider EMAIL_PROVIDER;
    private final List<RepoNotificationInfo> REPO_MAP;

    public NotificationLogic(IEmailProvider emailProvider, List<RepoNotificationInfo> repoNotificationInfoList) {
        if (emailProvider == null)
            throw new IllegalArgumentException("Email provider cannot be null");
        if (repoNotificationInfoList == null)
            throw new IllegalArgumentException("Repo list cannot be null");

        this.EMAIL_PROVIDER = emailProvider;
        this.REPO_MAP = repoNotificationInfoList;
    }


    @Override
    public void sendNotificationPendingArchival(Branch branch, String repoId) throws SendEmailException {
        if (branch == null)
            throw new IllegalArgumentException("Branch cannot be null");
        if (repoId == null)
            throw new IllegalArgumentException("Repo id cannot be null");

        RepoNotificationInfo info = findRepoInfo(repoId);
        List<String> recipients = getRecipientList(branch.commits(), info);

        String subject = "Pending archival of branch " + branch.name();

        String body = String.format("Branch %s will be archived in %d days. Commit to it again to prevent archival.",
                branch.name(), info.takeActionCountsDays().notificationBeforeActionDays());

        for (String r : recipients)
            EMAIL_PROVIDER.sendEmail(r, subject, body);
    }

    @Override
    public void sendNotificationArchival(Branch branch, Tag tag, String repoId) throws SendEmailException {
        if (branch == null)
            throw new IllegalArgumentException("Branch cannot be null");
        if (tag == null)
            throw new IllegalArgumentException("Tag cannot be null");
        if (repoId == null)
            throw new IllegalArgumentException("Repo id cannot be null");

        RepoNotificationInfo info = findRepoInfo(repoId);
        List<String> recipients = getRecipientList(branch.commits(), info);

        String subject = "Archival of branch " + branch.name();

        String body = String.format("Branch %s has been inactive for %d days. Branch archived as tag %s." +
                "Checkout the tag and recreate the branch to revive it.",
                branch.name(), info.takeActionCountsDays().staleBranchInactivityDays(), tag.name());

        for (String r : recipients)
            EMAIL_PROVIDER.sendEmail(r, subject, body);
    }

    @Override
    public void sendNotificationPendingTagDeletion(Tag tag, String repoId) throws SendEmailException {
        if (tag == null)
            throw new IllegalArgumentException("Tag cannot be null");
        if (repoId == null)
            throw new IllegalArgumentException("Repo id cannot be null");

        RepoNotificationInfo info = findRepoInfo(repoId);
        List<String> recipients = getRecipientList(tag.commits(), info);

        String subject = "Pending deletion of archive tag " + tag.name();

        String body = String.format("Archive tag %s will be deleted in %d days. " +
                "Create a new tag or branch on archive tag or commits will be lost.",
                tag.name(), info.takeActionCountsDays().notificationBeforeActionDays());

        for (String r : recipients)
            EMAIL_PROVIDER.sendEmail(r, subject, body);
    }

    @Override
    public void sendNotificationTagDeletion(Tag tag, String repoId) throws SendEmailException {
        if (tag == null)
            throw new IllegalArgumentException("Tag cannot be null");
        if (repoId == null)
            throw new IllegalArgumentException("Repo id cannot be null");

        RepoNotificationInfo info = findRepoInfo(repoId);
        List<String> recipients = getRecipientList(tag.commits(), info);

        String subject = "Deletion of archive tag " + tag.name();

        String body = String.format("Archive tag %s is %d days old and has been deleted," +
                "commits no longer guaranteed to be accessible",
                tag.name(), info.takeActionCountsDays().staleTagDays());

        for (String r : recipients)
            EMAIL_PROVIDER.sendEmail(r, subject, body);
    }


    private List<String> getLastThreeCommitters(List<Commit> commits) {
        List<String> distinctCommitters = new ArrayList<>();

        for (Commit c : commits) {
            if (!distinctCommitters.contains(c.authorEmail()))
                distinctCommitters.add(c.authorEmail());

            if (distinctCommitters.size() == 3)
                break;
        }

        return distinctCommitters;
    }

    private List<String> getRecipientList(List<Commit> commits, RepoNotificationInfo repoInfo) {
        List<String> lastThreeCommitters = getLastThreeCommitters(commits);
        List<String> recipients = new ArrayList<>(repoInfo.recipients());
        for (String committer : lastThreeCommitters)
            if (!recipients.contains(committer))
                recipients.add(committer);

        return recipients;
    }

    private RepoNotificationInfo findRepoInfo(String repoId) {
        for (RepoNotificationInfo repo : REPO_MAP)
            if (repo.repoId().equals(repoId)) {
                return repo;
            }

        throw new IllegalArgumentException("Repo id does not exist in given repo list");
    }
}
