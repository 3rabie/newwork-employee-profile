package com.newwork.employee.scheduling;

import com.newwork.employee.service.AbsenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
@Slf4j
public class AbsenceCompletionJob {

    private final AbsenceService absenceService;

    /**
     * Run daily to mark approved absences as completed after their end date.
     */
    @Scheduled(cron = "0 0 1 * * *")
    public void completeExpiredApproved() {
        int updated = absenceService.completeExpiredApproved(LocalDate.now());
        if (updated > 0) {
            log.info("Marked {} absence request(s) as COMPLETED", updated);
        }
    }
}
