package io.basquiat.user.repo;

import io.basquiat.user.model.User;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

public interface UserRepository extends ReactiveCrudRepository<User, Long>, CustomUserRepository {
}
