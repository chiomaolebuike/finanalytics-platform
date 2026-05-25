package com.finanalytics.finanalytics_platform.repository;

import com.finanalytics.finanalytics_platform.entity.UserBehaviourProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserBehaviourProfileRepository extends JpaRepository<UserBehaviourProfile, Long> {
}
