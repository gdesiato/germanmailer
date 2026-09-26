package com.desiato.germanMailer;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class GermanMailerApp implements CommandLineRunner {

    private final GermanTextGenerator generator;
    private final GermanEmailSender sender;

    public GermanMailerApp(GermanTextGenerator generator,
                           GermanEmailSender sender) {
        this.generator = generator;
        this.sender = sender;
    }

    public static void main(String[] args) {
        ConfigurableApplicationContext context =
                SpringApplication.run(GermanMailerApp.class, args);

        int exitCode = SpringApplication.exit(context, () -> 0);
        System.exit(exitCode);
    }

    @Override
    public void run(String... args) throws Exception {
        System.out.println("Generating today's German lesson...");

        String lesson = generator.generate();

        System.out.println("Sending email...");
        sender.send(lesson);

        System.out.println("Done.");
    }
}