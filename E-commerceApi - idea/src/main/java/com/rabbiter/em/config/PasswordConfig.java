package com.rabbiter.em.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class PasswordConfig {
    @Bean
    public PasswordEncoder passwordEncoder() {
        // Argon2id：64MiB 内存、3 次迭代；根据你的机器可稍微调大/调小
//        return new Argon2PasswordEncoder(
//                16,      // saltLength bytes
//                32,      // hashLength bytes
//                1,       // parallelism
//                1 << 16, // memory KiB = 65536 KiB = 64 MiB
//                3        // iterations
//        );
         return new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder(12);
    }

    /* 备选：如果你想先用 BCrypt，更换为下面一行即可
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder(12);
    }
    */
}
