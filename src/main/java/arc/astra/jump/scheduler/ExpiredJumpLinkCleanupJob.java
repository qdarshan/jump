package arc.astra.jump.scheduler;

import arc.astra.jump.dao.JumpLinkRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ExpiredJumpLinkCleanupJob {

    private final JumpLinkRepository jumpLinkRepository;

    public ExpiredJumpLinkCleanupJob(JumpLinkRepository jumpLinkRepository) {
        this.jumpLinkRepository = jumpLinkRepository;
    }

    @Scheduled(cron = "0 */30 * * * *")
    public void cleanup() {
        jumpLinkRepository.deleteExpired();
    }
}
