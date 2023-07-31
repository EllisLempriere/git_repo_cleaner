package Business;

public class EmailHandler implements IEmailHandler {

    private final int PRECEDING_DAYS_TO_WARN;

    public EmailHandler(int precedingDaysToWarn) {
        this.PRECEDING_DAYS_TO_WARN = precedingDaysToWarn;
    }


    @Override
    public Email buildPendingArchivalEmail(Branch branch) {
        return new Email(branch.commits().get(0).author(),
                String.format("Business.Branch %s will be archived in %d days", branch.name(), PRECEDING_DAYS_TO_WARN));
    }

    @Override
    public Email buildArchivalEmail(Branch branch, Tag tag) {
        return new Email(branch.commits().get(0).author(),
                String.format("Business.Branch %s archived as %s", branch.name(), tag.name()));
    }

    @Override
    public Email buildPendingTagDeletionEmail(Tag tag) {
        return new Email(tag.commit().author(),
                String.format("Archive tag %s will be deleted in %d days", tag.name(), PRECEDING_DAYS_TO_WARN));
    }

    @Override
    public Email buildTagDeletionEmail(Tag tag) {
        return new Email(tag.commit().author(),
                String.format("Archive tag %s deleted", tag.name()));
    }

    @Override
    public void sendEmail(Email email) throws SendEmailException {
        if (email == null)
            throw new SendEmailException("Failed to send email");
    }
}
