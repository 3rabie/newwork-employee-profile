package com.newwork.employee.config.jobs;

import com.newwork.employee.service.AbsenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Scheduled job to automatically complete expired approved absence requests.
 * Runs daily at 2:00 AM to mark absences as completed when their end date has passed.
 */
@Slf4j
@Component
@EnableScheduling
@RequiredArgsConstructor
public class AbsenceCompletionJob {

    private final AbsenceService absenceService;

    @Scheduled(cron = "0 0 2 * * *")
    public void autoCompleteExpiredAbsences() {
        log.info("Starting scheduled task: Auto-complete expired absence requests");
        try {
            int completed = absenceService.completeExpiredApproved(LocalDate.now());
            log.info("Successfully auto-completed {} expired absence request(s)", completed);
        } catch (Exception e) {
            log.error("Error auto-completing expired absence requests", e);
        }
    }
}
