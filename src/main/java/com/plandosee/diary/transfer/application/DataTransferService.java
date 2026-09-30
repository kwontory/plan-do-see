package com.plandosee.diary.transfer.application;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.plandosee.diary.auth.domain.AuthRules;
import com.plandosee.diary.common.db.StatementBudget;
import com.plandosee.diary.transfer.application.port.DataTransferMapper;
import com.plandosee.diary.transfer.domain.OwnedCounts;

/**
 * Moves the T06 data of the demo owner (app.demo-user-id) to one account, once (ADR-35, T07-C100). In one
 * transaction: lock both person rows, check that the account exists, is not the demo owner and owns nothing yet (so
 * no tag name can collide), then change the owner of plans, tags and reviews; every other table follows through its
 * plan or todo. The counts after must be the demo counts before, on the account, and zero on the demo owner; otherwise
 * everything rolls back. Running it again finds nothing to move (idempotent). Timestamps are kept.
 */
@Service
public class DataTransferService {

    private static final int TRANSACTION_SECONDS = 60;

    private final DataTransferMapper mapper;
    private final StatementBudget statementBudget;
    private final TransactionTemplate transaction;
    private final UUID demoUserId;

    public DataTransferService(DataTransferMapper mapper, StatementBudget statementBudget,
                               PlatformTransactionManager transactionManager,
                               @Value("${app.demo-user-id}") UUID demoUserId) {
        this.mapper = mapper;
        this.statementBudget = statementBudget;
        this.transaction = new TransactionTemplate(transactionManager);
        this.transaction.setTimeout(TRANSACTION_SECONDS);
        this.demoUserId = demoUserId;
    }

    public TransferReport transferTo(String typedLoginId) {
        String loginId = AuthRules.loginId(typedLoginId);
        if (loginId == null) {
            return TransferReport.refused(TransferOutcome.TARGET_NOT_FOUND);
        }
        return transaction.execute(status -> {
            statementBudget.useMaintenanceTimeouts();
            UUID target = mapper.findActiveLocalUserId(loginId);
            if (target == null) {
                return TransferReport.refused(TransferOutcome.TARGET_NOT_FOUND);
            }
            if (target.equals(demoUserId)) {
                return TransferReport.refused(TransferOutcome.TARGET_IS_DEMO_USER);
            }
            mapper.lockUsers(demoUserId, target);
            OwnedCounts demoBefore = mapper.countOwned(demoUserId);
            OwnedCounts targetBefore = mapper.countOwned(target);
            if (demoBefore.total() == 0) {
                return new TransferReport(TransferOutcome.NOTHING_TO_TRANSFER, demoBefore, targetBefore, demoBefore,
                        targetBefore);
            }
            if (targetBefore.total() > 0) {
                return new TransferReport(TransferOutcome.TARGET_HAS_DATA, demoBefore, targetBefore, null, null);
            }
            mapper.reassignPlans(demoUserId, target);
            mapper.reassignTags(demoUserId, target);
            mapper.reassignReviews(demoUserId, target);
            OwnedCounts demoAfter = mapper.countOwned(demoUserId);
            OwnedCounts targetAfter = mapper.countOwned(target);
            if (demoAfter.total() != 0 || !targetAfter.equals(demoBefore)) {
                throw new IllegalStateException("transfer counts do not match; rolled back");
            }
            return new TransferReport(TransferOutcome.TRANSFERRED, demoBefore, targetBefore, demoAfter, targetAfter);
        });
    }
}
