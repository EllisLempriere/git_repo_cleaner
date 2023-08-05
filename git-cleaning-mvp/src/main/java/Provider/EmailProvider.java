package Provider;

import Business.Models.SendEmailException;

public class EmailProvider implements IEmailProvider {

    @Override
    public void sendEmail(String email, String subject, String body) throws SendEmailException {
        if (email == null || subject == null || body == null)
            throw new SendEmailException("Could not send email");
        // create pseudo logger - write to file email info. Filename: "time_subject"
    }
}
