package org.prog3.central_api.model.generalModels;


import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class Match {
    private String id;
    private MatchClub clubPlayingHome;
    private MatchClub clubPlayingAway;
    private String stadium;
    private LocalDateTime matchDateTime;
    private MatchStatus actualStatus;

    private boolean transitionIsOkay(MatchStatus pretendingStatus) {
        return pretendingStatus.ordinal() - this.actualStatus.ordinal() == 1 || pretendingStatus.ordinal() - this.actualStatus.ordinal() == -1;
    }

    public boolean transitionStatus(MatchStatus pretendingStatus) {
        if (transitionIsOkay(pretendingStatus)) {
            setActualStatus(pretendingStatus);
            return true;
        }
        return false;
    }
}
