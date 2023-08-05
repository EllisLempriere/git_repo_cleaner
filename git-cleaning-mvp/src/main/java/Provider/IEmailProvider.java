package Provider;

import Business.Models.SendEmailException;

public interface IEmailProvider {

    void sendEmail(String email, String subject, String body) throws SendEmailException;
}
