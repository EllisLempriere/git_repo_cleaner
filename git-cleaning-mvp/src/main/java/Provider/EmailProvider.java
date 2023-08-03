package Provider;

import Business.Models.NotificationMessage;
import Business.Models.SendEmailException;

public class EmailProvider implements IEmailProvider {


    @Override
    public void sendNotificationEmail(NotificationMessage message) throws SendEmailException {
        if (message == null)
            throw new SendEmailException("Failed to send email");
    }
}
