package Provider;

import Business.Models.SendEmailException;

public class EmailProvider implements IEmailProvider {

    @Override
    public void sendEmail(String to, String subject, String body) throws SendEmailException {
        if (to == null || subject == null || body == null)
            throw new SendEmailException("Cannot have null parameters for email");
    }
}
