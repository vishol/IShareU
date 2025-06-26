package com.filesharing.service;

import com.filesharing.model.User;
import com.filesharing.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Collections;

@Service
@Transactional
public class UserService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * Save a user to the database
     */
    public User saveUser(User user) {
        // Encode password before saving
        if (user.getPassword() != null && !user.getPassword().startsWith("$2a$")) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }
        return userRepository.save(user);
    }

    /**
     * Find user by ID
     */
    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    /**
     * Find user by email
     */
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    /**
     * Get all users
     */
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    /**
     * Get users created after a specific date
     */
    public List<User> getUsersCreatedAfter(LocalDateTime date) {
        return userRepository.findByCreatedAtAfter(date);
    }

    /**
     * Get users created in the last 24 hours
     */
    public List<User> getRecentUsers() {
        LocalDateTime yesterday = LocalDateTime.now().minusDays(1);
        return userRepository.findByCreatedAtAfter(yesterday);
    }

    /**
     * Get users with files
     */
    public List<User> getUsersWithFiles() {
        return userRepository.findUsersWithFiles();
    }

    /**
     * Get users with active files
     */
    public List<User> getUsersWithActiveFiles() {
        return userRepository.findUsersWithActiveFiles();
    }

    /**
     * Get top users by file count
     */
    public List<User> getTopUsersByFileCount() {
        return userRepository.findTopUsersByFileCount();
    }

    /**
     * Get top users by total file size
     */
    public List<User> getTopUsersByFileSize() {
        return userRepository.findTopUsersByFileSize();
    }

    /**
     * Check if user exists by email
     */
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    /**
     * Create a new user
     */
    public User createUser(String email, String password) {
        if (existsByEmail(email)) {
            throw new RuntimeException("User with email " + email + " already exists");
        }

        User user = new User(email, password);
        return saveUser(user);
    }

    /**
     * Update user information
     */
    public User updateUser(Long id, String email, String password) {
        Optional<User> existingUser = findById(id);
        if (existingUser.isEmpty()) {
            throw new RuntimeException("User with ID " + id + " not found");
        }

        User user = existingUser.get();
        if (email != null && !email.equals(user.getEmail())) {
            if (existsByEmail(email)) {
                throw new RuntimeException("Email " + email + " is already taken");
            }
            user.setEmail(email);
        }

        if (password != null && !password.isEmpty()) {
            user.setPassword(password);
        }

        return saveUser(user);
    }

    /**
     * Delete a user
     */
    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }

    /**
     * Delete users with no files
     */
    public void deleteUsersWithNoFiles() {
        userRepository.deleteUsersWithNoFiles();
    }

    /**
     * Get user statistics
     */
    public UserStatistics getUserStatistics() {
        long totalUsers = userRepository.count();
        long recentUsers = userRepository.countRecentUsers(LocalDateTime.now().minusDays(1));
        long usersWithFiles = userRepository.findUsersWithFiles().size();
        long usersWithActiveFiles = userRepository.findUsersWithActiveFiles().size();

        return new UserStatistics(totalUsers, recentUsers, usersWithFiles, usersWithActiveFiles);
    }

    /**
     * Search users by email pattern
     */
    public List<User> searchUsersByEmail(String emailPattern) {
        return userRepository.findByEmailContainingIgnoreCase(emailPattern);
    }

    /**
     * Get users with file count greater than specified value
     */
    public List<User> getUsersWithFileCountGreaterThan(int fileCount) {
        return userRepository.findUsersWithFileCountGreaterThan(fileCount);
    }

    /**
     * Get users with total file size greater than specified value
     */
    public List<User> getUsersWithTotalFileSizeGreaterThan(Long totalSize) {
        return userRepository.findUsersWithTotalFileSizeGreaterThan(totalSize);
    }

    /**
     * Validate user credentials
     */
    public boolean validateCredentials(String email, String password) {
        Optional<User> user = findByEmail(email);
        if (user.isPresent()) {
            return passwordEncoder.matches(password, user.get().getPassword());
        }
        return false;
    }

    /**
     * Change user password
     */
    public boolean changePassword(Long userId, String oldPassword, String newPassword) {
        Optional<User> user = findById(userId);
        if (user.isPresent()) {
            User userEntity = user.get();
            if (passwordEncoder.matches(oldPassword, userEntity.getPassword())) {
                userEntity.setPassword(newPassword);
                saveUser(userEntity);
                return true;
            }
        }
        return false;
    }

    /**
     * Inner class for user statistics
     */
    public static class UserStatistics {
        private final long totalUsers;
        private final long recentUsers;
        private final long usersWithFiles;
        private final long usersWithActiveFiles;

        public UserStatistics(long totalUsers, long recentUsers, long usersWithFiles, long usersWithActiveFiles) {
            this.totalUsers = totalUsers;
            this.recentUsers = recentUsers;
            this.usersWithFiles = usersWithFiles;
            this.usersWithActiveFiles = usersWithActiveFiles;
        }

        public long getTotalUsers() {
            return totalUsers;
        }

        public long getRecentUsers() {
            return recentUsers;
        }

        public long getUsersWithFiles() {
            return usersWithFiles;
        }

        public long getUsersWithActiveFiles() {
            return usersWithActiveFiles;
        }

        public long getUsersWithoutFiles() {
            return totalUsers - usersWithFiles;
        }

        public double getActiveUserPercentage() {
            return totalUsers > 0 ? (double) usersWithActiveFiles / totalUsers * 100 : 0;
        }
    }

    @Override
    public UserDetails loadUserByUsername(String identifier) throws UsernameNotFoundException {
        Optional<User> userOpt = userRepository.findByEmail(identifier);
        if (userOpt.isEmpty()) {
            // Try username if not found by email
            userOpt = userRepository.findByUsername(identifier);
            if (userOpt.isEmpty()) {
                throw new UsernameNotFoundException("User not found with email or username: " + identifier);
            }
        }
        User user = userOpt.get();
        String role = user.isAdmin() ? "ADMIN" : "USER";
        return new org.springframework.security.core.userdetails.User(
            user.getEmail(),
            user.getPassword(),
            java.util.Collections.singletonList(new SimpleGrantedAuthority(role))
        );
    }

    public UserRepository getUserRepository() {
        return userRepository;
    }

    public Page<User> getAllUsersPaginated(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    public Page<User> searchUsersByEmail(String emailPattern, Pageable pageable) {
        return userRepository.findByEmailContainingIgnoreCase(emailPattern, pageable);
    }
} 