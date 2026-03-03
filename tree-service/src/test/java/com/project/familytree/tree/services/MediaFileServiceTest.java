package com.project.familytree.tree.services;

import com.project.familytree.auth.models.User;
import com.project.familytree.auth.services.UserService;
import com.project.familytree.tree.dto.MediaFileDTO;
import com.project.familytree.tree.impls.Gender;
import com.project.familytree.tree.impls.MediaFileType;
import com.project.familytree.tree.models.MediaFile;
import com.project.familytree.tree.models.Person;
import com.project.familytree.tree.models.Tree;
import com.project.familytree.tree.repositories.MediaFileRepository;
import com.project.familytree.tree.repositories.PersonRepository;
import com.project.familytree.tree.repositories.TreeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MediaFileServiceTest {

    @Mock private MediaFileRepository mediaFileRepository;
    @Mock private PersonRepository personRepository;
    @Mock private TreeRepository treeRepository;
    @Mock private UserService userService;
    @Mock private TreeService treeService;
    @Mock private S3Service s3Service;

    @InjectMocks
    private MediaFileService mediaFileService;

    private Tree tree;
    private Person person;
    private User uploader;
    private MediaFile mediaFile;

    @BeforeEach
    void setUp() {
        tree = new Tree();
        tree.setId(1L);
        tree.setName("Тестовое дерево");

        person = new Person(tree, "Иван", "Иванов", null, Gender.MALE);
        person.setId(10L);

        uploader = new User();
        uploader.setId(5L);
        uploader.setFirstName("Загрузчик");
        uploader.setLastName("Тест");
        uploader.setEmail("uploader@test.com");

        mediaFile = new MediaFile(
                person, tree, "photo.jpg",
                "trees/1/media/uuid.jpg",
                MediaFileType.PHOTO, 1024L,
                "Описание", uploader
        );
        mediaFile.setId(100L);
        mediaFile.setUploadedAt(Instant.now());
    }

    // ─── uploadFile ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("uploadFile: сохраняет метаданные и загружает в S3")
    void uploadFile_savesMetadataAndUploadsToS3() throws AccessDeniedException, IOException {
        when(treeService.canEdit(1L, 5L)).thenReturn(true);
        when(treeRepository.findById(1L)).thenReturn(Optional.of(tree));
        when(personRepository.findById(10L)).thenReturn(Optional.of(person));
        when(userService.findById(5L)).thenReturn(uploader);
        when(mediaFileRepository.save(any(MediaFile.class))).thenAnswer(inv -> {
            MediaFile mf = inv.getArgument(0);
            mf.setId(200L);
            mf.setUploadedAt(Instant.now());
            return mf;
        });
        when(s3Service.generatePresignedUrl(anyString())).thenReturn("https://s3.example.com/file");

        MockMultipartFile file = new MockMultipartFile(
                "file", "photo.jpg", "image/jpeg", "fake-image-content".getBytes()
        );

        MediaFileDTO result = mediaFileService.uploadFile(1L, 10L, file, MediaFileType.PHOTO, "Фото", 5L);

        assertThat(result.getId()).isEqualTo(200L);
        assertThat(result.getFileType()).isEqualTo(MediaFileType.PHOTO);
        assertThat(result.getUrl()).isEqualTo("https://s3.example.com/file");

        verify(s3Service).upload(anyString(), any(), anyString(), anyLong());
        verify(mediaFileRepository).save(any(MediaFile.class));
    }

    @Test
    @DisplayName("uploadFile: бросает AccessDeniedException если нет прав на редактирование")
    void uploadFile_throwsIfNoEditAccess() {
        when(treeService.canEdit(1L, 5L)).thenReturn(false);

        MockMultipartFile file = new MockMultipartFile(
                "file", "photo.jpg", "image/jpeg", new byte[0]
        );

        assertThatThrownBy(() -> mediaFileService.uploadFile(1L, 10L, file, MediaFileType.PHOTO, null, 5L))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("прав");

        verifyNoInteractions(s3Service, mediaFileRepository);
    }

    @Test
    @DisplayName("uploadFile: бросает RuntimeException если дерево не найдено")
    void uploadFile_throwsIfTreeNotFound() {
        when(treeService.canEdit(1L, 5L)).thenReturn(true);
        when(treeRepository.findById(1L)).thenReturn(Optional.empty());

        MockMultipartFile file = new MockMultipartFile(
                "file", "photo.jpg", "image/jpeg", new byte[0]
        );

        assertThatThrownBy(() -> mediaFileService.uploadFile(1L, 10L, file, MediaFileType.PHOTO, null, 5L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("не найдено");
    }

    // ─── getPersonMedia ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("getPersonMedia: возвращает список медиафайлов персоны")
    void getPersonMedia_returnsListForPerson() throws AccessDeniedException {
        when(treeService.canView(1L, 5L)).thenReturn(true);
        when(mediaFileRepository.findByTreeIdAndPersonId(1L, 10L)).thenReturn(List.of(mediaFile));
        when(s3Service.generatePresignedUrl(anyString())).thenReturn("https://s3.example.com/file");

        List<MediaFileDTO> result = mediaFileService.getPersonMedia(1L, 10L, 5L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getPersonId()).isEqualTo(10L);
        assertThat(result.get(0).getTreeId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("getPersonMedia: бросает AccessDeniedException если нет прав на просмотр")
    void getPersonMedia_throwsIfNoViewAccess() {
        when(treeService.canView(1L, 5L)).thenReturn(false);

        assertThatThrownBy(() -> mediaFileService.getPersonMedia(1L, 10L, 5L))
                .isInstanceOf(AccessDeniedException.class);
    }

    // ─── downloadFile ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("downloadFile: возвращает ресурс из S3")
    void downloadFile_returnsS3Resource() throws AccessDeniedException {
        when(treeService.canView(1L, 5L)).thenReturn(true);
        when(mediaFileRepository.findById(100L)).thenReturn(Optional.of(mediaFile));

        @SuppressWarnings("unchecked")
        ResponseInputStream<GetObjectResponse> s3Stream = mock(ResponseInputStream.class);
        when(s3Service.download("trees/1/media/uuid.jpg")).thenReturn(s3Stream);

        MediaFileService.DownloadResult result = mediaFileService.downloadFile(1L, 10L, 100L, 5L);

        assertThat(result).isNotNull();
        assertThat(result.fileName()).isEqualTo("photo.jpg");
        assertThat(result.resource()).isNotNull();
    }

    @Test
    @DisplayName("downloadFile: бросает AccessDeniedException если файл не принадлежит дереву")
    void downloadFile_throwsIfFileNotInTree() {
        Tree otherTree = new Tree();
        otherTree.setId(99L);
        MediaFile fileInOtherTree = new MediaFile(
                null, otherTree, "other.jpg", "trees/99/media/other.jpg",
                MediaFileType.PHOTO, 512L, null, uploader
        );
        fileInOtherTree.setId(200L);

        when(treeService.canView(1L, 5L)).thenReturn(true);
        when(mediaFileRepository.findById(200L)).thenReturn(Optional.of(fileInOtherTree));

        assertThatThrownBy(() -> mediaFileService.downloadFile(1L, null, 200L, 5L))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("не принадлежит");
    }

    // ─── deleteFile ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("deleteFile: удаляет из S3 и из репозитория")
    void deleteFile_deletesFromS3AndRepository() throws AccessDeniedException {
        when(treeService.canEdit(1L, 5L)).thenReturn(true);
        when(mediaFileRepository.findById(100L)).thenReturn(Optional.of(mediaFile));
        doNothing().when(s3Service).delete("trees/1/media/uuid.jpg");

        mediaFileService.deleteFile(1L, 10L, 100L, 5L);

        verify(s3Service).delete("trees/1/media/uuid.jpg");
        verify(mediaFileRepository).delete(mediaFile);
    }

    @Test
    @DisplayName("deleteFile: бросает AccessDeniedException если нет прав на редактирование")
    void deleteFile_throwsIfNoEditAccess() {
        when(treeService.canEdit(1L, 5L)).thenReturn(false);

        assertThatThrownBy(() -> mediaFileService.deleteFile(1L, 10L, 100L, 5L))
                .isInstanceOf(AccessDeniedException.class);

        verifyNoInteractions(mediaFileRepository, s3Service);
    }

    @Test
    @DisplayName("deleteFile: продолжает удаление из БД даже если S3 вернул ошибку")
    void deleteFile_continuesDeletionEvenIfS3Fails() throws AccessDeniedException {
        when(treeService.canEdit(1L, 5L)).thenReturn(true);
        when(mediaFileRepository.findById(100L)).thenReturn(Optional.of(mediaFile));
        doThrow(new RuntimeException("S3 unavailable")).when(s3Service).delete(anyString());

        // Не должно бросать исключение
        assertThatCode(() -> mediaFileService.deleteFile(1L, 10L, 100L, 5L))
                .doesNotThrowAnyException();

        // Запись в БД всё равно удаляется
        verify(mediaFileRepository).delete(mediaFile);
    }

    // ─── convertToDTO ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("convertToDTO: корректно маппит все поля")
    void convertToDTO_mapsAllFields() {
        when(s3Service.generatePresignedUrl("trees/1/media/uuid.jpg"))
                .thenReturn("https://s3.example.com/presigned");

        MediaFileDTO dto = mediaFileService.convertToDTO(mediaFile);

        assertThat(dto.getId()).isEqualTo(100L);
        assertThat(dto.getPersonId()).isEqualTo(10L);
        assertThat(dto.getTreeId()).isEqualTo(1L);
        assertThat(dto.getFileName()).isEqualTo("photo.jpg");
        assertThat(dto.getFileType()).isEqualTo(MediaFileType.PHOTO);
        assertThat(dto.getFileSize()).isEqualTo(1024L);
        assertThat(dto.getDescription()).isEqualTo("Описание");
        assertThat(dto.getUploadedById()).isEqualTo(5L);
        assertThat(dto.getUrl()).isEqualTo("https://s3.example.com/presigned");
    }

    @Test
    @DisplayName("convertToDTO: url = null если S3 presigned URL недоступен")
    void convertToDTO_urlIsNullIfPresignedUrlFails() {
        when(s3Service.generatePresignedUrl(anyString()))
                .thenThrow(new RuntimeException("S3 error"));

        MediaFileDTO dto = mediaFileService.convertToDTO(mediaFile);

        assertThat(dto.getUrl()).isNull();
    }
}
