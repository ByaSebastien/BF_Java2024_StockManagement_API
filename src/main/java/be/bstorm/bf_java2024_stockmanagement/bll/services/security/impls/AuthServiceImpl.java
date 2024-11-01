package be.bstorm.bf_java2024_stockmanagement.bll.services.security.impls;

import be.bstorm.bf_java2024_stockmanagement.bll.services.security.AuthService;
import be.bstorm.bf_java2024_stockmanagement.dal.repositories.UserRepository;
import be.bstorm.bf_java2024_stockmanagement.dl.entities.person.User;
import be.bstorm.bf_java2024_stockmanagement.il.utils.MailerThread;
import be.bstorm.bf_java2024_stockmanagement.il.utils.MailerUtils;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;

import java.util.List;
import java.util.Random;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final MailerUtils mailerUtils;

    @Override
    @Transactional
    public void register(User user) {

        if(userRepository.existsByEmail(user.getEmail())){
            throw new UsernameNotFoundException("User with email " + user.getEmail() + " already exists");
        }
        String password = generatePassword(49);
        user.setPassword(passwordEncoder.encode(password));
        user.setId(UUID.randomUUID());
        userRepository.save(user);

        Context context = new Context();
        context.setVariable("email", user.getEmail());
        context.setVariable("password", password);
        MailerThread mailerThread = mailerUtils.createThread(
                "Welcome",
                "sendPassword",
                context,
                user.getEmail()
        );
        Thread thread = new Thread(mailerThread);
        thread.start();
    }

    @Override
    public User login(User user) {
        User existingUser = userRepository.findByEmail(user.getEmail()).orElseThrow();
        if (!passwordEncoder.matches(user.getPassword(), existingUser.getPassword())) {
            throw new RuntimeException("Wrong password");
        }
        return existingUser;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return userRepository.findByEmail(email).orElseThrow(() -> new UsernameNotFoundException(email));
    }

    private String generatePassword(int length) {
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*()-_=+[]{}|\\:;\"'<>,.?/";

        Random random = new Random();
        StringBuilder password = new StringBuilder();
        for (int i = 0; i < length; i++) {
            password.append(characters.charAt(random.nextInt(characters.length())));
        }
        return password.toString();
    }
}
