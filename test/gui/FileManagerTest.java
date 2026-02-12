package gui;

import static org.junit.Assert.*;
import org.junit.Test;
import org.junit.Before;
import org.junit.After;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Tests for the FileManager class.
 *
 * Tests file I/O operations that don't require dialog interaction.
 * Constructed with null parent since we skip dialog methods.
 */
public class FileManagerTest {

    private FileManager fileManager;
    private File tempDir;
    private Path autoSavePath;

    @Before
    public void setUp() throws IOException {
        fileManager = new FileManager(null);
        tempDir = Files.createTempDirectory("filemanager-test").toFile();
        autoSavePath = Paths.get(System.getProperty("user.home"),
            ".risc16-simulator", "autosave.as16");
    }

    @After
    public void tearDown() {
        // Clean up temp directory
        if (tempDir != null && tempDir.exists()) {
            File[] files = tempDir.listFiles();
            if (files != null) {
                for (File f : files) {
                    f.delete();
                }
            }
            tempDir.delete();
        }

        // Clean up autosave file
        try {
            Files.deleteIfExists(autoSavePath);
        } catch (IOException e) {
            // ignore
        }
    }

    // ========================================================================
    // Helper methods
    // ========================================================================

    private File createTempFile(String name, String content) throws IOException {
        File file = new File(tempDir, name);
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(content);
        }
        return file;
    }

    // ========================================================================
    // loadFile tests
    // ========================================================================

    @Test
    public void testLoadFileReturnsContents() throws IOException {
        File file = createTempFile("test.as16", "addi r1, r0, 5\njalr r0, r0");
        String content = fileManager.loadFile(file);
        assertEquals("addi r1, r0, 5\njalr r0, r0", content);
    }

    @Test
    public void testLoadFileSetsCurrentFile() throws IOException {
        File file = createTempFile("test.as16", "some content");
        fileManager.loadFile(file);
        assertEquals(file, fileManager.getCurrentFile());
    }

    @Test
    public void testLoadFileNonExistentReturnsNull() {
        File missing = new File(tempDir, "nonexistent.as16");
        String content = fileManager.loadFile(missing);
        assertNull(content);
    }

    // ========================================================================
    // autoSave tests
    // ========================================================================

    @Test
    public void testAutoSaveWithCurrentFileWritesToCurrentFile() throws IOException {
        File file = createTempFile("test.as16", "original");
        fileManager.loadFile(file);

        fileManager.autoSave("updated content");

        String content = new String(Files.readAllBytes(file.toPath()));
        assertEquals("updated content", content);
    }

    @Test
    public void testAutoSaveWithoutCurrentFileSavesToAutosaveLocation() {
        fileManager.autoSave("autosave content");

        assertTrue(autoSavePath.toFile().exists());
    }

    @Test
    public void testAutoSaveEmptyContentIsNoOp() {
        fileManager.autoSave("");

        assertFalse(autoSavePath.toFile().exists());
    }

    @Test
    public void testAutoSaveNullContentIsNoOp() {
        fileManager.autoSave(null);

        assertFalse(autoSavePath.toFile().exists());
    }

    // ========================================================================
    // recoverAutoSave tests
    // ========================================================================

    @Test
    public void testRecoverAutoSaveReturnsContent() {
        fileManager.autoSave("recovered content");

        String recovered = fileManager.recoverAutoSave();
        assertEquals("recovered content", recovered);
    }

    @Test
    public void testRecoverAutoSaveReturnsNullWhenNoFile() {
        // Ensure no autosave file exists
        try {
            Files.deleteIfExists(autoSavePath);
        } catch (IOException e) {
            // ignore
        }

        assertNull(fileManager.recoverAutoSave());
    }

    // ========================================================================
    // clearAutoSave tests
    // ========================================================================

    @Test
    public void testClearAutoSaveDeletesFile() {
        fileManager.autoSave("some content");
        assertTrue(autoSavePath.toFile().exists());

        fileManager.clearAutoSave();

        assertNull(fileManager.recoverAutoSave());
    }

    // ========================================================================
    // newFile tests
    // ========================================================================

    @Test
    public void testNewFileClearsCurrentFile() throws IOException {
        File file = createTempFile("test.as16", "content");
        fileManager.loadFile(file);
        assertNotNull(fileManager.getCurrentFile());

        fileManager.newFile();

        assertNull(fileManager.getCurrentFile());
    }

    // ========================================================================
    // getCurrentFileName tests
    // ========================================================================

    @Test
    public void testGetCurrentFileNameUntitledInitially() {
        assertEquals("Untitled", fileManager.getCurrentFileName());
    }

    @Test
    public void testGetCurrentFileNameAfterLoad() throws IOException {
        File file = createTempFile("myprogram.as16", "content");
        fileManager.loadFile(file);

        assertEquals("myprogram.as16", fileManager.getCurrentFileName());
    }

    // ========================================================================
    // save tests
    // ========================================================================

    @Test
    public void testSaveWithCurrentFileWritesContent() throws IOException {
        File file = createTempFile("test.as16", "original");
        fileManager.loadFile(file);

        boolean result = fileManager.save("new content");

        assertTrue(result);
        String content = new String(Files.readAllBytes(file.toPath()));
        assertEquals("new content", content);
    }
}
