package Business.Models;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class ArchiveTagName {

    public final String name;
    public final ZonedDateTime createDate;
    public final String branchName;

    public ArchiveTagName(int epochSecondDate, String branchName) {
        StringBuilder tagName = new StringBuilder();
        tagName.append("zArchiveBranch_");

        Instant dateTime = Instant.ofEpochSecond(epochSecondDate);
        ZonedDateTime zonedDateTime = ZonedDateTime.ofInstant(dateTime, ZoneOffset.UTC);
        String formattedTime = zonedDateTime.format(DateTimeFormatter.BASIC_ISO_DATE).substring(0, 8);
        tagName.append(formattedTime);

        tagName.append("_");
        tagName.append(branchName);

        this.name = tagName.toString();
        this.createDate = zonedDateTime;
        this.branchName = branchName;
    }

    public static ArchiveTagName tryParse(String tagName) {
        if (tagName.matches("zArchiveBranch_\\d{8}_[\\w-]+")) {
            String createDateStr = tagName.substring(15, 23);
            String formattedCreateDate = createDateStr.substring(0, 4) + "-" + createDateStr.substring(4, 6) + "-" +
                    createDateStr.substring(6, 8) + "T00:00:01Z";
            int epochSecondDate = (int) Instant.parse(formattedCreateDate).getEpochSecond();

            String branchName = tagName.substring(24);

            return new ArchiveTagName(epochSecondDate, branchName);
        }

        return null;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null)
            return false;

        if (this.getClass() != obj.getClass())
            return false;

        final ArchiveTagName other = (ArchiveTagName) obj;

        if (!this.name.equals(other.name))
            return false;

        if (!this.createDate.equals(other.createDate))
            return false;

        if (!this.branchName.equals(other.branchName))
            return false;

        return true;
    }
}
