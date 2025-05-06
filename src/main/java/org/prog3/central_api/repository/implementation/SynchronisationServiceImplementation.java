package org.prog3.central_api.repository.implementation;

import org.prog3.central_api.repository.SynchronisationRepository;
import org.springframework.stereotype.Repository;

@Repository
public class SynchronisationServiceImplementation implements SynchronisationRepository {
    /**
     * @Description Fetch all data from different api
     * @How Use RestTemplate for the fetch; actual api's are in localhost but ports differce;
     * Like:    LA_LIGA: 8081, BUNDESLIGA:8082 .... According to the orders in the enum
     *
     * @return true if done , false or throw error if not
     */
    @Override
    public Boolean Syncronisation() {
        return null;
    }


}
