package com.portfolio.releasegovernance.domain;

import static com.portfolio.releasegovernance.domain.DomainEnums.ReleaseStatus.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class ReleaseStateMachineTest {

    @Test
    void permitsGovernedPromotionPath() {
        assertThat(ReleaseStateMachine.isAllowed(DISCOVERED, AWAITING_APPROVAL)).isTrue();
        assertThat(ReleaseStateMachine.isAllowed(AWAITING_APPROVAL, APPROVED)).isTrue();
        assertThat(ReleaseStateMachine.isAllowed(APPROVED, SCHEDULED)).isTrue();
        assertThat(ReleaseStateMachine.isAllowed(SCHEDULED, DEPLOYED)).isTrue();
        assertThat(ReleaseStateMachine.isAllowed(DEPLOYED, SUPERSEDED)).isTrue();
    }

    @Test
    void rejectsSkippingApprovalAndTerminalReuse() {
        assertThatThrownBy(() -> ReleaseStateMachine.ensureAllowed(DISCOVERED, DEPLOYED))
                .isInstanceOf(GovernanceRuleException.class)
                .hasMessageContaining("DISCOVERED");
        assertThat(ReleaseStateMachine.isAllowed(SUPERSEDED, DISCOVERED)).isFalse();
        assertThat(ReleaseStateMachine.isAllowed(APPROVED, APPROVED)).isFalse();
    }

    @Test
    void allowsBlockedReleaseToBeReassessed() {
        assertThat(ReleaseStateMachine.isAllowed(BLOCKED, DISCOVERED)).isTrue();
    }
}
