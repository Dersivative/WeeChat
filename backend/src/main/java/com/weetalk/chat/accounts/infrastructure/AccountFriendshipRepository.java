package com.weetalk.chat.accounts.infrastructure;

import com.weetalk.chat.accounts.domain.AccountFriendship;
import com.weetalk.chat.accounts.domain.FriendshipStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AccountFriendshipRepository extends JpaRepository<AccountFriendship, UUID> {
	Optional<AccountFriendship> findByRequesterIdAndAddresseeIdOrRequesterIdAndAddresseeId(
		UUID requesterId,
		UUID addresseeId,
		UUID reversedRequesterId,
		UUID reversedAddresseeId
	);

	@Query("select f from AccountFriendship f where f.status = :status and (f.requester.id = :accountId or f.addressee.id = :accountId)")
	List<AccountFriendship> findByStatusAndAccountId(@Param("status") FriendshipStatus status, @Param("accountId") UUID accountId);

	@Query("select f from AccountFriendship f where (f.requester.id = :accountId or f.addressee.id = :accountId) and f.status in :statuses")
	List<AccountFriendship> findByAccountIdAndStatusIn(
		@Param("accountId") UUID accountId,
		@Param("statuses") Collection<FriendshipStatus> statuses
	);

	List<AccountFriendship> findByStatusAndAddresseeIdIn(FriendshipStatus status, Collection<UUID> addresseeIds);
}
