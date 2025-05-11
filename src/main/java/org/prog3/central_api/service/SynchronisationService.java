package org.prog3.central_api.service;

import lombok.AllArgsConstructor;
import org.prog3.central_api.repository.SynchronisationRepository;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class SynchronisationService {
 private SynchronisationRepository repo;
    public void synchronisation(){
         repo.Syncronisation();
    }

}
