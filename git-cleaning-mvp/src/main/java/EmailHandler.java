public class EmailHandler implements IEmailHandler {

    private final Config CONFIG;

    public EmailHandler(Config config) {
        this.CONFIG = config;
    }


    @Override
    public Email buildPendingArchivalEmail(Branch branch) {
        return new Email(branch.commits().get(0).author(),
                String.format("Branch %s will be archived in %d days", branch.name(), CONFIG.K));
    }

    @Override
    public Email buildArchivalEmail(Branch branch, Tag tag) {
        return new Email(branch.commits().get(0).author(),
                String.format("Branch %s archived as %s", branch.name(), tag.name()));
    }

    @Override
    public Email buildPendingTagDeletionEmail(Tag tag) {
        return new Email(tag.commit().author(),
                String.format("Archive tag %s will be deleted in %d days", tag.name(), CONFIG.K));
    }

    @Override
    public Email buildTagDeletionEmail(Tag tag) {
        return new Email(tag.commit().author(),
                String.format("Archive tag %s deleted", tag.name()));
    }

    @Override
    public boolean sendEmail(Email email) {
        return true;
    }
}
