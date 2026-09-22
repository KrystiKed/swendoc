package at.technikum.swendoc.user;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PasswordsTest {

    @Test
    void acceptsTheOriginalPassword() {
        assertTrue(Passwords.matches("correct-horse", Passwords.hash("correct-horse")));
    }

    @Test
    void rejectsAWrongPassword() {
        assertFalse(Passwords.matches("wrong-horse", Passwords.hash("correct-horse")));
    }

    @Test
    void saltsEveryHashSeparately() {
        assertNotEquals(Passwords.hash("correct-horse"), Passwords.hash("correct-horse"));
    }
}
