package com.segula.saasgestion.repository;

import com.segula.saasgestion.domain.IssueDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IssueDocumentRepository extends JpaRepository<IssueDocument, Long> {
    List<IssueDocument> findByIssueIdOrderByUploadedAtDesc(Long issueId);
    List<IssueDocument> findByIssueIdIn(List<Long> issueIds);
    void deleteByIssueId(Long issueId);
}
