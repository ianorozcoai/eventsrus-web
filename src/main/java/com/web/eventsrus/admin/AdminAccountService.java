package com.web.eventsrus.admin;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * The whole admin module's account store - in-memory only (see AdminAccount),
 * seeded at startup with the one master login. There's no database-backed
 * admin table anywhere yet, so this is intentionally the simplest thing that
 * could work: a synchronized list guarded by BCrypt-hashed passwords, not a
 * fake-persistence stub that resets every request like the rest of this app's
 * stub CRUD - an admin created here needs to actually be able to log back in
 * for the remainder of this run.
 */
@Service
public class AdminAccountService {

    private static final String MASTER_USERNAME = "ianadmin";
    private static final String MASTER_PASSWORD = "Kerberos103!";

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    private final List<AdminAccount> accounts = new CopyOnWriteArrayList<>();
    private final AtomicLong nextId = new AtomicLong(1);

    public AdminAccountService() {
        accounts.add(new AdminAccount(nextId.getAndIncrement(), MASTER_USERNAME, encoder.encode(MASTER_PASSWORD), Instant.now()));
    }

    public boolean authenticate(String username, String rawPassword) {
        return findByUsername(username)
                .map(account -> encoder.matches(rawPassword, account.passwordHash()))
                .orElse(false);
    }

    public boolean usernameExists(String username) {
        return findByUsername(username).isPresent();
    }

    public AdminAccount create(String username, String rawPassword) {
        AdminAccount account = new AdminAccount(nextId.getAndIncrement(), username, encoder.encode(rawPassword), Instant.now());
        accounts.add(account);
        return account;
    }

    public List<AdminAccount> listAll() {
        return List.copyOf(accounts);
    }

    private Optional<AdminAccount> findByUsername(String username) {
        return accounts.stream().filter(a -> a.username().equalsIgnoreCase(username)).findFirst();
    }
}
