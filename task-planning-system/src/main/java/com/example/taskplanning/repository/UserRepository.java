// 4. 用户Repository (UserRepository.java)
package com.example.taskplanning.repository;

import com.example.taskplanning.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * 根据用户名检查用户是否存在
     */
    boolean existsByUsername(String username);

    /**
     * 根据邮箱检查用户是否存在
     */
    boolean existsByEmail(String email);

    /**
     * 根据手机号检查用户是否存在
     */
    boolean existsByPhone(String phone);

    /**
     * 根据用户名查找用户（用于登录）
     */
    Optional<User> findByUsername(String username);

    /**
     * 根据邮箱查找用户（用于登录）
     */
    Optional<User> findByEmail(String email);

    Optional<User> findByVerificationToken(String token);

    /**
     * 根据用户名或邮箱查找用户
     */
    @Query("SELECT u FROM User u WHERE u.username = :usernameOrEmail OR u.email = :usernameOrEmail")
    Optional<User> findByUsernameOrEmail(@Param("usernameOrEmail") String usernameOrEmail);
}