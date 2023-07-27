public interface IEmailHandler {

    Email buildPendingArchivalEmail(Branch branch);

    Email buildArchivalEmail(Branch branch, Tag tag);

    Email buildPendingTagDeletionEmail(Tag tag);

    Email buildTagDeletionEmail(Tag tag);

    boolean sendEmail(Email email);
}
