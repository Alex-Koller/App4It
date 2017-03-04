package com.dreambig.app4it.api;

import com.dreambig.app4it.entity.App4ItUser;
import com.dreambig.app4it.entity.App4ItUserCandidate;

import java.util.List;

/**
 * Created by Alexandr on 02/01/2015.
 */
public interface FirebaseApp4ItUsers {

    public void processAp4ItUsers(List<App4ItUser> users, List<App4ItUserCandidate> userCandidates);

}
