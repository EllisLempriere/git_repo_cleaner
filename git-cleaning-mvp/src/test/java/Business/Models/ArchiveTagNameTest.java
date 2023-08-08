package Business.Models;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.*;

public class ArchiveTagNameTest {

    @Test
    @DisplayName("Valid parameters, creates expected instance")
    void constructorTest1() {
        // arrange
        String expectedName = "zArchiveBranch_20230601_branch";
        ZonedDateTime expectedCreateDate = ZonedDateTime.ofInstant(Instant.parse("2023-06-01T00:00:01-07:00"),
                ZoneOffset.UTC);
        String expectedBranchName = "branch";

        // act
        ArchiveTagName actualArchiveTagName = new ArchiveTagName(1685602801, "branch");

        // assert
        assertAll(
                () -> assertEquals(expectedName, actualArchiveTagName.name),
                () -> assertEquals(expectedCreateDate, actualArchiveTagName.createDate),
                () -> assertEquals(expectedBranchName, actualArchiveTagName.branchName)
        );
    }

    @Test
    @DisplayName("Valid archive tag, returns expected tag")
    void tryParseTest1() {
        // arrange
        String expectedName = "zArchiveBranch_20230601_branch";
        ZonedDateTime expectedCreateDate = ZonedDateTime.ofInstant(Instant.parse("2023-06-01T00:00:01Z"),
                ZoneOffset.UTC);
        String expectedBranchName = "branch";

        // act
        ArchiveTagName actualArchiveTagName = ArchiveTagName.tryParse("zArchiveBranch_20230601_branch");

        // assert
        assertAll(
                () -> assertEquals(expectedName, actualArchiveTagName.name),
                () -> assertEquals(expectedCreateDate, actualArchiveTagName.createDate),
                () -> assertEquals(expectedBranchName, actualArchiveTagName.branchName)
        );
    }

    @Test
    @DisplayName("Non-archive tag name, returns null")
    void tryParseTest2() {
        // arrange

        // act
        ArchiveTagName result = ArchiveTagName.tryParse("non-archive_tag");

        // assert
        assertNull(result);
    }
}
