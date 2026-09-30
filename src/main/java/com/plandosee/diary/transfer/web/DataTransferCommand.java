package com.plandosee.diary.transfer.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.server.context.WebServerApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

import com.plandosee.diary.transfer.application.DataTransferService;
import com.plandosee.diary.transfer.application.TransferReport;

/**
 * Entry of the one-time data transfer (ADR-35), active only with {@code --app.transfer.to-login-id=<login id>} and
 * without a web server ({@code --spring.main.web-application-type=none}); in a running web app it refuses. It logs
 * the outcome and the per-table counts (never data, secrets or the database address) and then ends the process:
 * exit code 0 for TRANSFERRED or NOTHING_TO_TRANSFER, 1 otherwise ({@code app.transfer.exit=false} keeps it running,
 * for tests).
 */
@Component
@ConditionalOnProperty(name = "app.transfer.to-login-id")
public class DataTransferCommand implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataTransferCommand.class);

    private final DataTransferService service;
    private final ConfigurableApplicationContext context;
    private final String targetLoginId;
    private final boolean exit;

    public DataTransferCommand(DataTransferService service, ConfigurableApplicationContext context,
                               @Value("${app.transfer.to-login-id}") String targetLoginId,
                               @Value("${app.transfer.exit:true}") boolean exit) {
        this.service = service;
        this.context = context;
        this.targetLoginId = targetLoginId;
        this.exit = exit;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (context instanceof WebServerApplicationContext) {
            log.error("event=transfer_refused reason=web_server_running hint=spring.main.web-application-type=none");
            finish(2);
            return;
        }
        TransferReport report = service.transferTo(targetLoginId);
        log(report);
        finish(report.outcome().success() ? 0 : 1);
    }

    static void log(TransferReport report) {
        if (report.demoBefore() != null) {
            log.info("event=transfer_counts phase=before owner=demo {}", report.demoBefore().describe());
            log.info("event=transfer_counts phase=before owner=target {}", report.targetBefore().describe());
        }
        if (report.demoAfter() != null) {
            log.info("event=transfer_counts phase=after owner=demo {}", report.demoAfter().describe());
            log.info("event=transfer_counts phase=after owner=target {}", report.targetAfter().describe());
        }
        if (report.outcome().success()) {
            log.info("event=transfer_finished outcome={}", report.outcome());
        } else {
            log.error("event=transfer_finished outcome={}", report.outcome());
        }
    }

    private void finish(int code) {
        if (exit) {
            System.exit(SpringApplication.exit(context, () -> code));
        }
    }
}
