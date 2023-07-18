import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Properties;

public class JavaEmailApiTest {

    public static void main(String[] args) throws MessagingException {
        Properties props = new Properties();
        Session session = Session.getDefaultInstance(props, null);

        MimeMessage message = new MimeMessage(session);
        message.setFrom(new InternetAddress("ellis@pslfamily.org"));
        message.addRecipient(Message.RecipientType.TO, new InternetAddress("ellis@pslfamily.org"));
        message.setSubject("Test email");
        message.setText("Test email body text");
    }
}
