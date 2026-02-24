package com.project.familytree.tree.repositories;

import com.project.familytree.tree.models.MediaFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MediaFileRepository extends JpaRepository<MediaFile, Long> {

    List<MediaFile> findByPersonId(Long personId);

    List<MediaFile> findByTreeId(Long treeId);

    List<MediaFile> findByTreeIdAndPersonId(Long treeId, Long personId);

    void deleteByPersonId(Long personId);
}
