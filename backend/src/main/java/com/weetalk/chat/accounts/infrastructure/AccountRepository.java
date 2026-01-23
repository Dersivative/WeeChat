package com.weetalk.chat.accounts.infrastructure;

import com.weetalk.chat.accounts.domain.Account;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<Account, UUID> {
}
