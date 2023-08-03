package Provider;

import Business.Models.NotificationMessage;
import Business.Models.SendEmailException;

public interface IEmailProvider {

    void sendNotificationEmail(NotificationMessage message) throws SendEmailException;
}
