package org.prog3.central_api.model.generalModels;


import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

@EqualsAndHashCode(callSuper=true)
@Data
public class Season extends CreateSeason implements Serializable {
    private SeasonStatus status;
    private String id;

    //Méthode de vérification de la transition du status
    private boolean transitionIsOkay(SeasonStatus pretendingStatus) {
        return pretendingStatus.ordinal() - this.status.ordinal() == 1 || pretendingStatus.ordinal() - this.status.ordinal() == -1;
    }

    public boolean transitionStatus(SeasonStatus pretendingStatus) {
        if (transitionIsOkay(pretendingStatus)) {
            setStatus(pretendingStatus);
            return true;
        }
        return false;
    }

}
