package com.example.services;

import com.example.models.dto.UserUpdateDto;
import com.example.models.entities.User;
import com.example.repositories.MyService;
import com.example.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import javax.persistence.OptimisticLockException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class UserService implements MyService<User, Long>, UserDetailsService {

    private final UserRepository userRepository;

    @Autowired
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public <S extends User> S save(S user) {
        return userRepository.save(user);
    }

    @Override
    public Optional<User> findById(Long userId) {
        return userRepository.findById(userId);
    }

    @Override
    public List<User> findAll() {
        return (List<User>) userRepository.findAll();
    }

    @Override
    public void deleteById(Long userId) {
        userRepository.deleteById(userId);
    }

    public ResponseEntity<?> updateUser(UserUpdateDto userUpdateDto) {
        try {
            Optional<User> optionalUser = findById(userUpdateDto.getId());

            if (!optionalUser.isPresent()) {
                return ResponseEntity.notFound().build();
            }

            User user = optionalUser.get();
            user.setFirstName(userUpdateDto.getFirstName());
            user.setLastName(userUpdateDto.getLastName());
            user.setEmail(userUpdateDto.getEmail());
            user.setPermissions(userUpdateDto.getPermissions());

            return ResponseEntity.ok(save(user));
        } catch (OptimisticLockException ole ) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Update failed due to concurrent modification.");
        }
    }

    public boolean emailExists(String email) {
        return userRepository.findByEmail(email).isPresent();
    }

    public User findByEmail(String email) {
        Optional<User> optionalUser = userRepository.findByEmail(email);
        return optionalUser.orElse(null);
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Optional<User> user = this.userRepository.findByEmail(email);
        if (!user.isPresent()) throw new UsernameNotFoundException("Email " + email + " not found");
        List<GrantedAuthority> permissions = user.get().getPermissions().stream()
                .map(permission -> new SimpleGrantedAuthority(permission.name()))
                .collect(Collectors.toList());

        return new org.springframework.security.core.userdetails.User(user.get().getEmail(), user.get().getPasswordHash(), permissions);
    }
}
